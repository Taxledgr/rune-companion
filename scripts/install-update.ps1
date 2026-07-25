[CmdletBinding()]
param(
    [string]$Device,
    [string]$ApkPath = ".\app\build\outputs\apk\debug\app-debug.apk",
    [switch]$Build,
    [string]$BuildDirectory = "",
    [string]$BackupDirectory = ""
)

$ErrorActionPreference = "Stop"
$packageName = "io.github.taxledgr.runecompanion"
$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$adbPath = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$gradlePath = Join-Path $workspace "gradlew.bat"
$buildToolsRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"

if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
    throw "ADB was not found at $adbPath. Install Android SDK Platform-Tools first."
}

if ([string]::IsNullOrWhiteSpace($Device)) {
    $connected = @(
        & $adbPath devices |
            Select-Object -Skip 1 |
            ForEach-Object { ($_ -split "\s+")[0] } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    )
    if ($connected.Count -ne 1) {
        throw "Connect exactly one authorised phone or pass -Device IP:PORT."
    }
    $Device = $connected[0]
}

if ($Device -match "^\d{1,3}(\.\d{1,3}){3}:\d+$") {
    $connectOutput = (& $adbPath connect $Device 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $connectOutput -match "failed|unable") {
        throw "Could not connect to $Device.`n$connectOutput"
    }
}

$deviceState = (& $adbPath -s $Device get-state 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $deviceState -ne "device") {
    throw "Android device $Device is not connected and authorised."
}

if ($Build) {
    if (-not (Test-Path -LiteralPath $gradlePath -PathType Leaf)) {
        throw "Gradle wrapper was not found at $gradlePath."
    }
    $javaHome = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path -LiteralPath $javaHome -PathType Container) {
        $env:JAVA_HOME = $javaHome
    }
    $buildArguments = @("assembleDebug")
    if (-not [string]::IsNullOrWhiteSpace($BuildDirectory)) {
        $buildArguments += "-PruneCompanionBuildDir=$BuildDirectory"
        $ApkPath = Join-Path $BuildDirectory "outputs\apk\debug\app-debug.apk"
    }
    & $gradlePath @buildArguments
    if ($LASTEXITCODE -ne 0) {
        throw "The Rune Companion build failed. Nothing was installed."
    }
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$apkHash = (Get-FileHash -LiteralPath $resolvedApk -Algorithm SHA256).Hash
$latestBuildTools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
$aaptPath = Join-Path $latestBuildTools.FullName "aapt.exe"
if (-not (Test-Path -LiteralPath $aaptPath -PathType Leaf)) {
    throw "Android aapt was not found. Install an Android SDK Build-Tools package."
}
$apkBadging = (& $aaptPath dump badging $resolvedApk 2>&1 | Out-String)
$apkIdentity = [regex]::Match(
    $apkBadging,
    "package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'"
)
if (-not $apkIdentity.Success) {
    throw "Could not verify the selected APK identity. Nothing was installed."
}
$apkPackageName = $apkIdentity.Groups[1].Value
$apkVersionCode = $apkIdentity.Groups[2].Value
$apkVersionName = $apkIdentity.Groups[3].Value
if ($apkPackageName -ne $packageName) {
    throw "The selected APK belongs to $apkPackageName, not Rune Companion."
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resolvedBackupRoot = if ([string]::IsNullOrWhiteSpace($BackupDirectory)) {
    Join-Path $env:LOCALAPPDATA "RuneCompanion\update-backups"
} elseif ([System.IO.Path]::IsPathRooted($BackupDirectory)) {
    [System.IO.Path]::GetFullPath($BackupDirectory)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $workspace $BackupDirectory))
}
$updateBackup = Join-Path $resolvedBackupRoot $timestamp
New-Item -ItemType Directory -Force -Path $updateBackup | Out-Null

function Export-AppData {
    param([string]$Target)

    $packageInstalled = (& $adbPath -s $Device shell pm path $packageName 2>$null)
    if ([string]::IsNullOrWhiteSpace(($packageInstalled | Out-String))) {
        return $false
    }
    $archivePaths = @(
        "shared_prefs",
        "databases"
    ) | Where-Object {
        & $adbPath -s $Device shell run-as $packageName test -d $_ 2>$null
        $LASTEXITCODE -eq 0
    }
    if ($archivePaths.Count -eq 0) {
        return $false
    }
    $arguments = @(
        "-s", $Device,
        "exec-out",
        "run-as", $packageName,
        "tar", "-cf", "-"
    ) + $archivePaths
    $process = Start-Process `
        -FilePath $adbPath `
        -ArgumentList $arguments `
        -RedirectStandardOutput $Target `
        -NoNewWindow `
        -Wait `
        -PassThru
    if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $Target)) {
        Remove-Item -LiteralPath $Target -ErrorAction SilentlyContinue
        return $false
    }
    return (Get-Item -LiteralPath $Target).Length -gt 0
}

