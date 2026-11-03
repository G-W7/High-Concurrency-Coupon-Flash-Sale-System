<#
.SYNOPSIS
    清理 hm-dianping 压测期间写入 Redis 的测试数据。

.DESCRIPTION
    删除 prepare_data.ps1 预置的登录 token、秒杀库存与一人一单记录，
    以及秒杀产生的订单消息队列 stream.orders，方便重复压测。
    注意：不会删除 MySQL 中已落库的 tb_voucher_order 数据，如需请手动清理。

.PARAMETER VoucherId
    需要清理库存的秒杀优惠券 id。默认 10。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\cleanup.ps1
    powershell -ExecutionPolicy Bypass -File .\cleanup.ps1 -VoucherId 10
#>

param(
    [long]$VoucherId = 10
)

$ErrorActionPreference = "Stop"
$Container = "hcfs-redis"

Write-Host "==== 清理压测 Redis 数据 ====" -ForegroundColor Cyan

$running = docker ps --filter "name=$Container" --format "{{.Names}}"
if ($running -notmatch $Container) {
    Write-Host "错误：未找到运行中的 Redis 容器 '$Container'。" -ForegroundColor Red
    exit 1
}

# 1. 删除所有预置的登录 token（login:token:*）
$tokens = docker exec $Container redis-cli --scan --pattern "login:token:*"
if ($tokens) {
    $tokenList = $tokens -split "`n" | Where-Object { $_ -ne "" }
    Write-Host "发现 $($tokenList.Count) 个 token，正在删除..."
    foreach ($t in $tokenList) {
        "DEL $t" | docker exec -i $Container redis-cli | Out-Null
    }
}

# 2. 删除秒杀库存、一人一单集合、订单消息队列
$cmds = @(
    "DEL seckill:stock:$VoucherId",
    "DEL seckill:order:$VoucherId",
    "DEL stream.orders"
)
($cmds -join "`n") | docker exec -i $Container redis-cli | Out-Null

Write-Host "清理完成。" -ForegroundColor Green
Write-Host "提示：数据库 tb_voucher_order 中的订单未清理，如需重置请手动执行 SQL。" -ForegroundColor Yellow
