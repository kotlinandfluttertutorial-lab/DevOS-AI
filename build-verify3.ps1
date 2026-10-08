$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$logFile = "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\build-verify3.log"
"" | Set-Content $logFile

function Run-Step($label, [string[]]$gradleArgs) {
    "=== $label ===" | Add-Content $logFile
    $proc = Start-Process -FilePath "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\gradlew.bat" `
        -ArgumentList $gradleArgs `
        -WorkingDirectory "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI" `
        -RedirectStandardOutput "$logFile.tmp.out" `
        -RedirectStandardError  "$logFile.tmp.err" `
        -Wait -PassThru -NoNewWindow
    Get-Content "$logFile.tmp.out" | Add-Content $logFile
    Get-Content "$logFile.tmp.err" | Add-Content $logFile
    "EXIT: $($proc.ExitCode)" | Add-Content $logFile
    Remove-Item "$logFile.tmp.out","$logFile.tmp.err" -ErrorAction SilentlyContinue
    return $proc.ExitCode
}

$rc1 = Run-Step "core-database assembleDebug"     @(":core:core-database:assembleDebug",      "--no-daemon", "--rerun-tasks")
$rc2 = Run-Step "domain-repository assembleDebug" @(":domain:domain-repository:assembleDebug", "--no-daemon", "--rerun-tasks")
$rc3 = Run-Step "data-repository assembleDebug"   @(":data:data-repository:assembleDebug",     "--no-daemon", "--rerun-tasks")
$rc4 = Run-Step "data-repository tests"           @(":data:data-repository:testDebugUnitTest", "--no-daemon", "--rerun-tasks")
$rc5 = Run-Step "assembleDebug full"              @("assembleDebug",                           "--no-daemon", "--rerun-tasks")

"FINAL RESULTS: core-db=$rc1 domain-repo=$rc2 data-repo=$rc3 tests=$rc4 full=$rc5" | Add-Content $logFile
Write-Host "Build complete — see build-verify3.log"
