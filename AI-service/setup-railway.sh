#!/bin/bash

# Script tự động setup và deploy AI-service lên Railway
# Sử dụng: ./setup-railway.sh

set -e

echo "=========================================="
echo "    🚂 Railway Auto Setup & Deploy"
echo "=========================================="
echo ""

# Kiểm tra Railway CLI
echo "📦 Checking Railway CLI..."
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI chưa được cài đặt!"
    echo ""
    echo "Cài đặt Railway CLI:"

    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
        echo "  Windows PowerShell (chạy as Administrator):"
        echo "  iwr https://railway.app/install.ps1 | iex"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "  macOS:"
        echo "  curl -fsSL https://railway.app/install.sh | sh"
    else
        echo "  Linux:"
        echo "  curl -fsSL https://railway.app/install.sh | sh"
    fi

    echo ""
    echo "Sau khi cài xong, chạy lại script này."
    exit 1
fi

echo "✅ Railway CLI đã được cài đặt"
echo ""

# Kiểm tra đăng nhập
echo "🔐 Checking Railway authentication..."
if ! railway whoami &> /dev/null; then
    echo "❌ Chưa đăng nhập Railway!"
    echo "Đang mở trình duyệt để đăng nhập..."
    railway login

    if ! railway whoami &> /dev/null; then
        echo "❌ Đăng nhập thất bại!"
        exit 1
    fi
fi

RAILWAY_USER=$(railway whoami)
echo "✅ Đã đăng nhập: $RAILWAY_USER"
echo ""

# Kiểm tra project đã được link chưa
echo "🔗 Checking Railway project..."
if ! railway status &> /dev/null; then
    echo "⚠️  Project chưa được link với Railway"
    echo ""
    echo "Chọn cách setup:"
    echo "1. Tạo project mới"
    echo "2. Link với project có sẵn"
    read -p "Chọn (1/2): " choice

    if [ "$choice" = "1" ]; then
        echo "Đang tạo project mới..."
        railway init
    else
        echo "Đang link với project có sẵn..."
        railway link
    fi
fi

echo "✅ Project đã được link"
railway status
echo ""

# Kiểm tra file .env
echo "🔧 Checking environment variables..."
if [ ! -f ".env" ]; then
    echo "⚠️  File .env không tồn tại"
    if [ -f ".env.example" ]; then
        echo "Đang copy từ .env.example..."
        cp .env.example .env
        echo "⚠️  Vui lòng cập nhật API keys trong .env"
    fi
fi

# Hỏi có muốn import env variables không
read -p "Import environment variables từ .env? (y/n): " import_env

if [ "$import_env" = "y" ] || [ "$import_env" = "Y" ]; then
    if [ -f ".env" ]; then
        echo "📝 Đang import variables..."

        # Import từng variable (bỏ qua comment và empty lines)
        while IFS='=' read -r key value; do
            # Skip comments and empty lines
            if [[ ! "$key" =~ ^#.* ]] && [[ -n "$key" ]]; then
                # Remove quotes from value
                value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

                # Skip APP_PORT vì Railway tự động set PORT
                if [ "$key" != "APP_PORT" ]; then
                    echo "Setting $key..."
                    railway variables set "$key=$value" 2>/dev/null || true
                fi
            fi
        done < .env

        echo "✅ Variables đã được import"
    else
        echo "❌ File .env không tồn tại"
    fi
else
    echo "⚠️  Bạn cần set environment variables thủ công sau:"
    echo "   railway variables set GOOGLE_API_KEY=your_key"
    echo "   railway variables set LLM_PROVIDER=gemini"
fi
echo ""

# Hiển thị variables hiện tại
echo "📋 Current environment variables:"
railway variables
echo ""

# Confirm deploy
read -p "Bạn có muốn deploy ngay bây giờ? (y/n): " deploy_now

if [ "$deploy_now" = "y" ] || [ "$deploy_now" = "Y" ]; then
    echo ""
    echo "🚀 Deploying to Railway..."
    railway up

    echo ""
    echo "⏳ Waiting for deployment to complete..."
    sleep 10

    echo ""
    echo "=========================================="
    echo "    ✅ Deployment Complete!"
    echo "=========================================="
    echo ""

    # Lấy domain
    echo "🌐 Getting service URL..."
    DOMAIN=$(railway domain 2>/dev/null || echo "")

    if [ -n "$DOMAIN" ]; then
        echo "Service URL: https://$DOMAIN"
        echo ""

        # Health check
        echo "🏥 Running health check..."
        sleep 5

        HEALTH_RESPONSE=$(curl -s "https://$DOMAIN/health" || echo "")
        if [ -n "$HEALTH_RESPONSE" ]; then
            echo "✅ Health check passed!"
            echo "Response: $HEALTH_RESPONSE"
        else
            echo "⚠️  Health check failed - service might still be starting"
        fi
    else
        echo "⚠️  Domain chưa được generate"
        echo "Generate domain: railway domain"
    fi

    echo ""
    echo "📊 Useful commands:"
    echo "  railway logs -f          # View logs"
    echo "  railway open             # Open in browser"
    echo "  railway domain           # Get domain"
    echo "  railway variables        # List variables"
    echo ""

    # Hỏi có muốn xem logs không
    read -p "Xem logs real-time? (y/n): " view_logs
    if [ "$view_logs" = "y" ] || [ "$view_logs" = "Y" ]; then
        railway logs -f
    fi
else
    echo ""
    echo "⏸️  Deploy bị hủy"
    echo ""
    echo "Deploy thủ công:"
    echo "  railway up"
fi

echo ""
echo "=========================================="
echo "    📚 Next Steps"
echo "=========================================="
echo ""
echo "1. Generate domain (nếu chưa có):"
echo "   railway domain"
echo ""
echo "2. Test service:"
echo "   ./health-check.sh https://your-domain.up.railway.app"
echo ""
echo "3. Update CORS in main.py với Railway domain"
echo ""
echo "4. Update admin-panel .env:"
echo "   VITE_AI_SERVICE_URL=https://your-domain.up.railway.app"
echo ""
echo "5. View documentation:"
echo "   cat QUICK_START.md"
echo ""
