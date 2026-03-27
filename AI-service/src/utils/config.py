import os
import re
from pathlib import Path
from typing import Any

import yaml
from dotenv import load_dotenv
from src.utils.logging import get_logger

logger = get_logger(__name__)

# Load .env trước để os.environ có đầy đủ giá trị
load_dotenv()

_CONFIG_PATH = Path(__file__).parent.parent.parent / "config" / "app.yaml"


def _substitute_env_vars(text: str) -> str:
    """
    Thay thế ${VAR:-default} và ${VAR} trong chuỗi bằng giá trị từ os.environ.

    Cú pháp hỗ trợ:
        ${VAR}          → giá trị của VAR, chuỗi rỗng nếu không tồn tại
        ${VAR:-default} → giá trị của VAR, hoặc 'default' nếu VAR không tồn tại / rỗng
    """
    def _replace(match: re.Match) -> str:
        var_name: str = match.group(1)
        default: str | None = match.group(2)
        value = os.environ.get(var_name)
        if value:
            return value
        if default is not None:
            return default
        return ""

    # Pattern: ${VAR:-default} hoặc ${VAR}
    return re.sub(r"\$\{([^}:]+)(?::-(.*?))?\}", _replace, text)


class ConfigManager:
    """
    Singleton quản lý toàn bộ cấu hình ứng dụng.

    Flow:
        1. load_dotenv() → nạp .env vào os.environ
        2. Đọc app.yaml dạng string
        3. os.path.expandvars() → thay ${VAR} bằng giá trị từ os.environ
        4. yaml.safe_load() → parse thành dict
    """

    _instance: "ConfigManager | None" = None
    _data: dict

    def __new__(cls) -> "ConfigManager":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._load()
        return cls._instance

    def _load(self) -> None:
        try:
            raw = _CONFIG_PATH.read_text(encoding="utf-8")
            substituted = _substitute_env_vars(raw)
            self._data = yaml.safe_load(substituted) or {}
            logger.info("Config loaded successfully from %s", _CONFIG_PATH)
        except FileNotFoundError:
            logger.error("Config file not found: %s", _CONFIG_PATH)
            raise
        except yaml.YAMLError as e:
            logger.error("Failed to parse app.yaml: %s", e)
            raise

    def get(self, *keys: str, default: Any = None) -> Any:
        node = self._data
        for key in keys:
            if not isinstance(node, dict):
                return default
            node = node.get(key, default)
        return node

    # ─── App ──────────────────────────────────────────────────────────────────

    @property
    def app_host(self) -> str:
        return str(self.get("app", "host", default="0.0.0.0"))

    @property
    def app_port(self) -> int:
        return int(self.get("app", "port", default=8000))

    # ─── LLM ──────────────────────────────────────────────────────────────────

    @property
    def llm_provider(self) -> str:
        return str(self.get("llm", "provider", default="gemini")).lower()

    @property
    def llm_temperature(self) -> float:
        return float(self.get("llm", "temperature", default=0.7))

    @property
    def llm_top_p(self) -> float:
        return float(self.get("llm", "top_p", default=0.9))

    @property
    def llm_max_tokens(self) -> int:
        return int(self.get("llm", "max_tokens", default=2048))

    # Gemini
    @property
    def gemini_api_key(self) -> str:
        return str(self.get("llm", "gemini", "api_key", default=""))

    @property
    def gemini_model(self) -> str:
        return str(self.get("llm", "gemini", "model", default="gemini-2.0-flash"))

    # OpenAI
    @property
    def openai_api_key(self) -> str:
        return str(self.get("llm", "openai", "api_key", default=""))

    @property
    def openai_model(self) -> str:
        return str(self.get("llm", "openai", "model", default="gpt-4o-mini"))

    # Local (OpenAI-compatible server: LM Studio, vLLM, llama.cpp...)
    @property
    def local_base_url(self) -> str:
        return str(self.get("llm", "local", "base_url", default="http://localhost:1234/v1"))

    @property
    def local_model(self) -> str:
        return str(self.get("llm", "local", "model", default="local-model"))

    @property
    def local_api_key(self) -> str:
        return str(self.get("llm", "local", "api_key", default=""))

    # Ollama
    @property
    def ollama_model(self) -> str:
        return str(self.get("llm", "ollama", "model", default="llama3.2"))

    @property
    def ollama_base_url(self) -> str:
        return str(self.get("llm", "ollama", "base_url", default="http://localhost:11434"))


# Singleton instance — import và dùng trực tiếp ở bất kỳ đâu
config_manager = ConfigManager()
