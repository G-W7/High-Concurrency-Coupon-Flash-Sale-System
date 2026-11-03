<#
.SYNOPSIS
    hm-dianping 秒杀压测数据准备脚本

.DESCRIPTION
    通过 docker 容器 hcfs-redis 内的 redis-cli，向 Redis 批量写入：
      1. 多个不同用户的登录 token（key: login:token:{uuid}，hash 至少含 id 字段）
      2. 秒杀库存（key: seckill:stock:{voucherId}），并清空历史购买记录（seckill:order:{voucherId}）
    同时把 token,userId 导出到 tokens.csv，供 JMeter 的 CSV Data Set Config 参数化使用。

    由于 RefreshTokenIntercepter 只从 Redis 的 token hash 还原 UserDTO，
    因此这里无需在 MySQL 中真实创建用户，userId 使用虚拟值即可跑通秒杀链路。

.PARAMETER Count
    生成的 token（并发用户）数量，需 >= JMeter 线程组的线程数。默认 200。

.PARAMETER VoucherId
    压测的秒杀优惠券 id。默认 10。

.PARAMETER Stock
    预热到 Redis 的库存数量，建议远大于 Count，避免"库存不足"干扰。默认 100000。

.PARAMETER UserStart
    虚拟 userId 的起始值。默认 100000（即 userId = 100001, 100002 ...）。

.PARAMETER TokenTtl
    token 过期时间（秒）。默认 7200（2 小时），保证压测期间不过期。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\prepare_data.ps1
    powershell -ExecutionPolicy Bypass -File .\prepare_data.ps1 -Count 500 -VoucherId 10 -Stock 200000
#>

param(
    [int]$Count = 200,
    [long]$VoucherId = 10,
    [long]$Stock = 100000,
    [long]$UserStart = 100000,
    [int]$TokenTtl = 7200
)

$ErrorActionPreference = "Stop"
$Container = "hcfs-redis"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$CsvPath = Join-Path $ScriptDir "tokens.csv"

Write-Host "==== hm-dianping 秒杀压测数据准备 ====" -ForegroundColor Cyan
Write-Host "并发用户数(token): $Count | 优惠券id: $VoucherId | 库存: $Stock"

# 1. 检查 redis 容器是否运行
$running = docker ps --filter "name=$Container" --format "{{.Names}}"
if ($running -notmatch $Container) {
    Write-Host "错误：未找到运行中的 Redis 容器 '$Container'。请先执行 docker-compose up -d" -ForegroundColor Red
    exit 1
}

# 2. 组装 redis-cli 命令（一次性通过 stdin 批量执行，避免逐条 docker exec 太慢）
$cmds = New-Object System.Collections.Generic.List[string]

# 2.1 库存预热 + 清空一人一单记录
$cmds.Add("SET seckill:stock:$VoucherId $Stock")
$cmds.Add("DEL seckill:order:$VoucherId")

# 2.2 批量写入 token（hash 必须含 id 字段，否则 UserHolder.getUser().getId() 会 NPE）
$csvLines = New-Object System.Collections.Generic.List[string]
$csvLines.Add("token,userId")

for ($i = 1; $i -le $Count; $i++) {
    $token = [guid]::NewGuid().ToString()
    $userId = $UserStart + $i
    # nickName / icon 使用无空格占位值，规避 redis-cli 内联命令的引号解析问题
    $cmds.Add("HSET login:token:$token id $userId nickName user_$userId icon na")
    $cmds.Add("EXPIRE login:token:$token $TokenTtl")
    $csvLines.Add("$token,$userId")
}

# 3. 执行 redis 命令
$payload = ($cmds -join "`n")
$payload | docker exec -i $Container redis-cli | Out-Null
Write-Host "已写入 $Count 个 token 到 Redis，并预热库存 seckill:stock:$VoucherId = $Stock" -ForegroundColor Green

# 4. 导出 tokens.csv（UTF8 无 BOM）
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($CsvPath, $csvLines, $utf8NoBom)
Write-Host "已导出 CSV: $CsvPath" -ForegroundColor Green

Write-Host ""
Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "  1) 启动项目 HCFSApplication（端口 8081）"
Write-Host "  2) GUI 调试:  jmeter -t seckill.jmx"
Write-Host "  3) 正式压测:  jmeter -n -t seckill.jmx -l result.jtl -e -o report_html"
