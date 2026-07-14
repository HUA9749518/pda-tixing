# 修复损坏的 gradle-wrapper.jar
# 用法：在 PowerShell 中执行 .\scripts\fix-gradle-wrapper.ps1

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
$urls = @(
    "https://raw.githubusercontent.com/gradle/gradle/v7.6.1/gradle/wrapper/gradle-wrapper.jar",
    "https://github.com/gradle/gradle/raw/v7.6.1/gradle/wrapper/gradle-wrapper.jar"
)

New-Item -ItemType Directory -Force -Path (Split-Path $jarPath) | Out-Null

foreach ($url in $urls) {
    Write-Host "Trying: $url"
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $jarPath -UseBasicParsing -TimeoutSec 120
        $size = (Get-Item $jarPath).Length
        if ($size -gt 50000) {
            Write-Host "OK: gradle-wrapper.jar ($size bytes)"
            exit 0
        }
        Write-Host "File too small ($size bytes), retry next mirror..."
    } catch {
        Write-Host "Failed: $_"
    }
}

Write-Host ""
Write-Host "自动下载失败。请在 Android Studio 中打开项目并 Sync，"
Write-Host "或手动将 gradle-wrapper.jar 放到: $jarPath"
Write-Host "（正常大小约 60KB+）"
exit 1
