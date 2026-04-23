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

## AI Smart Selection Architecture (Long-term Memory)
- **Feature Overview**: Users can highlight text while reading in the Android app to get instant AI-generated explanations (`explain`) or summaries (`summarize`).
- **Data Payload**: The Android client automatically extracts up to 300 characters of `context_before` and `context_after` the highlighted text. This ensures the AI accurately understands pronouns, cut-off sentences, and the surrounding plot without guessing.
- **API Design**: The API at `/api/ai/explain` and `/api/ai/summarize` utilizes a **One-Shot (1 Cục)** response mechanism instead of streaming. This ensures mobile UI stability, rapid rendering for small text snippets (1-1.5s latency), and reliable Markdown parsing.
- **Prompts (`src/core/prompts/*.md`)**: Prompts strictly enforce a "Mobile-First" output layer: Zero-Yapping (no conversational fillers), max 2-3 short bullet points, and an explicit prohibition of Markdown headings (`#`, `##`) to preserve formatting on mobile screens. We also enforce a "No-Spoiler" rule for literary context. 
- **Agent Initialization**: The repository utilizes `AgentFactory` in `AI-service/src/core/agents/factory.py` to lazy-load and cache the underlying LangGraph structures based on the `task_type`.
