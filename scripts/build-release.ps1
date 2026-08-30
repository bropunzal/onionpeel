# Build signed release AAB (Play Store) and APK (GitHub fallback).
# Requires keystore.properties or ONIONPEEL_* env vars — see PLAY.md.
# Usage: .\scripts\build-release.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$propsFile = "$Root\keystore.properties"
if (-not (Test-Path $propsFile) -and -not $env:ONIONPEEL_KEYSTORE) {
    Write-Host "ERROR: No signing config found."
    Write-Host "  1. Copy keystore.properties.example to keystore.properties"
    Write-Host "  2. Generate a keystore: see PLAY.md"
    Write-Host "  Or set ONIONPEEL_KEYSTORE and related env vars."
    exit 1
}

$gradleOut = Get-Content "$Root\app\build.gradle.kts" -Raw
if ($gradleOut -match 'versionName\s*=\s*"([^"]+)"') { $version = $Matches[1] } else { $version = "0.0.0" }

Write-Host "Building OnionPeel $version (release) ..."
& "$Root\gradlew.bat" bundleRelease assembleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$destDir = "$Root\releases"
New-Item -ItemType Directory -Force -Path $destDir | Out-Null

$aabSrc = "$Root\app\build\outputs\bundle\release\app-release.aab"
$apkSrc = "$Root\app\build\outputs\apk\release\app-release.apk"
$aabDest = "$destDir\onionpeel-$version.aab"
$apkDest = "$destDir\onionpeel-$version.apk"

Copy-Item -Force $aabSrc $aabDest
Copy-Item -Force $apkSrc $apkDest

Write-Host ""
Write-Host "Release artifacts ready:"
Write-Host "  Play Store: $aabDest"
Write-Host "  GitHub APK: $apkDest"
Write-Host ""
Write-Host "Upload the AAB to Play Console closed testing - see PLAY.md"
