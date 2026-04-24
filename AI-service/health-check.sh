#!/bin/bash

# Script kiểm tra health của AI service sau khi deploy
# Sử dụng: ./health-check.sh <railway-url>

set -e

# Màu sắc cho output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Lấy URL từ argument hoặc hỏi user
if [ -z "$1" ]; then
    read -p "Nhập Railway URL (vd: https://your-service.up.railway.app): " RAILWAY_URL
else
    RAILWAY_URL=$1
fi

# Xóa trailing slash nếu có
RAILWAY_URL=${RAILWAY_URL%/}

echo "=========================================="
echo "    🏥 Health Check - AI Service"
echo "=========================================="
echo "URL: $RAILWAY_URL"
echo ""

# Test 1: Health endpoint
echo "Test 1: Health Check Endpoint"
echo "GET $RAILWAY_URL/health"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Status: $HTTP_CODE"
    echo "Response: $BODY"
else
    echo -e "${RED}❌ FAIL${NC} - Status: $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi
echo ""

# Test 2: Swagger UI
echo "Test 2: Swagger UI Documentation"
echo "GET $RAILWAY_URL/docs"
DOCS_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/docs")
HTTP_CODE=$(echo "$DOCS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Status: $HTTP_CODE"
    echo "Swagger UI is accessible"
else
    echo -e "${RED}❌ FAIL${NC} - Status: $HTTP_CODE"
fi
echo ""

# Test 3: OpenAPI JSON
echo "Test 3: OpenAPI Schema"
echo "GET $RAILWAY_URL/openapi.json"
OPENAPI_RESPONSE=$(curl -s -w "\n%{http_code}" "$RAILWAY_URL/openapi.json")
HTTP_CODE=$(echo "$OPENAPI_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Status: $HTTP_CODE"
    echo "OpenAPI schema is available"
else
    echo -e "${RED}❌ FAIL${NC} - Status: $HTTP_CODE"
fi
echo ""

# Test 4: CORS Headers
echo "Test 4: CORS Configuration"
echo "OPTIONS $RAILWAY_URL/health"
CORS_RESPONSE=$(curl -s -I -X OPTIONS "$RAILWAY_URL/health")

if echo "$CORS_RESPONSE" | grep -q "access-control-allow-origin"; then
    echo -e "${GREEN}✅ PASS${NC} - CORS headers present"
    echo "$CORS_RESPONSE" | grep "access-control"
else
    echo -e "${YELLOW}⚠️  WARNING${NC} - CORS headers not found"
fi
echo ""

# Test 5: Response Time
echo "Test 5: Response Time"
echo "GET $RAILWAY_URL/health"
RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" "$RAILWAY_URL/health")
RESPONSE_TIME_MS=$(echo "$RESPONSE_TIME * 1000" | bc)

if (( $(echo "$RESPONSE_TIME < 2" | bc -l) )); then
    echo -e "${GREEN}✅ PASS${NC} - Response time: ${RESPONSE_TIME_MS}ms"
else
    echo -e "${YELLOW}⚠️  SLOW${NC} - Response time: ${RESPONSE_TIME_MS}ms"
fi
echo ""

# Summary
echo "=========================================="
echo "    📊 Health Check Summary"
echo "=========================================="
echo -e "${GREEN}✅ Service is running${NC}"
echo ""
echo "Next steps:"
echo "1. Test API endpoints via Swagger UI:"
echo "   $RAILWAY_URL/docs"
echo ""
echo "2. Update CORS in main.py if needed"
echo ""
echo "3. Update backend URL in admin-panel:"
echo "   VITE_AI_SERVICE_URL=$RAILWAY_URL"
echo ""