import uuid
from typing import Optional, Dict, Any
from langfuse import Langfuse
from langfuse.langchain import CallbackHandler
from src.utils.logging import get_logger
from src.utils.config import config_manager

logger = get_logger(__name__)


class LangfuseManager:
    """Singleton – khởi tạo Langfuse client và cấp handler cho từng request."""

    _instance: Optional["LangfuseManager"] = None
    _initialized: bool = False
    _client: Optional[Langfuse] = None
    _enabled: bool = False

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if self._initialized:
            return
        self._initialized = True
        self._cfg = config_manager.get_langfuse_config()
        self._client = None
        self._enabled = False
        self._initialize()

    def _initialize(self):
        if not self._cfg.enabled:
            logger.info("Langfuse tracing is disabled.")
            return
        try:
            self._client = Langfuse(
                secret_key=self._cfg.secret_key,
                public_key=self._cfg.public_key,
                host=self._cfg.host,
                timeout=10,
                flush_at=5,
                flush_interval=10,
            )
            self._enabled = True
            logger.info(f"Langfuse initialised → {self._cfg.host}")
        except Exception as e:
            logger.error(f"Langfuse init failed: {e}")

    def is_enabled(self) -> bool:
        return self._enabled

    def get_client(self):
        return self._client

    def get_handler(
        self, trace_name: Optional[str] = None, **kwargs
    ) -> Optional[CallbackHandler]:
        """Trả về CallbackHandler để truyền vào config['callbacks'] của LangGraph."""
        if not self._enabled:
            return None
        try:
            return CallbackHandler(
                public_key=self._cfg.public_key,
                trace_context={"trace_id": uuid.uuid4().hex},
            )
        except Exception as e:
            logger.warning(f"Langfuse get_handler failed: {e}")
            return None

    def flush(self):
        if self._client:
            self._client.flush()

    def shutdown(self):
        self.flush()


# Singleton instance
tracer = LangfuseManager()


