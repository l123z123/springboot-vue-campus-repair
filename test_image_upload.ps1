# 测试图片上传功能
$token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZSI6MiwiaWF0IjoxNzc2ODk0NzQ5LCJleHAiOjE3NzY5MDE5NDl9.EPEywvmfRr0w1-a52bY_SQSL_Enn0v4AaLKsUqJJHS8"
$uploadUrl = "http://localhost:8081/api/file/upload"

# 创建一个简单的测试文件
$testContent = "This is a test file for image upload"
$testFile = "test_upload.txt"
Set-Content -Path $testFile -Value $testContent

Write-Host "Testing file upload..."

# 构建 multipart/form-data 请求
$boundary = [System.Guid]::NewGuid().ToString()
$fileBytes = [System.IO.File]::ReadAllBytes($testFile)
$fileContent = [System.Text.Encoding]::UTF8.GetString($fileBytes)

$body = "--$boundary`r`n"
$body += "Content-Disposition: form-data; name=`"file`"; filename=`"$testFile`"`r`n"
$body += "Content-Type: text/plain`r`n`r`n"
$body += "$fileContent`r`n"
$body += "--$boundary--`r`n"

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "multipart/form-data; boundary=$boundary"
}

try {
    $response = Invoke-WebRequest -Uri $uploadUrl -Method POST -Headers $headers -Body $body -UseBasicParsing
    Write-Host "Upload successful!"
    Write-Host "Response: $($response.Content)"
} catch {
    Write-Host "Upload failed: $($_.Exception.Message)"
    if ($_.ErrorDetails) {
        Write-Host "Error details: $($_.ErrorDetails.Message)"
    }
}

# 清理测试文件
Remove-Item -Path $testFile -ErrorAction SilentlyContinue