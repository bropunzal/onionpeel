# Build beta APK and copy to releases/ with versioned filename.
# Usage: .\scripts\build-beta.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$gradleOut = Get-Content "$Root\app\build.gradle.kts" -Raw
if ($gradleOut -match 'versionName\s*=\s*"([^"]+)"') { $version = $Matches[1] } else { $version = "0.0.0" }

Write-Host "Building OnionPeel $version ..."
& "$Root\gradlew.bat" assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$src = "$Root\app\build\outputs\apk\debug\app-debug.apk"
$destDir = "$Root\releases"
$dest = "$destDir\onionpeel-$version.apk"

New-Item -ItemType Directory -Force -Path $destDir | Out-Null
Copy-Item -Force $src $dest

Write-Host ""
Write-Host "Beta APK ready:"
Write-Host "  $dest"
Write-Host ""
Write-Host "Ship companion/ folder + BETA.md with this APK to closed testers."
