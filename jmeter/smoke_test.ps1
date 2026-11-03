<#
.SYNOPSIS
    秒杀接口冒烟测试：用 tokens.csv 中的真实 token 打一次秒杀请求，验证鉴权与库存是否就绪。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\smoke_test.ps1
    powershell -ExecutionPolicy Bypass -File .\smoke_test.ps1 -VoucherId 10
#>
param(
    [long]$VoucherId = 10,
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$CsvPath = Join-Path $ScriptDir "tokens.csv"

# 取 CSV 第二行（第一条真实 token）
$line = (Get-Content $CsvPath)[1]
$token = $line.Split(',')[0]
Write-Host "使用 token: $token" -ForegroundColor Cyan

$url = "$BaseUrl/voucher-order/seckill/$VoucherId"
try {
    $resp = Invoke-WebRequest -Uri $url -Method POST -Headers @{ "authorization" = $token } -UseBasicParsing -TimeoutSec 10
    Write-Host "HTTP $($resp.StatusCode)" -ForegroundColor Green
    Write-Host "响应体: $($resp.Content)"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "请求失败 HTTP $code : $($_.Exception.Message)" -ForegroundColor Red
}
