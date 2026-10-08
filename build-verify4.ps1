$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$log = "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\bv4.log"
Set-Content $log ""
Set-Location "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI"

$gw = "f:\Android\AndroidStudioProjects\Kiro\DevOS-AI\gradlew.bat"

function Gradle-Run {
    param([string]$label, [string]$task)
    Add-Content $log "=== $label ==="
    & cmd /c "$gw $task --no-daemon --rerun-tasks >> `"$log`" 2>&1"
    $rc = $LASTEXITCODE
    Add-Content $log "EXIT=$rc"
    return $rc
}

$r1 = Gradle-Run "core-database"    ":core:core-database:assembleDebug"
$r2 = Gradle-Run "domain-repo"      ":domain:domain-repository:assembleDebug"
$r3 = Gradle-Run "data-repo"        ":data:data-repository:assembleDebug"
$r4 = Gradle-Run "data-repo-tests"  ":data:data-repository:testDebugUnitTest"
$r5 = Gradle-Run "full-build"       "assembleDebug"

Add-Content $log "FINAL: r1=$r1 r2=$r2 r3=$r3 r4=$r4 r5=$r5"
