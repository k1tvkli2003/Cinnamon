[CmdletBinding()]
param(
    [ValidateRange(1, 30)]
    [int]$Runs = 5,
    [ValidateRange(5, 120)]
    [int]$ReadyTimeoutSeconds = 30,
    [string]$DeviceSerial,
    [switch]$ResetAppDataEachRun,
    [string]$BuildLabel = "unspecified",
    [string]$OutputDirectory = "output/performance"
)

<#!
.SYNOPSIS
Captures repeatable, local Android startup timing for Cinnamon.

.DESCRIPTION
The default scenario is process-cold startup with existing app data: the app is
force-stopped, not cleared, then started while the opt-in CinnamonStartup tag
is collected. Pass -ResetAppDataEachRun only when a disposable test emulator is
selected and a fresh-app-data scenario is intentionally required. That switch
erases Cinnamon's app data on the selected device before every run.

The script emits only startup stage durations and am start metrics. It does not
collect learner text, identifiers, or content values.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "com.cinnamon.app"
$activityComponent = "$packageName/.MainActivity"
$startupTag = "CinnamonStartup"

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
        [string[]]$Arguments
    )

    $output = & $script:adbPath @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed: $($output -join [Environment]::NewLine)"
    }
    return ($output -join [Environment]::NewLine)
}

function Invoke-DeviceAdb {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    return Get-AdbOutput -Arguments (@("-s", $script:deviceSerial) + $Arguments)
}

function Get-NumberFromAmStart {
    param(
        [Parameter(Mandatory)]
        [string]$Text,
        [Parameter(Mandatory)]
        [string]$Name
    )

    $match = [regex]::Match($Text, "(?m)^$([regex]::Escape($Name)):\s*(\d+)")
    if ($match.Success) {
        return [int]$match.Groups[1].Value
    }
    return $null
}

function Get-TraceStageMilliseconds {
    param(
        [Parameter(Mandatory)]
        [string]$Text,
        [Parameter(Mandatory)]
        [string]$Stage
    )

    $matches = [regex]::Matches($Text, "\b$([regex]::Escape($Stage))=(\d+)ms")
    if ($matches.Count -eq 0) {
        return $null
    }
    return [int]$matches[$matches.Count - 1].Groups[1].Value
}

function Get-Percentile {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        [AllowEmptyCollection()]
        [object[]]$Values,
        [Parameter(Mandatory)]
        [ValidateRange(0.0, 1.0)]
        [double]$Percentile
    )

    $ordered = @($Values | Where-Object { $null -ne $_ } | Sort-Object)
    if ($ordered.Count -eq 0) {
        return $null
    }
    $index = [Math]::Max(0, [Math]::Ceiling($Percentile * $ordered.Count) - 1)
    return $ordered[$index]
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
$packagePath = Invoke-DeviceAdb -Arguments @("shell", "pm", "path", $packageName)
if ([string]::IsNullOrWhiteSpace($packagePath) -or $packagePath -notmatch "^package:") {
    throw "Package $packageName is not installed on $deviceSerial. Install the intended debug/profile APK first."
}

