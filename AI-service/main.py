from fastapi import FastAPI
from contextlib import asynccontextmanager
from src.startup import startup_handler
from src.routers.ai_router import router as ai_router
from src.utils.logging import get_logger
logger = get_logger(__name__)


app = FastAPI(
    title="Book AI Service",
    description="AI Service cung cấp tính năng AI cho app đọc sách",
    version="1.0.0",
)

app.include_router(ai_router, prefix="/api/ai", tags=["AI"])

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
