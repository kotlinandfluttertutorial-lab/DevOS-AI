$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI"

Write-Host "=== Step 1: core-database ===" -ForegroundColor Cyan
.\gradlew.bat :core:core-database:assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: core-database" -ForegroundColor Red; exit 1 }

Write-Host "=== Step 2: domain-repository ===" -ForegroundColor Cyan
.\gradlew.bat :domain:domain-repository:assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: domain-repository" -ForegroundColor Red; exit 1 }

Write-Host "=== Step 3: data-repository ===" -ForegroundColor Cyan
.\gradlew.bat :data:data-repository:assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: data-repository" -ForegroundColor Red; exit 1 }

Write-Host "=== Step 4: data-repository tests ===" -ForegroundColor Cyan
.\gradlew.bat :data:data-repository:testDebugUnitTest --no-daemon
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: data-repository tests" -ForegroundColor Red; exit 1 }

Write-Host "=== Step 5: full assembleDebug ===" -ForegroundColor Cyan
.\gradlew.bat assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: assembleDebug" -ForegroundColor Red; exit 1 }

Write-Host "=== ALL CHECKS PASSED ===" -ForegroundColor Green
