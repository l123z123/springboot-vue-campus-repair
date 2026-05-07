# 1. 配置项 (请根据实际情况修改)
$port = 8080  # <--- 确认这里！如果是 8080 就改这里
$baseUrl = "http://localhost:$port/api"
$loginUrl = "$baseUrl/auth/login"

# 测试账号密码 (请替换为你数据库中真实的账号密码)
$username = "admin"
$password = "123456" 

Write-Host "----------------------------------------"
Write-Host "🚀 正在尝试登录..."
Write-Host "地址：$loginUrl"
Write-Host "账号：$username"
Write-Host "----------------------------------------"

# 2. 构建 JSON 请求体
$body = @{
    username = $username
    password = $password
} | ConvertTo-Json -Compress

# 3. 设置请求头 (告诉后端我发的是 JSON)
$headers = @{
    "Content-Type" = "application/json"
}

# 4. 发送 POST 请求
try {
    # -Method Post 是关键！
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Headers $headers -Body $body
    
    Write-Host "✅ 登录成功！响应数据：" -ForegroundColor Green
    $responseJson = $response | ConvertTo-Json -Depth 5
    Write-Host $responseJson

    # 5. 尝试提取 Token
    # 常见的返回结构有两种，我们做个判断
    $token = ""
    if ($response.token) {
        $token = $response.token
    } elseif ($response.data -and $response.data.token) {
        $token = $response.data.token
    } elseif ($response.code -eq 200 -and $response.data) {
        # 有些封装会返回 code:200, data:{token:...}
        if ($response.data.GetType().Name -eq "String") {
             $token = $response.data # 有时候 data 直接就是 token 字符串
        } elseif ($response.data.token) {
             $token = $response.data.token
        }
    }

    if ($token) {
        Write-Host "----------------------------------------"
        Write-Host "🎉 成功提取到 Token:" -ForegroundColor Cyan
        Write-Host $token
        Write-Host "----------------------------------------"
        
        # 保存到环境变量，方便下一步直接用
        $env:MY_JWT_TOKEN = $token
        Write-Host "💡 Token 已保存至环境变量 %MY_JWT_TOKEN%"
        Write-Host "💡 接下来运行上传脚本时，可直接使用 `$env:MY_JWT_TOKEN`"
    } else {
        Write-Host "⚠️ 登录成功但未找到 Token 字段，请检查上方返回的 JSON 结构。" -ForegroundColor Yellow
    }

} catch {
    Write-Host "❌ 登录请求失败！" -ForegroundColor Red
    Write-Host "错误信息：$($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Host "后端返回详情：$($_.ErrorDetails.Message)"
    }
    Write-Host "💡 提示：请检查账号密码是否正确，或者后端控制台是否有报错堆栈。"
}