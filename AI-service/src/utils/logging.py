import logging
import sys
from pathlib import Path


def get_logger(name: str) -> logging.Logger:
    """
    Trả về logger đã được cấu hình cho module tương ứng.

    Usage:
        from src.utils.logging import get_logger
        logger = get_logger(__name__)

    Args:
        name: Thường là __name__ của module gọi

    Returns:
        logging.Logger đã cấu hình handler console + file
    """
    logger = logging.getLogger(name)

    # Tránh add duplicate handlers nếu logger đã được tạo trước đó
    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG)

    formatter = logging.Formatter(
        fmt="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    # ─── Console handler ──────────────────────────────────────────────────────
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    # ─── File handler ─────────────────────────────────────────────────────────
    log_dir = Path(__file__).parent.parent.parent / "logs"
    log_dir.mkdir(exist_ok=True)
    file_handler = logging.FileHandler(log_dir / "app.log", encoding="utf-8")
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    return logger
