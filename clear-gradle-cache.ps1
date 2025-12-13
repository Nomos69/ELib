# Clear Gradle cache script
Write-Host "Clearing Gradle cache..." -ForegroundColor Yellow

# Stop any running Gradle daemons
if (Test-Path "gradlew.bat") {
    Write-Host "Stopping Gradle daemons..." -ForegroundColor Cyan
    & .\gradlew.bat --stop 2>$null
}

# Delete project Gradle cache
if (Test-Path ".gradle") {
    Write-Host "Deleting .gradle folder..." -ForegroundColor Cyan
    Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
}

# Delete build folders
if (Test-Path "build") {
    Write-Host "Deleting build folder..." -ForegroundColor Cyan
    Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
}

if (Test-Path "app\build") {
    Write-Host "Deleting app\build folder..." -ForegroundColor Cyan
    Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue
}

# Clear user Gradle cache
$gradleCache = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCache) {
    Write-Host "Clearing user Gradle cache..." -ForegroundColor Cyan
    Remove-Item -Recurse -Force "$gradleCache\*" -ErrorAction SilentlyContinue
}

Write-Host "`nGradle cache cleared! Now:" -ForegroundColor Green
Write-Host "1. Open Android Studio" -ForegroundColor White
Write-Host "2. File -> Invalidate Caches / Restart" -ForegroundColor White
Write-Host "3. After restart, File -> Sync Project with Gradle Files" -ForegroundColor White

