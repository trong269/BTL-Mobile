# AI Service - Smart Selection 🧠✨

Microservice độc lập viết bằng **Python (FastAPI)** dùng để cung cấp sức mạnh Trí tuệ Nhân tạo cho ứng dụng đọc sách BookApp. Cốt lõi của service này là khả năng cung cấp tính năng **AI Smart Selection (Bôi đen thông minh)** giúp người đọc giải nghĩa thuật ngữ, hoặc tóm tắt đoạn văn theo ngữ cảnh của sách.

---

## 🚀 Các tính năng chính (Mới cập nhật)

- **One-Shot API & Low Latency**: Phản hồi trực tiếp thay vì streaming để đảm bảo UI/UX ổn định trên ứng dụng di động (Mobile-First).
- **Ngữ cảnh thông minh (Context-Aware)**: Tự động trích xuất các câu văn phía trước và phía sau đoạn người dùng bôi đen (tối đa 300 ký tự) cùng tên sách để suy luận chính xác ý nghĩa đại từ nhân xưng, hoặc các câu bị cắt dở.
- **Rich Markdown Output**: AI sẽ sinh ra nội dung được định dạng Markdown sinh động (in đậm, in nghiêng, trích dẫn, heading) thay vì chỉ gạch đầu dòng khô khan, giúp hiển thị đẹp mắt qua thư viện `Markwon` trên Android.
- **Agent Architecture**: Xây dựng dựa trên `LangGraph` và `LangChain`, cho phép cấu trúc luồng suy nghĩ của AI theo Graph tĩnh. Các Agent (`ExplainAgent`, `SummarizeAgent`) được quản lý bằng `AgentFactory` với cơ chế Lazy-loading và Caching.
- **Zero-Yapping / No-Spoilers**: Các Prompts được tùy biến để AI trực tiếp đi vào vấn đề, không dài dòng, không dùng mẫu câu người máy (vd: "Đoạn văn này nói về...") và đặc biệt không spoil tương lai cốt truyện.

---

## 🛠 Cấu trúc thư mục

```text
AI-service/
├── config/              # Chứa file cấu hình YAML, quản lý môi trường
├── src/                 # Source code chính
│   ├── core/
│   │   ├── agents/      # Định nghĩa ExplainAgent & SummarizeAgent (LangGraph)
│   │   ├── llm/         # Factory khởi tạo mô hình AI (Gemini/OpenAI)
│   │   └── prompts/     # Các prompt (summarize.md, explanation.md) chuẩn Markdown
│   ├── routers/         # Định nghĩa các Endpoints cho FastAPI (ai_router.py)
│   └── utils/           # Utilities (response_sanitizer.py, logging, tracing)
├── tests/               # Unit tests (sử dụng pytest)
├── main.py              # Entry-point của FastAPI
├── requirements.txt     # Danh sách thư viện Python
└── start.sh             # Script khởi động server
```

---

## 🌐 API Endpoints

### 1. `POST /api/ai/explain`
Dùng để giải thích một từ khóa, thuật ngữ hoặc một câu nói ẩn dụ.

**Request Body (`application/json`):**
```json
{
  "text": "vung tay múa chân",
  "book_name": "Đắc Nhân Tâm",
  "context_before": "Ông Lý tỏ ra vô cùng bực tức,",
  "context_after": "khiến mọi người xung quanh đều hoảng sợ."
}
```

### 2. `POST /api/ai/summarize`
Dùng để tóm tắt một đoạn văn bản dài mà người dùng bôi đen. Yêu cầu Input body giống hệt đường dẫn `explain`.

**Response (`application/json`):**
Cả hai đường dẫn đều chung 1 format kết quả trả về, trong đó nội dung `result` sẽ chứa mã Markdown.
```json
{
  "result": "### Phân tích ngữ cảnh\nÔng Lý đang **tức giận** và mất kiểm soát hành vi...",
  "task": "explain"
}
```

---

## 💻 Hướng dẫn chạy môi trường (Local Development)

Môi trường yêu cầu: **Conda** & **Python 3.10+**.

**1. Kích hoạt môi trường (Conda):**
```bash
conda activate btl-mobile
```

**2. Cài đặt thư viện:**
```bash
pip install -r requirements.txt
```

**3. Cấu hình biến môi trường (`.env`):**
Sao chép mẫu từ `.env.example` sang `.env` và điền Key AI (VD: Gemini API Key).
```bash
cp .env.example .env
```

**4. Khởi động Server:**
```bash
# Chạy ở chế độ auto-reload thông qua uvicorn trên cổng 8000
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Hoặc dùng file sh
bash start.sh
```

**5. Chạy Automated Tests:**
```bash
pytest tests/ -v
```

---

## 📝 Quy chuẩn Code (Code Style)
- Code Python giới hạn theo **PEP 8**, thò thụt bằng hệ thống 4 **spaces**.
- Import được quy chuẩn bằng Absolute Path (bắt đầu bằng `src.`).
- Mọi thay đổi về Agent Prompt phải giữ nguyên tinh thần **Mobile-First** và không được phá vỡ các định dạng Markdown đã cấu hình.
