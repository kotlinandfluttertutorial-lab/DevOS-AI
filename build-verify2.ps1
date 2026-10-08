$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$logFile = "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\build-verify.log"
"" | Set-Content $logFile

function Run-Step($label, $args) {
    "=== $label ===" | Add-Content $logFile
    $result = & "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\gradlew.bat" @args 2>&1
    $result | Add-Content $logFile
    $exit = $LASTEXITCODE
    "EXIT: $exit" | Add-Content $logFile
    return $exit
}

Set-Location "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI"

$rc1 = Run-Step "core-database assembleDebug"   @(":core:core-database:assembleDebug",      "--no-daemon", "--stacktrace")
$rc2 = Run-Step "domain-repository assembleDebug" @(":domain:domain-repository:assembleDebug",  "--no-daemon")
$rc3 = Run-Step "data-repository assembleDebug"  @(":data:data-repository:assembleDebug",    "--no-daemon")
$rc4 = Run-Step "data-repository tests"          @(":data:data-repository:testDebugUnitTest","--no-daemon")
$rc5 = Run-Step "assembleDebug (full)"           @("assembleDebug",                          "--no-daemon")

"RESULTS: core-db=$rc1 domain-repo=$rc2 data-repo=$rc3 tests=$rc4 full=$rc5" | Add-Content $logFile
Write-Host "Done. See build-verify.log"
