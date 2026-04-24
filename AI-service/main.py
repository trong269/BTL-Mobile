from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.startup import startup_handler
from src.routers.ai_router import router as ai_router
from src.routers.config_router import router as config_router
from src.utils.logging import get_logger

logger = get_logger(__name__)


app = FastAPI(
    title="Book AI Service",
    description="AI Service cung cấp tính năng AI cho app đọc sách",
    version="1.0.0",
)

# CORS middleware for admin-panel and production
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",  # Vite dev server
        "http://localhost:3000",  # Alternative dev port
        "http://127.0.0.1:5173",
        "http://127.0.0.1:3000",
        "https://ai-book-app-three.vercel.app",  # Production Vercel app
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(ai_router, prefix="/api/ai", tags=["AI"])
app.include_router(config_router, prefix="/api/ai", tags=["Config"])


@app.on_event("startup")
async def startup_event():
    """Initialize agents on startup"""
    try:
        await startup_handler()
        logger.info("Startup completed successfully")
    except Exception as e:
        logger.error(f"Startup failed: {e}")
        raise


@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "Book AI Service"}
