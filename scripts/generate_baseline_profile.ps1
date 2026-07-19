[CmdletBinding()]
param(
    [string]$DeviceSerial,
    [switch]$ResetAppData,
    [string]$OutputPath = "app/src/main/baseline-prof.txt"
)

<#
.SYNOPSIS
Collects Cinnamon's startup/CUJ Baseline Profile on a connected API 34+ device.

.DESCRIPTION
Builds the non-debuggable, non-minified benchmark variant, resets ART's profile,
executes startup plus the four primary tabs, asks ProfileInstaller to save the
runtime profile, and retrieves the human-readable rules. App data is preserved
unless -ResetAppData is explicitly supplied; use that switch only on a disposable
test device. Release signing credentials are neither read nor used.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "com.cinnamon.app"
$activityComponent = "$packageName/.MainActivity"
$receiver = "$packageName/androidx.profileinstaller.ProfileInstallReceiver"
$benchmarkApk = "app/build/outputs/apk/benchmark/app-benchmark.apk"
$deviceProfile = "/data/misc/profman/$packageName-primary.prof.txt"

function Find-Adb {
    $candidates = @(
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }
    if ($candidates.Count -gt 0) { return (Resolve-Path -LiteralPath $candidates[0]).Path }
    $fromPath = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) { return $fromPath.Source }
    throw "adb.exe was not found."
}

function Invoke-Adb {
    param([Parameter(Mandatory)][string[]]$Arguments)
    $result = & $script:adbPath -s $script:DeviceSerial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed: $($result -join [Environment]::NewLine)"
    }
    return ($result -join [Environment]::NewLine)
}

$adbPath = Find-Adb
if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $devices = & $adbPath devices | Select-String "\sdevice$" | ForEach-Object {
        ($_ -split "\s+")[0]
    }
    if ($devices.Count -ne 1) {
        throw "Pass -DeviceSerial when exactly one connected device is not available."
    }
    $DeviceSerial = $devices[0]
}

$apiLevel = [int](Invoke-Adb -Arguments @("shell", "getprop", "ro.build.version.sdk")).Trim()
if ($apiLevel -lt 34) {
    throw "Manual non-root profile collection requires API 34+; selected device is API $apiLevel."
}

& "$PSScriptRoot\..\gradlew.bat" :app:assembleBenchmark --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Benchmark APK build failed." }
$resolvedApk = (Resolve-Path -LiteralPath $benchmarkApk).Path

Invoke-Adb -Arguments @("install", "-r", $resolvedApk) | Out-Null
if ($ResetAppData) {
    $clearResult = Invoke-Adb -Arguments @("shell", "pm", "clear", $packageName)
    if ($clearResult -notmatch "Success") { throw "pm clear did not confirm success." }
}

Invoke-Adb -Arguments @(
    "shell", "am", "broadcast", "-a", "androidx.profileinstaller.action.SKIP_FILE",
    "--es", "EXTRA_SKIP_FILE_OPERATION", "WRITE_SKIP_FILE", $receiver
) | Out-Null
Invoke-Adb -Arguments @("shell", "cmd", "package", "compile", "-f", "-m", "verify", $packageName) | Out-Null
Invoke-Adb -Arguments @("shell", "pm", "art", "clear-app-profiles", $packageName) | Out-Null
Invoke-Adb -Arguments @("shell", "am", "force-stop", $packageName) | Out-Null
Invoke-Adb -Arguments @("shell", "am", "start", "-W", "-n", $activityComponent) | Out-Null

# Allow first-run seed/catalog work to settle, then exercise the four primary
# destinations using proportional phone coordinates. The profile remains valid
# if a route is still loading because startup methods are the critical target.
Start-Sleep -Seconds 12
$sizeText = Invoke-Adb -Arguments @("shell", "wm", "size")
$match = [regex]::Match($sizeText, "(\d+)x(\d+)")
if (-not $match.Success) { throw "Could not read device display size: $sizeText" }
$width = [int]$match.Groups[1].Value
$height = [int]$match.Groups[2].Value
$tabY = [int]($height * 0.955)
foreach ($fraction in @(0.37, 0.63, 0.88, 0.12)) {
    Invoke-Adb -Arguments @("shell", "input", "tap", [string][int]($width * $fraction), [string]$tabY) | Out-Null
    Start-Sleep -Milliseconds 900
}
Start-Sleep -Seconds 5

Invoke-Adb -Arguments @(
    "shell", "am", "broadcast", "-a", "androidx.profileinstaller.action.SAVE_PROFILE", $receiver
) | Out-Null
Start-Sleep -Seconds 1
Invoke-Adb -Arguments @("shell", "am", "force-stop", $packageName) | Out-Null
Invoke-Adb -Arguments @("shell", "pm", "dump-profiles", "--dump-classes-and-methods", $packageName) | Out-Null

$resolvedOutput = [IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputPath))
$outputDirectory = Split-Path -Parent $resolvedOutput
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$evidenceDirectory = [IO.Path]::GetFullPath((Join-Path (Get-Location) "output/performance"))
New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
$fullProfilePath = Join-Path $evidenceDirectory "baseline-profile-full-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
& $adbPath -s $DeviceSerial pull $deviceProfile $fullProfilePath
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $fullProfilePath)) {
    throw "Could not retrieve generated Baseline Profile from $deviceProfile."
}

$fullRules = @(Get-Content -LiteralPath $fullProfilePath | Where-Object { $_.Trim() })
# AndroidX/Kotlin dependencies already contribute their own AAR profiles. Keep
# Cinnamon-owned methods/classes so the app profile remains focused and does not
# AOT-compile tens of thousands of incidental framework methods.
$appRules = @($fullRules | Where-Object { $_ -match '^[HSP]*Lcom/cinnamon/app/' })
if ($appRules.Count -lt 100) {
    throw "Generated app profile is unexpectedly small: $($appRules.Count) Cinnamon-owned rules."
}
if ($appRules.Count -gt 10000) {
    throw "Generated app profile is unexpectedly broad: $($appRules.Count) Cinnamon-owned rules."
}
$appRules | Set-Content -LiteralPath $resolvedOutput -Encoding utf8
Write-Host "Focused Baseline Profile written to $resolvedOutput ($($appRules.Count) app rules)."
Write-Host "Full collection retained at $fullProfilePath ($($fullRules.Count) total rules)."
