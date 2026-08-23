[CmdletBinding()]
param(
    [string]$DeviceSerial,
    [switch]$AllowPhysicalDevice,
    [string]$OutputDirectory = "qa/runtime-proof"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$arguments = @{
    Scenario = "Quest"
    OutputDirectory = $OutputDirectory
}
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $arguments.DeviceSerial = $DeviceSerial
}
if ($AllowPhysicalDevice) {
    $arguments.AllowPhysicalDevice = $true
}

& (Join-Path $PSScriptRoot "verify_gamification_process_restart.ps1") @arguments
