#!/bin/bash

# Dừng script ngay lập tức nếu có bất kỳ lệnh nào bị lỗi
set -e

# Chuyển đến thư mục gốc của project (nơi chứa start.sh)
cd "$(dirname "$0")"

echo "=========================================="
echo "    🚀 Khởi động Book AI Service"
echo "=========================================="

# 1. Kiểm tra và tải môi trường ảo (nếu bạn dùng conda/venv)
# (Bỏ comment nếu bạn muốn tự động activate môi trường ảo)
# source .venv/bin/activate
# conda activate btl-mobile

# 2. Kiểm tra file .env
if [ ! -f ".env" ]; then
    echo "⚠️  Không tìm thấy file .env!"
    echo "Tự động copy từ .env.example sang .env..."
    cp .env.example .env
    echo "❌ Vui lòng mở file .env, điền các API keys cần thiết rồi chạy lại lệnh: ./start.sh"
    exit 1
fi

# 3. Đảm bảo thư mục logs tồn tại
mkdir -p logs

# 4. Đọc biến môi trường từ .env để lấy HOST và PORT
set -a
source .env
set +a

HOST=${APP_HOST:-0.0.0.0}
PORT=${APP_PORT:-8000}

# 5. Khởi động server
echo "✅ Đang khởi động FastAPI Uvicorn server..."
echo "🌐 API Server     : http://$HOST:$PORT"
echo "📚 Swagger UI Docs: http://$HOST:$PORT/docs"
echo "🛑 Cách thoát     : Nhấn Ctrl+C"
echo "------------------------------------------"
echo ""

# Chạy server với chế độ --reload (tự động restart khi sửa code)
uvicorn main:app --host "$HOST" --port "$PORT" --reload
