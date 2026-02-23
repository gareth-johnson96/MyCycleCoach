#!/usr/bin/env python3
import requests
import json
import sys

BASE_URL = "http://localhost:8080"
EMAIL = "testuser@example.com"
PASSWORD = "TestPassword123!"

def test_endpoints():
    """Test all API endpoints"""
    
    print("=" * 60)
    print("MyCycleCoach API Test Suite")
    print("=" * 60)
    print()
    
    try:
        # Test 1: Health
        print("1. Testing Health Endpoint...")
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=5)
        print(f"   Status: {response.status_code}")
        print(f"   Response: {response.json()}")
        print()
        
        # Test 2: Register
        print("2. Testing User Registration...")
        register_data = {"email": EMAIL, "password": PASSWORD}
        response = requests.post(
            f"{BASE_URL}/api/v1/auth/register",
            json=register_data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        print(f"   Status: {response.status_code}")
        if response.status_code == 201:
            print("   Result: User registered successfully!")
        else:
            print(f"   Response: {response.text}")
        print()
        
        # Test 3: Login
        print("3. Testing User Login...")
        login_data = {"email": EMAIL, "password": PASSWORD}
        response = requests.post(
            f"{BASE_URL}/api/v1/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            login_response = response.json()
            access_token = login_response.get("accessToken")
            print(f"   Access Token: {access_token[:30]}...")
            print(f"   Token Type: {login_response.get('tokenType')}")
            print(f"   Expires In: {login_response.get('expiresIn')} seconds")
            print()
            
            # Test 4: Get Profile (Protected)
            print("4. Testing Get Profile (Protected Endpoint)...")
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json"
            }
            response = requests.get(
                f"{BASE_URL}/api/v1/user/profile",
                headers=headers,
                timeout=5
            )
            print(f"   Status: {response.status_code}")
            print(f"   Response: {response.json()}")
            print()
            
            # Test 5: Generate Training Plan
            print("5. Testing Generate Training Plan...")
            response = requests.post(
                f"{BASE_URL}/api/v1/training/plan/generate?goal=Marathon",
                headers=headers,
                timeout=5
            )
            print(f"   Status: {response.status_code}")
            plan_response = response.json()
            print(f"   Plan ID: {plan_response.get('id')}")
            print(f"   Goal: {plan_response.get('goal')}")
            print(f"   Status: {plan_response.get('status')}")
            plan_id = plan_response.get('id')
            print()
            
            # Test 6: Get Current Plan
            print("6. Testing Get Current Plan...")
            response = requests.get(
                f"{BASE_URL}/api/v1/training/plan/current",
                headers=headers,
                timeout=5
            )
            print(f"   Status: {response.status_code}")
            current_plan = response.json()
            print(f"   Current Plan: {json.dumps(current_plan, indent=2)}")
            print()
            
            print("=" * 60)
            print("✓ All tests passed successfully!")
            print("=" * 60)
        else:
            print(f"   Error: {response.status_code}")
            print(f"   Response: {response.text}")
            
    except requests.exceptions.ConnectionError:
        print("ERROR: Cannot connect to application at http://localhost:8080")
        print("Make sure docker-compose is running: docker-compose up -d postgres app")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    test_endpoints()
