# Copy release jar and notes into dist/ for Modrinth / manual upload.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path "$root\build.gradle")) {
    $root = Split-Path -Parent $PSScriptRoot
}
Set-Location $root

$props = Get-Content "gradle.properties" | Where-Object { $_ -match '^mod_version=' }
$version = ($props -split '=', 2)[1].Trim()
$jarName = "effecoria-$version.jar"
$srcJar = Join-Path $root "build\libs\$jarName"

if (-not (Test-Path $srcJar)) {
    Write-Host "Building $jarName ..."
    & .\gradlew.bat build --no-daemon -q
}

$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
Copy-Item -Force $srcJar (Join-Path $dist $jarName)

$notes = Join-Path $root "docs\monetization\RELEASE_$version.md"
if (Test-Path $notes) {
    Copy-Item -Force $notes (Join-Path $dist "RELEASE_$version.md")
}
Copy-Item -Force (Join-Path $root "docs\monetization\KNOWN_ISSUES.md") (Join-Path $dist "KNOWN_ISSUES.md")

Write-Host "Packaged: $dist\$jarName"
