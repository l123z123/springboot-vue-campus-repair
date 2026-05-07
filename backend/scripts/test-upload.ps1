# Upload test script for Windows PowerShell 5.1
# Usage: .\test-upload.ps1 -Token "JWT" -FilePath "F:\path\to\image.jpg"
# Token with or without "Bearer " prefix. On 401, login again to get a new token.

param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [Parameter(Mandatory = $true)]
    [string]$FilePath,
    [string]$BaseUrl = "http://localhost:8081/api"
)

if ($Token -match '^\s*Bearer\s+') { $Token = $Token -replace '^\s*Bearer\s+', '' }
$Token = $Token.Trim()

Add-Type -AssemblyName System.Net.Http

if (-not (Test-Path $FilePath)) {
    Write-Host "File not found: $FilePath" -ForegroundColor Red
    exit 1
}

$fileName = [System.IO.Path]::GetFileName($FilePath)
$fileBytes = [System.IO.File]::ReadAllBytes($FilePath)

$httpClient = $null
$formData = $null
try {
    $formData = New-Object System.Net.Http.MultipartFormDataContent
    $fileContent = New-Object System.Net.Http.ByteArrayContent(,$fileBytes)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")
    $formData.Add($fileContent, "file", $fileName)

    $httpClient = New-Object System.Net.Http.HttpClient
    $httpClient.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)
    $httpClient.Timeout = [TimeSpan]::FromSeconds(30)

    $response = $httpClient.PostAsync("$BaseUrl/file/upload", $formData).Result
    $content = $response.Content.ReadAsStringAsync().Result

    if ($response.IsSuccessStatusCode) {
        Write-Host "Upload OK (HTTP $($response.StatusCode))" -ForegroundColor Green
        Write-Host $content
    } else {
        Write-Host "Upload failed (HTTP $($response.StatusCode))" -ForegroundColor Red
        Write-Host $content
        if ($response.StatusCode -eq 401) {
            Write-Host "Tip: 401 usually means token expired. Login again and use the new token." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Request error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.InnerException) { Write-Host $_.Exception.InnerException.Message }
} finally {
    if ($httpClient) { $httpClient.Dispose() }
    if ($formData) { $formData.Dispose() }
}
