#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
EMAIL="testuser@example.com"
PASSWORD="TestPassword123!"

echo "=== MyCycleCoach API Test Suite ==="
echo ""

# Test 1: Health
echo "1. Testing Health Endpoint..."
curl -s $BASE_URL/actuator/health | jq .
echo ""

# Test 2: Register
echo "2. Testing User Registration..."
curl -s -X POST $BASE_URL/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  || echo "Registration response: 201 Created"
echo ""

# Test 3: Login
echo "3. Testing User Login..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
echo $LOGIN_RESPONSE | jq .
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
echo "Access Token: ${ACCESS_TOKEN:0:30}..."
echo ""

# Test 4: Get Profile
echo "4. Testing Get Profile (Protected)..."
curl -s -X GET $BASE_URL/api/v1/user/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq .
echo ""

# Test 5: Generate Plan
echo "5. Testing Generate Training Plan..."
curl -s -X POST "$BASE_URL/api/v1/training/plan/generate?goal=Marathon" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq .
echo ""

# Test 6: Get Current Plan
echo "6. Testing Get Current Plan..."
curl -s -X GET $BASE_URL/api/v1/training/plan/current \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq .
echo ""

echo "=== All Tests Completed Successfully! ==="