$resolvedOutputDirectory = [IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
New-Item -ItemType Directory -Force -Path $resolvedOutputDirectory | Out-Null

$runResults = [System.Collections.Generic.List[object]]::new()
$tagWasEnabled = $false

try {
    Invoke-DeviceAdb -Arguments @("shell", "setprop", "log.tag.$startupTag", "DEBUG") | Out-Null
    $tagWasEnabled = $true

    for ($run = 1; $run -le $Runs; $run++) {
        if ($ResetAppDataEachRun) {
            $clearResult = Invoke-DeviceAdb -Arguments @("shell", "pm", "clear", $packageName)
            if ($clearResult -notmatch "Success") {
                throw "pm clear did not confirm Success for run ${run}: $clearResult"
            }
        }

        Invoke-DeviceAdb -Arguments @("logcat", "-c") | Out-Null
        Invoke-DeviceAdb -Arguments @("shell", "am", "force-stop", $packageName) | Out-Null

        $startOutput = Invoke-DeviceAdb -Arguments @("shell", "am", "start", "-W", "-n", $activityComponent)
        $deadline = [Diagnostics.Stopwatch]::StartNew()
        $traceOutput = ""

        while ($deadline.Elapsed.TotalSeconds -lt $ReadyTimeoutSeconds) {
            $traceOutput = Invoke-DeviceAdb -Arguments @("logcat", "-d", "-s", "${startupTag}:D", "*:S")
            if ($traceOutput -match "\bprepare=\d+ms") {
                break
            }
            Start-Sleep -Milliseconds 250
        }

        $catalogMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "catalog"
        $snapshotMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "progress_snapshot"
        $importMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "progress_import"
        $lexiconMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "lexicon_seed"
        $reconciliationMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "catalog_reconciliation"
        $prepareMs = Get-TraceStageMilliseconds -Text $traceOutput -Stage "prepare"

        $progressPipelineMs = if ($null -ne $snapshotMs -and $null -ne $importMs) { $snapshotMs + $importMs } else { $null }
        $stageSumMs = if (
            $null -ne $catalogMs -and
            $null -ne $progressPipelineMs -and
            $null -ne $lexiconMs -and
            $null -ne $reconciliationMs
        ) {
            $catalogMs + $progressPipelineMs + $lexiconMs + $reconciliationMs
        } else {
            $null
        }
        $overlapMs = if ($null -ne $stageSumMs -and $null -ne $prepareMs) {
            [Math]::Max(0, $stageSumMs - $prepareMs)
        } else {
            $null
        }

        $runResults.Add([PSCustomObject]@{
            run = $run
            started_at = (Get-Date).ToString("o")
            scenario = if ($ResetAppDataEachRun) { "fresh-app-data" } else { "process-cold-existing-data" }
            am_this_time_ms = Get-NumberFromAmStart -Text $startOutput -Name "ThisTime"
            am_total_time_ms = Get-NumberFromAmStart -Text $startOutput -Name "TotalTime"
            catalog_ms = $catalogMs
            progress_snapshot_ms = $snapshotMs
            progress_import_ms = $importMs
            progress_pipeline_ms = $progressPipelineMs
            lexicon_seed_ms = $lexiconMs
            catalog_reconciliation_ms = $reconciliationMs
            prepare_ms = $prepareMs
            independent_stage_sum_ms = $stageSumMs
            independent_overlap_ms = $overlapMs
            trace_ready = $null -ne $prepareMs
        })
    }
} finally {
    if ($tagWasEnabled) {
        & $adbPath -s $deviceSerial shell setprop "log.tag.$startupTag" INFO | Out-Null
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonPath = Join-Path $resolvedOutputDirectory "startup-$timestamp.json"
$csvPath = Join-Path $resolvedOutputDirectory "startup-$timestamp.csv"
$prepareValues = @($runResults | ForEach-Object { $_.prepare_ms })
$reconciliationValues = @($runResults | ForEach-Object { $_.catalog_reconciliation_ms })
$overlapValues = @($runResults | ForEach-Object { $_.independent_overlap_ms })
$amTotalValues = @($runResults | ForEach-Object { $_.am_total_time_ms })
$summary = [PSCustomObject]@{
    runs_requested = $Runs
    runs_with_trace = @($runResults | Where-Object { $_.trace_ready }).Count
    am_total_time_p50_ms = Get-Percentile -Values $amTotalValues -Percentile 0.50
    am_total_time_p95_ms = Get-Percentile -Values $amTotalValues -Percentile 0.95
    prepare_p50_ms = Get-Percentile -Values $prepareValues -Percentile 0.50
    prepare_p95_ms = Get-Percentile -Values $prepareValues -Percentile 0.95
    catalog_reconciliation_p50_ms = Get-Percentile -Values $reconciliationValues -Percentile 0.50
    catalog_reconciliation_p95_ms = Get-Percentile -Values $reconciliationValues -Percentile 0.95
    overlap_p50_ms = Get-Percentile -Values $overlapValues -Percentile 0.50
    overlap_p95_ms = Get-Percentile -Values $overlapValues -Percentile 0.95
}
$report = [PSCustomObject]@{
    metadata = [PSCustomObject]@{
        package = $packageName
        activity = $activityComponent
        device_serial = $deviceSerial
        build_mode = $BuildLabel
        scenario = if ($ResetAppDataEachRun) { "fresh app data on a disposable device" } else { "process-cold existing app data" }
        ready_timeout_seconds = $ReadyTimeoutSeconds
        created_at = (Get-Date).ToString("o")
    }
    summary = $summary
    runs = @($runResults)
}

$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding utf8
$runResults | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding utf8

Write-Host "Startup timing written to:"
Write-Host "  $jsonPath"
Write-Host "  $csvPath"
Write-Host "Trace-ready runs: $($summary.runs_with_trace)/$($summary.runs_requested)"
Write-Host "am TotalTime p50/p95: $($summary.am_total_time_p50_ms)/$($summary.am_total_time_p95_ms) ms"
Write-Host "prepare p50/p95: $($summary.prepare_p50_ms)/$($summary.prepare_p95_ms) ms"
Write-Host "catalog reconciliation p50/p95: $($summary.catalog_reconciliation_p50_ms)/$($summary.catalog_reconciliation_p95_ms) ms"
Write-Host "overlap p50/p95: $($summary.overlap_p50_ms)/$($summary.overlap_p95_ms) ms"
