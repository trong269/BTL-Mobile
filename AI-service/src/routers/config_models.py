from pydantic import BaseModel, Field
from typing import Optional, Literal


# ─── LLM Configuration Models ─────────────────────────────────────────────────

class GeminiConfig(BaseModel):
    model: str = "gemini-2.0-flash"
    api_key_configured: bool = False


class OpenAIConfig(BaseModel):
    model: str = "gpt-4o-mini"
    api_key_configured: bool = False


class LocalConfig(BaseModel):
    base_url: str = "http://localhost:1234/v1"
    model: str = "local-model"
    api_key_configured: bool = False


class OllamaConfig(BaseModel):
    model: str = "llama3.2"
    base_url: str = "http://localhost:11434"


class LLMConfig(BaseModel):
    provider: Literal["gemini", "openai", "local", "ollama"]
    temperature: float = Field(ge=0.0, le=1.0, default=0.7)
    top_p: float = Field(ge=0.0, le=1.0, default=0.9)
    max_tokens: int = Field(ge=1, le=8192, default=2048)

    gemini: Optional[GeminiConfig] = None
    openai: Optional[OpenAIConfig] = None
    local: Optional[LocalConfig] = None
    ollama: Optional[OllamaConfig] = None


class LLMConfigUpdate(BaseModel):
    provider: Optional[Literal["gemini", "openai", "local", "ollama"]] = None
    temperature: Optional[float] = Field(None, ge=0.0, le=1.0)
    top_p: Optional[float] = Field(None, ge=0.0, le=1.0)
    max_tokens: Optional[int] = Field(None, ge=1, le=8192)

    # Provider-specific updates
    gemini_model: Optional[str] = None
    openai_model: Optional[str] = None
    local_base_url: Optional[str] = None
    local_model: Optional[str] = None
    ollama_model: Optional[str] = None
    ollama_base_url: Optional[str] = None


class APIKeysUpdate(BaseModel):
    """Update API keys for providers"""
    google_api_key: Optional[str] = None
    openai_api_key: Optional[str] = None
    local_api_key: Optional[str] = None


# ─── Agent Models ─────────────────────────────────────────────────────────────

class AgentInfo(BaseModel):
    name: Literal["summarize", "explain", "qa", "suggestions"]
    display_name: str
    enabled: bool = True
    status: Literal["active", "inactive", "error"] = "active"
    last_used: Optional[str] = None
    total_requests: int = 0
    avg_response_time: float = 0.0  # milliseconds


# ─── Logging Models ───────────────────────────────────────────────────────────

class AILog(BaseModel):
    id: str
    timestamp: str
    agent: str
    level: Literal["info", "warning", "error"]
    message: str
    duration_ms: Optional[float] = None
    user_id: Optional[str] = None
    book_id: Optional[str] = None


class FileLogEntry(BaseModel):
    """Log entry from log file"""
    timestamp: str
    level: str
    logger: str
    message: str
    line_number: int


# ─── Statistics Models ────────────────────────────────────────────────────────

class RequestsByAgent(BaseModel):
    summarize: int = 0
    explain: int = 0
    qa: int = 0
    suggestions: int = 0


class HourlyRequest(BaseModel):
    hour: str
    count: int


class AIStats(BaseModel):
    total_requests_today: int = 0
    total_requests_week: int = 0
    avg_response_time: float = 0.0
    error_rate: float = 0.0
    requests_by_agent: RequestsByAgent
    requests_by_hour: list[HourlyRequest] = Field(default_factory=list)