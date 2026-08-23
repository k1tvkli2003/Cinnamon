[CmdletBinding()]
param(
    [ValidateSet("Quest", "LearningFocus")]
    [string]$Scenario = "Quest",
    [string]$DeviceSerial,
    [switch]$AllowPhysicalDevice,
    [string]$OutputDirectory = "qa/runtime-proof"
)

<#!
.SYNOPSIS
Proves a stateful Cinnamon gamification flow across a real Android process restart.

.DESCRIPTION
Builds and installs the debug app plus its instrumentation APK, runs the selected
scenario's write method, force-stops the target package, verifies that its PID is
gone, then runs the persistence method in a fresh instrumentation invocation.

The first method intentionally resets Cinnamon's local learning/gamification state
inside the selected debug app. For safety, this script accepts emulators by default;
pass -AllowPhysicalDevice only for an explicitly disposable physical test device.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "com.cinnamon.app"
$testPackageName = "com.cinnamon.app.test"
$runner = "androidx.test.runner.AndroidJUnitRunner"
$scenarioContract = switch ($Scenario) {
    "Quest" {
        [pscustomobject]@{
            Name = "Quest"
            EvidencePrefix = "quest"
            TestClass = "com.cinnamon.app.ui.QuestLifecycleAndroidTest"
            StepOne = "step1_reviewCompletesQuest_claimsOnce_andSurvivesActivityRecreation"
            StepTwo = "step2_claimPersistsAcrossFreshInstrumentationProcess"
            LogTag = "QuestProcessProof"
        }
    }
    "LearningFocus" {
        [pscustomobject]@{
            Name = "Learning Focus"
            EvidencePrefix = "learning-focus"
            TestClass = "com.cinnamon.app.ui.LearningFocusLifecycleAndroidTest"
            StepOne = "step1_reviewEvidenceUnlocksFocus_selectionIsImmutable_andSurvivesActivityRecreation"
            StepTwo = "step2_focusPersistsAcrossFreshInstrumentationProcess"
            LogTag = "FocusProcessProof"
        }
    }
}

function Find-Adb {
    $candidates = @(
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    if ($candidates.Count -gt 0) {
        return (Resolve-Path -LiteralPath $candidates[0]).Path
    }

    $fromPath = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        return $fromPath.Source
    }

    throw "adb.exe was not found. Install Android platform-tools or set ANDROID_SDK_ROOT."
}

function Get-AdbOutput {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [switch]$AllowEmpty,
        [switch]$AllowNonZeroExit
    )

    $output = & $script:adbPath @Arguments 2>&1
    if ($LASTEXITCODE -ne 0 -and -not $AllowNonZeroExit) {
        throw "adb $($Arguments -join ' ') failed: $($output -join [Environment]::NewLine)"
    }
    $text = ($output -join [Environment]::NewLine).Trim()
    if (-not $AllowEmpty -and [string]::IsNullOrWhiteSpace($text)) {
        throw "adb $($Arguments -join ' ') returned no output."
    }
    return $text
}

function Invoke-DeviceAdb {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [switch]$AllowEmpty,
        [switch]$AllowNonZeroExit
    )

    return Get-AdbOutput `
        -Arguments (@("-s", $script:deviceSerial) + $Arguments) `
        -AllowEmpty:$AllowEmpty `
        -AllowNonZeroExit:$AllowNonZeroExit
}

function Invoke-InstrumentationMethod {
    param(
        [Parameter(Mandatory)]
        [string]$MethodName,
        [switch]$RequireFreshProcess
    )

    $selector = "$($scenarioContract.TestClass)#$MethodName"
    $arguments = @(
        "shell", "am", "instrument", "-w", "-r",
        "-e", "class", $selector
    )
    if ($RequireFreshProcess) {
        $arguments += @("-e", "requireFreshProcess", "true")
    }
    $arguments += "$testPackageName/$runner"
    $output = Invoke-DeviceAdb -Arguments $arguments
    if (
        $output -notmatch "(?m)^\s*OK \(1 test\)\s*$" -or
        $output -notmatch "(?m)^\s*INSTRUMENTATION_CODE:\s*-1\s*$"
    ) {
        throw "Instrumentation method $selector did not report OK (1 test):`n$output"
    }
    return $output
}

