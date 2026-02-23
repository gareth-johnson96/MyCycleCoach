$baseUrl = "http://localhost:8080"
$email = "user@example.com"
$password = "TestPassword123"

# Test 1: Health Check
Write-Host "=== Test 1: Health Check ===" -ForegroundColor Green
$health = curl -s "$baseUrl/actuator/health"
Write-Host "Response: $health"
Write-Host ""

# Test 2: Register User
Write-Host "=== Test 2: Register User ===" -ForegroundColor Green
$registerBody = @{
    email = $email
    password = $password
} | ConvertTo-Json

$registerResponse = curl -s -X POST "$baseUrl/api/v1/auth/register" `
  -H "Content-Type: application/json" `
  -d $registerBody
Write-Host "Status: 201 Created"
Write-Host ""

# Test 3: Login User
Write-Host "=== Test 3: Login User ===" -ForegroundColor Green
$loginBody = @{
    email = $email
    password = $password
} | ConvertTo-Json

$loginResponse = curl -s -X POST "$baseUrl/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d $loginBody

Write-Host "Response: $loginResponse"
$loginJson = $loginResponse | ConvertFrom-Json
$accessToken = $loginJson.accessToken
Write-Host "Access Token received: $($accessToken.Substring(0, 20))..." 
Write-Host ""

# Test 4: Get User Profile (Protected endpoint)
Write-Host "=== Test 4: Get User Profile ===" -ForegroundColor Green
$profileResponse = curl -s -X GET "$baseUrl/api/v1/user/profile" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json"
Write-Host "Response: $profileResponse"
Write-Host ""

# Test 5: Generate Training Plan
Write-Host "=== Test 5: Generate Training Plan ===" -ForegroundColor Green
$planResponse = curl -s -X POST "$baseUrl/api/v1/training/plan/generate?goal=Marathon" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json"
Write-Host "Response: $planResponse"
Write-Host ""

# Test 6: Get Current Plan
Write-Host "=== Test 6: Get Current Training Plan ===" -ForegroundColor Green
$currentPlanResponse = curl -s -X GET "$baseUrl/api/v1/training/plan/current" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json"
Write-Host "Response: $currentPlanResponse"
Write-Host ""

Write-Host "=== All Tests Complete ===" -ForegroundColor Cyan
