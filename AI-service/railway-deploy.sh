#!/bin/bash

# Script deploy nhanh lên Railway
# Sử dụng: ./railway-deploy.sh

set -e

echo "=========================================="
echo "    🚂 Railway Deployment Script"
echo "=========================================="

# Kiểm tra Railway CLI
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI chưa được cài đặt!"
    echo ""
    echo "Cài đặt Railway CLI:"
    echo "  Windows: iwr https://railway.app/install.ps1 | iex"
    echo "  macOS/Linux: curl -fsSL https://railway.app/install.sh | sh"
    exit 1
fi

echo "✅ Railway CLI đã được cài đặt"

# Kiểm tra đăng nhập
if ! railway whoami &> /dev/null; then
    echo "❌ Chưa đăng nhập Railway!"
    echo "Đang mở trình duyệt để đăng nhập..."
    railway login
fi

echo "✅ Đã đăng nhập Railway"

# Kiểm tra project đã được link chưa
if ! railway status &> /dev/null; then
    echo "⚠️  Project chưa được link với Railway"
    echo "Đang khởi tạo project mới..."
    railway init
fi

echo "✅ Project đã được link"

# Hỏi có muốn set environment variables không
read -p "Bạn có muốn set environment variables? (y/n): " set_env

if [ "$set_env" = "y" ] || [ "$set_env" = "Y" ]; then
    if [ -f ".env" ]; then
        echo "📝 Đang import variables từ .env..."
        railway variables set --from-file .env
        echo "✅ Variables đã được set"
    else
        echo "⚠️  File .env không tồn tại"
        echo "Vui lòng set variables thủ công sau khi deploy"
    fi
fi

# Deploy
echo ""
echo "🚀 Đang deploy lên Railway..."
railway up

echo ""
echo "=========================================="
echo "✅ Deploy thành công!"
echo "=========================================="
echo ""
echo "📊 Xem logs:"
echo "   railway logs"
echo ""
echo "🌐 Mở service:"
echo "   railway open"
echo ""
echo "📝 Xem variables:"
echo "   railway variables"
echo ""
echo "🔗 Lấy domain:"
echo "   railway domain"
echo ""