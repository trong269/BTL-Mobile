# AI Service - Reader AI Platform

Microservice độc lập viết bằng **Python (FastAPI)** để cung cấp AI cho BookApp Reader.  
Service hiện hỗ trợ cả **Smart Selection one-shot** và **Chatbot-QA streaming** trong chế độ đọc sách.

---

## 🚀 Tính năng chính

- Smart Selection cho Reader: `explain` và `summarize` theo ngữ cảnh (before/after).
- Chatbot-QA theo nội dung sách: hỗ trợ one-shot (`/qa`) và stream token (`/qa/stream`).
- Sinh câu hỏi gợi ý theo chương (`/suggestions`) để tăng tương tác hỏi đáp.
- Prompt chuẩn Mobile-First + Zero-Yapping + Anti-Spoiler.
- Agent-based architecture với `LangGraph` + `AgentFactory` (lazy init + cache).
- Output Markdown tối ưu cho Android (`Markwon`).

---

## 🧩 Kiến trúc tổng quan

- `ExplainAgent` / `SummarizeAgent`: xử lý bôi đen văn bản (one-shot).
- `QAAgent`: trả lời câu hỏi trong reader, hỗ trợ cả `arun` và `astream`.
- `SuggestionsAgent`: sinh 5 câu hỏi gợi ý theo chương hiện tại.
- `AgentFactory`: phân phối + cache agent theo `task_type`.
- `ai_router.py`: điều phối endpoint, validation, fallback, chuẩn hóa response.

---

## 🛠 Cấu trúc thư mục

```text
AI-service/
├── config/
├── src/
│   ├── core/
│   │   ├── agents/       # explain, summarize, qa, suggestions
│   │   ├── llm/          # LLMFactory
│   │   └── prompts/      # explanation.md, summarize.md, qa.md, suggestions.md
│   ├── routers/          # ai_router.py
│   └── utils/            # logging, tracing, response_sanitizer
├── tests/
├── main.py
├── requirements.txt
└── start.sh
```

---

## 🌐 API Endpoints

### 1) `POST /api/ai/explain`
Giải thích đoạn chọn theo ngữ cảnh.

### 2) `POST /api/ai/summarize`
Tóm tắt đoạn chọn theo ngữ cảnh.

### 3) `POST /api/ai/qa`
Chatbot-QA one-shot.

Ví dụ request:
```json
{
  "question": "Nhân vật này đang muốn gì?",
  "book_name": "Lão Hạc",
  "current_chapter_title": "Chương 3",
  "context_chunks": ["..."],
  "chat_history": [{"role":"user","content":"..."}]
}
```

### 4) `POST /api/ai/qa/stream`
Stream câu trả lời QA theo SSE/chunked (`text/event-stream`).

Format event:
```text
data: {"token":"..."}

data: {"done": true}
```

### 5) `POST /api/ai/suggestions`
Sinh câu hỏi gợi ý theo chương.

Ví dụ response:
```json
{
  "task": "suggestions",
  "questions": [
    "Đoạn này tập trung vào mâu thuẫn nào?",
    "Nhân vật chính đang theo đuổi điều gì?",
    "Chi tiết nào báo hiệu thay đổi sắp tới?",
    "Tâm trạng nhân vật biến chuyển ra sao?",
    "Nếu tóm nhanh, 3 ý quan trọng là gì?"
  ]
}
```

---

## 💻 Local Development

Yêu cầu: **Conda** + **Python 3.10+**.

```bash
conda activate btl-mobile
cd AI-service
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Chạy test:
```bash
conda run -n btl-mobile env PYTHONPATH=. pytest -q
```

---

## 🧪 Test Coverage Notes

- `tests/test_api.py`: health check, explain/summarize/qa, qa stream, suggestions.
- `tests/test_agents.py`: node/agent build cho explain/summarize/qa/suggestions.

---

## 📝 Coding Notes

- Python theo PEP 8, indentation 4 spaces.
- Import ưu tiên absolute path `src.*`.
- Prompt và sanitizer phải giữ tinh thần mobile-first, anti-spoiler, rõ fallback.
