# 📚 Book AI Service

Dịch vụ xử lý trí tuệ nhân tạo (AI) cho ứng dụng quản lý sách, được xây dựng bằng **FastAPI**, **LangChain**, và **LangGraph**.

## 🚀 Tính năng chính

- **Summarize Agent**: Tóm tắt nội dung sách thông minh.
- **Explain Agent**: Giải thích các khái niệm, thuật ngữ trong sách.
- **Analyze Image Agent**: Phân tích hình ảnh bìa sách hoặc nội dung trang sách.
- **Tracing & Monitoring**: Tích hợp **Langfuse** để theo dõi hiệu năng và debug Agent.
- **Multi-LLM Support**: Hỗ trợ Google Gemini, OpenAI, và các mô hình chạy Local (Ollama, vLLM).

---

## 🛠 Yêu cầu hệ thống

- Python 3.10+
- Conda (khuyến nghị) hoặc venv
- API Keys: Google API Key (Gemini) hoặc OpenAI API Key.

---

## 🏗 Cấu trúc dự án

```text
AI-service/
├── config/             # Cấu hình app (app.yaml)
├── src/
│   ├── core/
│   │   ├── agents/     # Định nghĩa logic của các AI Agent (LangGraph)
│   │   ├── llm/        # Factory khởi tạo các mô hình ngôn ngữ
│   │   └── prompts/    # Quản lý Prompt từ các file .md
│   ├── utils/          # Logging, Tracing, Config Manager
├── main.py             # Điểm khởi đầu của ứng dụng FastAPI
├── .env.example        # File mẫu lưu biến môi trường
├── requirements.txt    # Danh sách thư viện cần thiết
└── start.sh            # Script khởi động nhanh
```

---

## 🚦 Hướng dẫn cài đặt và chạy

### 1. Tạo môi trường ảo
```bash
conda create -n btl-mobile python=3.10
conda activate btl-mobile
```

### 2. Cài đặt thư viện
```bash
pip install -r requirements.txt
```

### 3. Cấu hình biến môi trường
Copy file mẫu và điền thông tin API Key của bạn:
```bash
cp .env.example .env
```
*Lưu ý: Mở file `.env` và cập nhật ít nhất một API Key (ví dụ `GOOGLE_API_KEY`).*

### 4. Khởi động Service
Sử dụng script `start.sh` để khởi động nhanh (tự động nhận diện host/port từ `.env`):
```bash
chmod +x start.sh
./start.sh
```

Hoặc chạy thủ công bằng uvicorn:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 📖 Tài liệu API (Swagger UI)

Khi Service đã chạy, bạn có thể truy cập tài liệu API tự động tại:
- **Swagger UI**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **ReDoc**: [http://localhost:8000/redoc](http://localhost:8000/redoc)

---

## 🔍 Giám sát (Langfuse)

Nếu bạn muốn theo dõi các bước suy nghĩ của Agent:
1. Đăng ký tài khoản tại [cloud.langfuse.com](https://cloud.langfuse.com).
2. Lấy Public Key, Secret Key và điền vào `.env`.
3. Bật tracing trong `config/app.yaml`:
   ```yaml
   tracing:
     langfuse:
       enabled: true
   ```

---

## 🛡 Bảo trì và Phát triển

- **Logs**: Kiểm tra thư mục `logs/app.log` để xem thông tin chi tiết quá trình xử lý.
- **Prompts**: Thay đổi logic của Agent trong thư mục `src/core/prompts/*.md`.
