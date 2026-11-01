# 拉取镜像并运行（Windows PowerShell）
#
# 用法：
#   .\scripts\pull-and-run.ps1                       # 拉取 aftersales-demo:1.0.0 并启动
#   .\scripts\pull-and-run.ps1 -Image 'ghcr.io/you/aftersales-demo:1.0.0'
#   .\scripts\pull-and-run.ps1 -DbHost 192.168.1.10 -DbPassword '你的密码'
#   .\scripts\pull-and-run.ps1 -Build                # 不拉镜像，改用本地源码构建
#
# 前提：目标 MySQL 已执行过后端的 database/init.sql。
# 想一条命令连数据库一起起，请用仓库根目录的 docker compose。

param(
    [string]$Image = 'aftersales-demo:1.0.0',
    [string]$Name = 'aftersales-app',
    [int]$Port = 8082,
    [string]$DbHost = 'host.docker.internal',
    [int]$DbPort = 3306,
    [string]$DbName = 'aftersales_demo',
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456',
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if ($Build) {
        Write-Host "[1/4] 从源码构建镜像 $Image ..." -ForegroundColor Cyan
        docker build -t $Image .
    } else {
        Write-Host "[1/4] 拉取镜像 $Image ..." -ForegroundColor Cyan
        docker pull $Image
    }

    Write-Host "[2/4] 清理同名容器（如果存在）..." -ForegroundColor Cyan
    docker rm -f $Name 2>$null | Out-Null

    Write-Host "[3/4] 启动容器 $Name ..." -ForegroundColor Cyan
    docker run -d --name $Name -p "${Port}:8082" `
        -e "DB_HOST=$DbHost" -e "DB_PORT=$DbPort" -e "DB_NAME=$DbName" `
        -e "DB_USERNAME=$DbUser" -e "DB_PASSWORD=$DbPassword" `
        --restart unless-stopped `
        $Image | Out-Null

    Write-Host "[4/4] 等待健康检查通过..." -ForegroundColor Cyan
    $ok = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        $status = (docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $Name) 2>$null
        if ($status -eq 'healthy' -or $status -eq 'running') { $ok = $true; break }
        Write-Host ("  ... " + $status)
    }

    if ($ok) {
        Write-Host ""
        Write-Host "启动成功： http://localhost:$Port/" -ForegroundColor Green
        Write-Host "演示账号： lin / hong / kefu_wang / wh_li ，密码统一 123456"
        Write-Host "查看日志： docker logs -f $Name"
        Write-Host "停止容器： docker rm -f $Name"
    } else {
        Write-Host "容器未在预期时间内就绪，请查看日志： docker logs $Name" -ForegroundColor Yellow
        exit 1
    }
}
finally {
    Pop-Location
}
