# 测试不同账户的登录功能
$baseUrl = "http://localhost:8081/api"
$loginUrl = "$baseUrl/auth/login"

# 测试管理员账户
Write-Host "测试账户: admin (管理员)"
Write-Host "---------------------------------------"
try {
    $response = Invoke-WebRequest -Uri $loginUrl -Method POST -ContentType "application/json" -Body '{"username": "admin", "password": "123456"}'
    $content = $response.Content | ConvertFrom-Json
    Write-Host "登录成功！状态码: $($response.StatusCode)"
    Write-Host "用户信息: $($content.data.username) - $($content.data.realName)"
} catch {
    Write-Host "登录失败: $($_.Exception.Message)"
}

Write-Host ""

# 测试维修工账户
Write-Host "测试账户: repair01 (维修工)"
Write-Host "---------------------------------------"
try {
    $response = Invoke-WebRequest -Uri $loginUrl -Method POST -ContentType "application/json" -Body '{"username": "repair01", "password": "123456"}'
    $content = $response.Content | ConvertFrom-Json
    Write-Host "登录成功！状态码: $($response.StatusCode)"
    Write-Host "用户信息: $($content.data.username) - $($content.data.realName)"
} catch {
    Write-Host "登录失败: $($_.Exception.Message)"
}

Write-Host ""

# 测试学生账户
Write-Host "测试账户: stu001 (学生)"
Write-Host "---------------------------------------"
try {
    $response = Invoke-WebRequest -Uri $loginUrl -Method POST -ContentType "application/json" -Body '{"username": "stu001", "password": "123456"}'
    $content = $response.Content | ConvertFrom-Json
    Write-Host "登录成功！状态码: $($response.StatusCode)"
    Write-Host "用户信息: $($content.data.username) - $($content.data.realName)"
} catch {
    Write-Host "登录失败: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "测试完成"