# Repository Guidelines

## Project Structure & Module Organization
This repository has four modules:
- `backend/`: Spring Boot API with `controller/`, `service/`, `repository/`, `model/`, and `dto/`.
- `BookApp/`: Android client; app code is in `app/src/main/java`, resources in `app/src/main/res`.
- `admin-panel/`: Vite + React + TypeScript dashboard under `src/`.
- `AI-service/`: FastAPI service with runtime code in `src/` and tests in `tests/`.

## Build, Test, and Development Commands
Run commands from each module directory.
```bash
cd backend && ./mvnw spring-boot:run      # start API on :8080
cd backend && ./mvnw test                 # run Spring tests
cd BookApp && ./gradlew assembleDebug     # build Android debug APK
cd BookApp && ./gradlew test              # run JVM unit tests
cd admin-panel && npm install && npm run dev   # start Vite on :3000
cd admin-panel && npm run build && npm run lint # production build + TS check
conda activate btl-mobile && cd AI-service && pip install -r requirements.txt
conda activate btl-mobile && cd AI-service && uvicorn main:app --reload --host 0.0.0.0 --port 8000
conda activate btl-mobile && cd AI-service && pytest   # run FastAPI/agent tests
```

## Coding Style & Naming Conventions
Follow existing module conventions instead of forcing one style across the repo.
- TypeScript/React: 2-space indentation, PascalCase for components/pages, camelCase for API helpers.
- Java/Kotlin: 4-space indentation, PascalCase classes, camelCase methods, package names under `com.bookapp`.
- Python: PEP 8, 4-space indentation, `snake_case` modules and functions.
Keep controller/service/repository boundaries intact. Name tests after the target class or behavior, for example `BookControllerTest` or `test_summarize_success`.

## Testing Guidelines
Add or update tests with each behavior change. Backend changes should extend Spring tests in `backend/src/test/java`. Android changes should use `src/test` for logic and `src/androidTest` for UI flows. AI service changes should add `pytest` cases in `AI-service/tests`, and any run or test command for `AI-service` should be executed after `conda activate btl-mobile`. The admin panel currently has no automated test suite; at minimum run `npm run lint` and document manual verification.

## Commit & Pull Request Guidelines
Recent history mixes short imperative subjects (`add filter`) with typed commits (`fix:`, `chore:`). Prefer `type: concise summary`, for example `fix: handle expired admin sessions`. Keep commits scoped to one module. PRs should state affected modules, config changes, linked issues, and include screenshots for `BookApp/` or `admin-panel/` UI changes.

## Security & Configuration Tips
Do not commit real secrets. Use `AI-service/.env.example` as the template for AI settings and prefer environment variables for MongoDB and server ports in `backend/`. Activate Conda environment `btl-mobile` before installing dependencies, running the API, or executing tests in `AI-service`. Before building the Android app, set `BookApp/app/build.gradle.kts` `BASE_URL` to the emulator or LAN host you are targeting.

## AI Reader Assistant Architecture (Long-term Memory)
- **Feature Overview**: Reader hiện có 2 lớp AI chính:
- `Smart Selection` cho đoạn bôi đen (`/api/ai/explain`, `/api/ai/summarize`, one-shot).
- `Chatbot-QA` trong chế độ đọc (`/api/ai/qa`, `/api/ai/qa/stream`) với session chat in-memory.
- **Context Strategy**:
- Với Smart Selection: Android gửi `text` + `context_before` + `context_after` (mỗi bên tối đa ~300 ký tự).
- Với QA: Android gửi `question`, `context_chunks`, `current_chapter_title`, `chat_history`.
- **Streaming Policy**:
- QA ưu tiên streaming qua `POST /api/ai/qa/stream` (SSE/chunked) để render token theo thời gian thực.
- One-shot `POST /api/ai/qa` vẫn giữ như fallback.
- **Chapter-based Suggestions**:
- Android gọi `POST /api/ai/suggestions` để sinh ~5 câu hỏi gợi ý cho chương đang đọc.
- Trigger khi vào/chuyển chương, chạy nền, cache theo chapter, có fallback câu generic khi timeout/lỗi.
- **Mobile UX Constraints**:
- BottomSheet chat phải keyboard-safe (IME không che ô nhập).
- Output ngắn gọn, dễ quét nhanh; ưu tiên Markdown đơn giản phù hợp Markwon.
- **Prompt Rules (`AI-service/src/core/prompts/*.md`)**:
- Mobile-First, Zero-Yapping, No-Spoiler, không bịa dữ kiện ngoài ngữ liệu đã cung cấp.
- **Agent Initialization**:
- `AgentFactory` lazy-load + cache theo `task_type` (`summarize`, `explain`, `qa`, `suggestions`).
