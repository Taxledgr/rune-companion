[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Device,

    [string]$ApkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"
$adbPath = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
    throw "ADB was not found at $adbPath. Install Android SDK Platform-Tools first."
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$deviceState = (& $adbPath -s $Device get-state 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $deviceState -ne "device") {
    throw "Android device $Device is not connected and authorised."
}

$installOutput = (& $adbPath -s $Device install -r $resolvedApk 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $installOutput -notmatch "Success") {
    throw @"
Rune Companion was NOT updated. The existing app was left installed, so its saved data is intact.
Do not uninstall the app to work around this error. Use an APK signed with the same key and with a
higher version code.

$installOutput
"@
}

Write-Host "Rune Companion updated in place. Android app data was retained."
