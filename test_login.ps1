# 测试不同账户的登录功能
$baseUrl = "http://localhost:8081/api"
$loginUrl = "$baseUrl/auth/login"

# 测试账户列表
$testAccounts = @(
    @{ username = "admin"; password = "123456"; role = "管理员" },
    @{ username = "repair01"; password = "123456"; role = "维修工" },
    @{ username = "repair02"; password = "123456"; role = "维修工" },
    @{ username = "stu001"; password = "123456"; role = "学生" },
    @{ username = "stu002"; password = "123456"; role = "学生" }
)

Write-Host "=======================================" -ForegroundColor Green
Write-Host "开始测试登录功能" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

foreach ($account in $testAccounts) {
    Write-Host "\n测试账户: $($account.username) ($($account.role))" -ForegroundColor Cyan
    Write-Host "---------------------------------------"
    
    $body = @{
        username = $account.username
        password = $account.password
    } | ConvertTo-Json -Compress
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Headers $headers -Body $body
        Write-Host "✅ 登录成功！" -ForegroundColor Green
        Write-Host "用户ID: $($response.data.userId)"
        Write-Host "用户名: $($response.data.username)"
        Write-Host "真实姓名: $($response.data.realName)"
        Write-Host "角色: $($response.data.role)"
        Write-Host "Token: $($response.data.token.Substring(0, 20))..."  # 只显示部分token
    } catch {
        Write-Host "❌ 登录失败: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) {
            Write-Host "错误详情: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
        }
    }
}

Write-Host "\n=======================================" -ForegroundColor Green
Write-Host "登录测试完成" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green