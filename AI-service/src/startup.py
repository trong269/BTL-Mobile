from fastapi import FastAPI
from src.core.agents.factory import AgentFactory
from src.utils.logging import get_logger

logger = get_logger(__name__)


async def startup_handler() -> None:
    """
    Khởi tạo AgentFactory và inject vào app.state khi server start.
    """
    logger.info("Application startup: initializing AgentFactory...")
    try:
        AgentFactory._build_all_agents() 
        logger.info("AgentFactory initialized successfully on startup.")
    except Exception as e:
        logger.critical("Failed to initialize AgentFactory on startup: %s", e)
        raise
