import logging
import sys
from pathlib import Path


class ColoredFormatter(logging.Formatter):
    """
    Custom formatter to add ANSI colors for terminal output.
    """
    COLORS = {
        'DEBUG':    '\033[0;36m',  # Cyan
        'INFO':     '\033[0;32m',  # Green
        'WARNING':  '\033[0;33m',  # Yellow
        'ERROR':    '\033[0;31m',  # Red
        'CRITICAL': '\033[1;31m',  # Bold Red
    }
    RESET = '\033[0m'

    def format(self, record):
        original_levelname = record.levelname
        color = self.COLORS.get(original_levelname, self.RESET)
        record.levelname = f"{color}{original_levelname:8s}{self.RESET}"
        
        # Color specific parts of the message if needed, e.g., thread/process names
        # Here we just return the standard format with the colored levelname
        result = super().format(record)
        
        # Restore the original levelname so subsequent handlers (e.g. file) aren't affected
        record.levelname = original_levelname
        return result


def get_logger(name: str) -> logging.Logger:
    """
    Trả về logger đã được cấu hình cho module tương ứng.
    """
    logger = logging.getLogger(name)

    # Tránh add duplicate handlers nếu logger đã được tạo trước đó
    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG)

    # ─── Formatter (Standard) ──────────────────────────────────────────────────
    log_format = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
    date_format = "%Y-%m-%d %H:%M:%S"
    
    standard_formatter = logging.Formatter(fmt=log_format, datefmt=date_format)
    colored_formatter = ColoredFormatter(fmt=log_format, datefmt=date_format)

    # ─── Console handler (With Colors) ─────────────────────────────────────────
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(colored_formatter)
    logger.addHandler(console_handler)

    # ─── File handler (Without Colors) ─────────────────────────────────────────
    log_dir = Path(__file__).parent.parent.parent / "logs"
    log_dir.mkdir(exist_ok=True)
    file_handler = logging.FileHandler(log_dir / "app.log", encoding="utf-8")
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(standard_formatter)
    logger.addHandler(file_handler)

    return logger
