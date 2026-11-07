# =====================================================================================
# depo-modernizasyon testlerini çalıştırır.
#
#   .\testleri-calistir.ps1            -> birim testleri + uçtan uca test (Docker gerekir)
#   .\testleri-calistir.ps1 -SadeceBirim -> yalnızca birim testleri (Docker gerekmez)
#
# JAVA_HOME tanımlı değilse %USERPROFILE%\dev-tools altındaki JDK 25 ve Maven kullanılır.
# Kapalı ağda Oracle imajı kurum registry'sinden alınacaksa:
#   .\testleri-calistir.ps1 -OracleImaj registry.kurum/gvenzl/oracle-free:23-slim-faststart
# =====================================================================================
param(
    [switch]$SadeceBirim,
    [string]$OracleImaj
)
$ErrorActionPreference = 'Stop'

$araclar = Join-Path $env:USERPROFILE 'dev-tools'
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $jdk = Get-ChildItem $araclar -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue | Select-Object -Last 1
    if (-not $jdk) { throw "JDK 25 bulunamadı. JAVA_HOME'u ayarlayın ya da JDK'yı $araclar altına açın." }
    $env:JAVA_HOME = $jdk.FullName
}
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $maven = Get-ChildItem $araclar -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue | Select-Object -Last 1
    if (-not $maven) { throw "Maven bulunamadı. PATH'e ekleyin ya da $araclar altına açın." }
    $env:Path = (Join-Path $maven.FullName 'bin') + ';' + $env:Path
}

Set-Location $PSScriptRoot
# Windows PowerShell 5.1, yerel komutların stderr satırlarını hata sayar; çıkış kodları esas alınır.
$ErrorActionPreference = 'Continue'
# 'clean': silinen bir kaynak dosya (ör. eski bir Flyway script'i) target/ altında kalıp testlere karışmasın.
$argumanlar = @('-B', 'clean')
if ($SadeceBirim) {
    $argumanlar += 'test'
} else {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Docker motoru çalışmıyor. Docker Desktop'ı açın (WSL2 kurulu olmalı) ya da -SadeceBirim kullanın." -ForegroundColor Red
        exit 2
    }
    $argumanlar += 'verify'
}
if ($OracleImaj) { $argumanlar += "-Dtest.oracle.imaj=$OracleImaj" }

& mvn @argumanlar
exit $LASTEXITCODE
