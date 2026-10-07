$repo = "F:\Android\AndroidStudioProjects\Kiro\DevOS-AI"
Set-Location $repo

Write-Host "=== Step 1: Verify token is gone from working tree ===" -ForegroundColor Cyan
$content = Get-Content "docs\figma\figma-ui-remove-later.md" -Raw
if ($content -match "figd_") {
    Write-Host "ERROR: Token still present in working tree file!" -ForegroundColor Red
    exit 1
} else {
    Write-Host "OK: Token not found in working tree." -ForegroundColor Green
}

Write-Host "`n=== Step 2: Soft-reset to c391cea ===" -ForegroundColor Cyan
git reset --soft c391cea
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR on reset" -ForegroundColor Red; exit 1 }
Write-Host "OK: Reset done." -ForegroundColor Green

Write-Host "`n=== Step 3: Stage the 4 files ===" -ForegroundColor Cyan
git add "docs/figma/figma-ui-remove-later.md"
git add "docs/mockups/devos-ai-mockups.html"
git add ".kiro/settings/mcp.json"
git add "gradle/gradle-daemon-jvm.properties"
git status --short
Write-Host "OK: Files staged." -ForegroundColor Green

Write-Host "`n=== Step 4: Commit without the token ===" -ForegroundColor Cyan
git commit -m "Add DevOS AI mobile UI mockups, Gradle daemon JVM properties, and MCP settings"
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR on commit" -ForegroundColor Red; exit 1 }
Write-Host "OK: Commit done." -ForegroundColor Green

Write-Host "`n=== Step 5: Merge origin/main ===" -ForegroundColor Cyan
git merge origin/main --no-edit
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR on merge" -ForegroundColor Red; exit 1 }
Write-Host "OK: Merge done." -ForegroundColor Green

Write-Host "`n=== Step 6: Force-push ===" -ForegroundColor Cyan
git push --force-with-lease origin main
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR on push" -ForegroundColor Red; exit 1 }
Write-Host "`nSUCCESS: Push complete. Token scrubbed from history." -ForegroundColor Green

Write-Host "`n=== Final log ===" -ForegroundColor Cyan
git log --oneline -6