$adbPath = Find-Adb
$deviceListing = Get-AdbOutput -Arguments @("devices", "-l")
$onlineRows = @($deviceListing -split "`r?`n" | Where-Object { $_ -match "^(\S+)\s+device(?:\s|$)" })

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    if ($onlineRows.Count -ne 1) {
        throw "Expected exactly one online Android device, found $($onlineRows.Count). Pass -DeviceSerial explicitly."
    }
    $DeviceSerial = ([regex]::Match($onlineRows[0], "^(\S+)")).Groups[1].Value
}
$deviceSerial = $DeviceSerial

$isEmulator = (Invoke-DeviceAdb -Arguments @("shell", "getprop", "ro.kernel.qemu") -AllowEmpty) -eq "1"
if (-not $isEmulator -and -not $AllowPhysicalDevice) {
    throw "Refusing to reset learning state on physical device $deviceSerial. Use an emulator or pass -AllowPhysicalDevice explicitly."
}

$gradleWrapper = (Resolve-Path -LiteralPath ".\gradlew.bat").Path
& $gradleWrapper --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
if ($LASTEXITCODE -ne 0) {
    throw "Gradle failed to build the debug or instrumentation APK."
}

$appApk = (Resolve-Path -LiteralPath "app/build/outputs/apk/debug/app-debug.apk").Path
$testApk = (Resolve-Path -LiteralPath "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk").Path
Invoke-DeviceAdb -Arguments @("install", "-r", "-t", $appApk) | Out-Null
Invoke-DeviceAdb -Arguments @("install", "-r", "-t", $testApk) | Out-Null
Invoke-DeviceAdb -Arguments @("logcat", "-c") -AllowEmpty | Out-Null

$stepOneOutput = Invoke-InstrumentationMethod -MethodName $scenarioContract.StepOne
$pidBeforeForceStop = Invoke-DeviceAdb `
    -Arguments @("shell", "pidof", $packageName) `
    -AllowEmpty `
    -AllowNonZeroExit
Invoke-DeviceAdb -Arguments @("shell", "am", "force-stop", $packageName) -AllowEmpty | Out-Null
Start-Sleep -Milliseconds 500
$pidAfterForceStop = Invoke-DeviceAdb `
    -Arguments @("shell", "pidof", $packageName) `
    -AllowEmpty `
    -AllowNonZeroExit
if (-not [string]::IsNullOrWhiteSpace($pidAfterForceStop)) {
    throw "Package $packageName still has PID $pidAfterForceStop after force-stop."
}

$stepTwoOutput = Invoke-InstrumentationMethod `
    -MethodName $scenarioContract.StepTwo `
    -RequireFreshProcess
$pidAfterFreshInvocation = Invoke-DeviceAdb `
    -Arguments @("shell", "pidof", $packageName) `
    -AllowEmpty `
    -AllowNonZeroExit
$processProofLog = Invoke-DeviceAdb `
    -Arguments @("logcat", "-d", "-v", "brief", "$($scenarioContract.LogTag):I", "*:S") `
    -AllowEmpty
if (
    $processProofLog -notmatch "step=1 pid=\d+" -or
    $processProofLog -notmatch "step=2 previousPid=\d+ currentPid=\d+ freshRequired=true"
) {
    throw "The device log did not contain both process-proof checkpoints:`n$processProofLog"
}

$apiLevel = Invoke-DeviceAdb -Arguments @("shell", "getprop", "ro.build.version.sdk")
$utcTimestamp = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
$resolvedOutputDirectory = [IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
New-Item -ItemType Directory -Force -Path $resolvedOutputDirectory | Out-Null
$evidenceName = "$($scenarioContract.EvidencePrefix)-process-restart-$([DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')).txt"
$evidencePath = Join-Path $resolvedOutputDirectory $evidenceName
$evidence = @(
    "scenario=$($scenarioContract.Name)",
    "timestamp_utc=$utcTimestamp",
    "device_serial=$deviceSerial",
    "api_level=$apiLevel",
    "pid_before_force_stop=$pidBeforeForceStop",
    "pid_after_force_stop=$pidAfterForceStop",
    "pid_after_fresh_invocation=$pidAfterFreshInvocation",
    "step_one=PASS",
    "step_two=PASS",
    "process_restart_asserted=true",
    "",
    "[step_one_output]",
    $stepOneOutput,
    "",
    "[step_two_output]",
    $stepTwoOutput,
    "",
    "[process_proof_log]",
    $processProofLog
)
[IO.File]::WriteAllLines($evidencePath, $evidence, [Text.UTF8Encoding]::new($false))

Write-Output "$($scenarioContract.Name) process-restart proof passed on $deviceSerial (API $apiLevel)."
Write-Output "Evidence: $evidencePath"