function Get-PreferenceHashes {
    $hashes = [ordered]@{}
    $files = @(
        & $adbPath -s $Device shell run-as $packageName ls shared_prefs 2>$null
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    foreach ($file in $files) {
        $trimmedFile = $file.Trim()
        $hashOutput = (
            & $adbPath -s $Device shell run-as $packageName `
                sha256sum "shared_prefs/$trimmedFile" 2>$null |
                Out-String
        ).Trim()
        $hashMatch = [regex]::Match($hashOutput, "^([0-9a-fA-F]{64})\s")
        if ($hashMatch.Success) {
            $hashes[$trimmedFile] = $hashMatch.Groups[1].Value.ToUpperInvariant()
        }
    }
    return $hashes
}

$beforeArchive = Join-Path $updateBackup "before-update.tar"
$installedPackagePath = (
    & $adbPath -s $Device shell pm path $packageName 2>$null |
        Out-String
).Trim()
$wasInstalled = -not [string]::IsNullOrWhiteSpace($installedPackagePath)
$hadBackup = Export-AppData -Target $beforeArchive
if ($wasInstalled -and -not $hadBackup) {
    throw @"
Rune Companion is installed, but its private safety archive could not be created.
Nothing was updated. Do not uninstall the existing app.
"@
}
$beforeFiles = if ($hadBackup) {
    @(& $adbPath -s $Device shell run-as $packageName ls shared_prefs 2>$null) |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
} else {
    @()
}
$beforeHashes = Get-PreferenceHashes
$beforeHashes |
    ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $updateBackup "before-preference-hashes.json")

$installOutput = (& $adbPath -s $Device install -r $resolvedApk 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $installOutput -notmatch "Success") {
    throw @"
Rune Companion was NOT updated. The existing app was left installed.
Do not uninstall the app. The pre-update safety archive is at:
$beforeArchive

$installOutput
"@
}

$afterArchive = Join-Path $updateBackup "after-update.tar"
$afterBackupCreated = Export-AppData -Target $afterArchive
$afterFiles = @(& $adbPath -s $Device shell run-as $packageName ls shared_prefs 2>$null) |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
$missingFiles = @($beforeFiles | Where-Object { $_ -notin $afterFiles })
if ($missingFiles.Count -gt 0) {
    throw @"
The APK installed, but saved preference files are missing: $($missingFiles -join ", ")
Do not clear or uninstall Rune Companion. Keep the safety archive at:
$beforeArchive
"@
}
$afterHashes = Get-PreferenceHashes
$afterHashes |
    ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $updateBackup "after-preference-hashes.json")
$changedFiles = @(
    $beforeHashes.Keys |
        Where-Object {
            $afterHashes.Contains($_) -and
                $beforeHashes[$_] -ne $afterHashes[$_]
        }
)
if ($changedFiles.Count -gt 0) {
    throw @"
The APK installed, but saved preference files changed during installation: $($changedFiles -join ", ")
Do not clear or uninstall Rune Companion. Keep the safety archive at:
$beforeArchive
"@
}

$packageDetails = (& $adbPath -s $Device shell dumpsys package $packageName | Out-String)
$versionName = [regex]::Match($packageDetails, "versionName=([^\s]+)").Groups[1].Value
$versionCode = [regex]::Match($packageDetails, "versionCode=(\d+)").Groups[1].Value
if ($versionName -ne $apkVersionName -or $versionCode -ne $apkVersionCode) {
    throw @"
The installed version does not match the selected APK.
Expected: $apkVersionName ($apkVersionCode)
Installed: $versionName ($versionCode)
Keep the safety archive at:
$beforeArchive
"@
}

Write-Host ""
Write-Host "Rune Companion updated safely on $Device."
Write-Host "Installed version: $versionName ($versionCode)"
Write-Host "APK SHA-256: $apkHash"
Write-Host "Pre-update data archive: $beforeArchive"
if ($afterBackupCreated) {
    Write-Host "Post-update data archive: $afterArchive"
}
Write-Host "Existing saved preference files: verified present."
