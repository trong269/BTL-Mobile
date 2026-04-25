import os
import re
from datetime import datetime, timedelta
from typing import Optional
from pathlib import Path
from fastapi import APIRouter, HTTPException, Query
from src.routers.config_models import (
    LLMConfig,
    LLMConfigUpdate,
    APIKeysUpdate,
    AgentInfo,
    AILog,
    AIStats,
    RequestsByAgent,
    HourlyRequest,
    GeminiConfig,
    OpenAIConfig,
    LocalConfig,
    OllamaConfig,
    FileLogEntry,
)
from src.utils.config import config_manager
from src.core.agents.factory import AgentFactory
from src.utils.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


# ─── In-memory storage for demo (replace with database in production) ─────────
_agent_stats = {
    "summarize": {"total_requests": 0, "total_time": 0.0, "last_used": None},
    "explain": {"total_requests": 0, "total_time": 0.0, "last_used": None},
    "qa": {"total_requests": 0, "total_time": 0.0, "last_used": None},
    "suggestions": {"total_requests": 0, "total_time": 0.0, "last_used": None},
}

_logs: list[dict] = []


def _track_request(agent: str, duration_ms: float):
    """Track agent request for statistics"""
    if agent in _agent_stats:
        _agent_stats[agent]["total_requests"] += 1
        _agent_stats[agent]["total_time"] += duration_ms
        _agent_stats[agent]["last_used"] = datetime.utcnow().isoformat() + "Z"


def _add_log(agent: str, level: str, message: str, duration_ms: Optional[float] = None):
    """Add log entry"""
    log = {
        "id": f"{len(_logs) + 1}",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "agent": agent,
        "level": level,
        "message": message,
        "duration_ms": duration_ms,
    }
    _logs.append(log)
    # Keep only last 1000 logs
    if len(_logs) > 1000:
        _logs.pop(0)


# ─── LLM Configuration Endpoints ──────────────────────────────────────────────

@router.get(
    "/config/llm",
    response_model=LLMConfig,
    summary="Lấy cấu hình LLM hiện tại",
    description="Trả về cấu hình LLM provider, temperature, top_p, max_tokens và thông tin các provider",
)
async def get_llm_config() -> LLMConfig:
    try:
        # Build provider-specific configs
        gemini_config = GeminiConfig(
            model=config_manager.gemini_model,
            api_key_configured=bool(config_manager.gemini_api_key),
        )

        openai_config = OpenAIConfig(
            model=config_manager.openai_model,
            api_key_configured=bool(config_manager.openai_api_key),
        )

        local_config = LocalConfig(
            base_url=config_manager.local_base_url,
            model=config_manager.local_model,
            api_key_configured=bool(config_manager.local_api_key),
        )

        ollama_config = OllamaConfig(
            model=config_manager.ollama_model,
            base_url=config_manager.ollama_base_url,
        )

        config = LLMConfig(
            provider=config_manager.llm_provider,
            temperature=config_manager.llm_temperature,
            top_p=config_manager.llm_top_p,
            max_tokens=config_manager.llm_max_tokens,
            gemini=gemini_config,
            openai=openai_config,
            local=local_config,
            ollama=ollama_config,
        )

        logger.info("LLM config retrieved successfully")
        return config
    except Exception as e:
        logger.error(f"Failed to get LLM config: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể lấy cấu hình LLM: {e}")


@router.put(
    "/config/llm",
    response_model=LLMConfig,
    summary="Cập nhật cấu hình LLM",
    description="Cập nhật cấu hình LLM (lưu vào environment variables - cần restart service để áp dụng)",
)
async def update_llm_config(update: LLMConfigUpdate) -> LLMConfig:
    try:
        # Note: This updates the config_manager in-memory only
        # For persistent changes, you need to update .env file or use a database

        if update.provider is not None:
            # Validate provider
            if update.provider not in ["gemini", "openai", "local", "ollama"]:
                raise HTTPException(status_code=400, detail="Provider không hợp lệ")
            os.environ["LLM_PROVIDER"] = update.provider
            logger.info(f"Updated LLM provider to: {update.provider}")

        if update.temperature is not None:
            os.environ["LLM_TEMPERATURE"] = str(update.temperature)
            logger.info(f"Updated temperature to: {update.temperature}")

        if update.top_p is not None:
            os.environ["LLM_TOP_P"] = str(update.top_p)
            logger.info(f"Updated top_p to: {update.top_p}")

        if update.max_tokens is not None:
            os.environ["LLM_MAX_TOKENS"] = str(update.max_tokens)
            logger.info(f"Updated max_tokens to: {update.max_tokens}")

        # Provider-specific updates
        if update.gemini_model is not None:
            os.environ["GEMINI_MODEL"] = update.gemini_model
            logger.info(f"Updated Gemini model to: {update.gemini_model}")

        if update.openai_model is not None:
            os.environ["OPENAI_MODEL"] = update.openai_model
            logger.info(f"Updated OpenAI model to: {update.openai_model}")

        if update.local_base_url is not None:
            os.environ["LOCAL_BASE_URL"] = update.local_base_url
            logger.info(f"Updated local base_url to: {update.local_base_url}")

        if update.local_model is not None:
            os.environ["LOCAL_MODEL"] = update.local_model
            logger.info(f"Updated local model to: {update.local_model}")

        if update.ollama_model is not None:
            os.environ["OLLAMA_MODEL"] = update.ollama_model
            logger.info(f"Updated Ollama model to: {update.ollama_model}")

        if update.ollama_base_url is not None:
            os.environ["OLLAMA_BASE_URL"] = update.ollama_base_url
            logger.info(f"Updated Ollama base_url to: {update.ollama_base_url}")

        # Reload config
        config_manager._load()

        _add_log("system", "info", "Cấu hình LLM đã được cập nhật")

        # Return updated config
        return await get_llm_config()

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to update LLM config: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể cập nhật cấu hình: {e}")


@router.put(
    "/config/keys",
    response_model=dict,
    summary="Cập nhật API keys",
    description="Cập nhật API keys cho các providers (Gemini, OpenAI, Local) và lưu vào .env file",
)
async def update_api_keys(update: APIKeysUpdate) -> dict:
    try:
        updated_keys = []
        env_file = Path(__file__).parent.parent.parent / ".env"

        # Read current .env file
        if not env_file.exists():
            raise HTTPException(status_code=500, detail=".env file not found")

        env_content = env_file.read_text(encoding='utf-8')
        lines = env_content.split('\n')

        # Update environment variables and .env file
        if update.google_api_key is not None:
            os.environ["GOOGLE_API_KEY"] = update.google_api_key
            updated_keys.append("GOOGLE_API_KEY")
            logger.info("Updated Google API key")

            # Update in .env file
            updated = False
            for i, line in enumerate(lines):
                if line.startswith('GOOGLE_API_KEY='):
                    lines[i] = f'GOOGLE_API_KEY={update.google_api_key}'
                    updated = True
                    break
            if not updated:
                lines.append(f'GOOGLE_API_KEY={update.google_api_key}')

        if update.openai_api_key is not None:
            os.environ["OPENAI_API_KEY"] = update.openai_api_key
            updated_keys.append("OPENAI_API_KEY")
            logger.info("Updated OpenAI API key")

            # Update in .env file
            updated = False
            for i, line in enumerate(lines):
                if line.startswith('OPENAI_API_KEY='):
                    lines[i] = f'OPENAI_API_KEY={update.openai_api_key}'
                    updated = True
                    break
            if not updated:
                lines.append(f'OPENAI_API_KEY={update.openai_api_key}')

        if update.local_api_key is not None:
            os.environ["LOCAL_API_KEY"] = update.local_api_key
            updated_keys.append("LOCAL_API_KEY")
            logger.info("Updated Local API key")

            # Update in .env file
            updated = False
            for i, line in enumerate(lines):
                if line.startswith('LOCAL_API_KEY='):
                    lines[i] = f'LOCAL_API_KEY={update.local_api_key}'
                    updated = True
                    break
            if not updated:
                lines.append(f'LOCAL_API_KEY={update.local_api_key}')

        if not updated_keys:
            raise HTTPException(status_code=400, detail="No API keys provided")

        # Write back to .env file
        env_file.write_text('\n'.join(lines), encoding='utf-8')
        logger.info(f"Updated .env file with keys: {', '.join(updated_keys)}")

        # Reload config
        config_manager._load()

        _add_log("system", "info", f"API keys updated and saved to .env: {', '.join(updated_keys)}")

        return {
            "success": True,
            "updated_keys": updated_keys,
            "message": "API keys đã được cập nhật và lưu vào .env file"
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to update API keys: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể cập nhật API keys: {e}")


# ─── Agent Management Endpoints ───────────────────────────────────────────────

@router.get(
    "/agents",
    response_model=list[AgentInfo],
    summary="Lấy danh sách agents và trạng thái",
    description="Trả về thông tin tất cả agents: status, metrics, last_used",
)
async def get_agents() -> list[AgentInfo]:
    try:
        agents = []
        agent_names = {
            "summarize": "Tóm tắt",
            "explain": "Giải thích",
            "qa": "Hỏi đáp",
            "suggestions": "Gợi ý câu hỏi",
        }

        for name, display_name in agent_names.items():
            stats = _agent_stats.get(name, {})
            total_requests = stats.get("total_requests", 0)
            total_time = stats.get("total_time", 0.0)

            avg_response_time = 0.0
            if total_requests > 0:
                avg_response_time = total_time / total_requests

            # Check if agent is in cache (initialized)
            status = "active" if name in AgentFactory._cache else "inactive"

            agent_info = AgentInfo(
                name=name,
                display_name=display_name,
                enabled=True,
                status=status,
                last_used=stats.get("last_used"),
                total_requests=total_requests,
                avg_response_time=round(avg_response_time, 2),
            )
            agents.append(agent_info)

        logger.info("Agents info retrieved successfully")
        return agents

    except Exception as e:
        logger.error(f"Failed to get agents info: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể lấy thông tin agents: {e}")


# ─── Logging Endpoints ────────────────────────────────────────────────────────

@router.get(
    "/logs",
    response_model=list[AILog],
    summary="Lấy logs của AI service",
    description="Trả về danh sách logs với filter theo agent, level, limit",
)
async def get_logs(
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
    agent: Optional[str] = Query(None, description="Filter by agent name"),
    level: Optional[str] = Query(None, description="Filter by log level"),
) -> list[AILog]:
    try:
        filtered_logs = _logs.copy()

        # Filter by agent
        if agent and agent != "all":
            filtered_logs = [log for log in filtered_logs if log["agent"] == agent]

        # Filter by level
        if level and level != "all":
            filtered_logs = [log for log in filtered_logs if log["level"] == level]

        # Sort by timestamp descending (newest first)
        filtered_logs.sort(key=lambda x: x["timestamp"], reverse=True)

        # Apply pagination
        paginated_logs = filtered_logs[offset : offset + limit]

        # Convert to AILog models
        result = [AILog(**log) for log in paginated_logs]

        logger.info(f"Retrieved {len(result)} logs")
        return result

    except Exception as e:
        logger.error(f"Failed to get logs: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể lấy logs: {e}")


# ─── Statistics Endpoints ─────────────────────────────────────────────────────

@router.get(
    "/stats",
    response_model=AIStats,
    summary="Lấy thống kê sử dụng AI",
    description="Trả về thống kê: total requests, avg response time, error rate, requests by agent",
)
async def get_stats() -> AIStats:
    try:
        # Calculate total requests
        total_requests = sum(stats["total_requests"] for stats in _agent_stats.values())

        # Calculate avg response time
        total_time = sum(stats["total_time"] for stats in _agent_stats.values())
        avg_response_time = 0.0
        if total_requests > 0:
            avg_response_time = total_time / total_requests

        # Calculate error rate
        error_logs = [log for log in _logs if log["level"] == "error"]
        error_rate = 0.0
        if total_requests > 0:
            error_rate = (len(error_logs) / total_requests) * 100

        # Requests by agent
        requests_by_agent = RequestsByAgent(
            summarize=_agent_stats["summarize"]["total_requests"],
            explain=_agent_stats["explain"]["total_requests"],
            qa=_agent_stats["qa"]["total_requests"],
            suggestions=_agent_stats["suggestions"]["total_requests"],
        )

        # Generate hourly data (last 24 hours)
        now = datetime.utcnow()
        requests_by_hour = []
        for i in range(24):
            hour_time = now - timedelta(hours=23 - i)
            hour_str = hour_time.strftime("%H:00")
            # Mock data - in production, query from database
            count = 0
            requests_by_hour.append(HourlyRequest(hour=hour_str, count=count))

        stats = AIStats(
            total_requests_today=total_requests,
            total_requests_week=total_requests,
            avg_response_time=round(avg_response_time, 2),
            error_rate=round(error_rate, 2),
            requests_by_agent=requests_by_agent,
            requests_by_hour=requests_by_hour,
        )

        logger.info("Stats retrieved successfully")
        return stats

    except Exception as e:
        logger.error(f"Failed to get stats: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể lấy thống kê: {e}")


# ─── File Logs Endpoint ───────────────────────────────────────────────────────

@router.delete(
    "/logs",
    response_model=dict,
    summary="Xóa logs",
    description="Xóa tất cả logs trong memory và file logs",
)
async def delete_logs() -> dict:
    try:
        # Clear in-memory logs
        _logs.clear()

        # Clear log files
        log_dir = Path(__file__).parent.parent.parent / "logs"
        log_files = list(log_dir.glob("*.log"))

        deleted_files = []
        for log_file in log_files:
            try:
                log_file.write_text("", encoding='utf-8')
                deleted_files.append(log_file.name)
            except Exception as e:
                logger.error(f"Failed to clear log file {log_file.name}: {e}")

        logger.info(f"Cleared {len(deleted_files)} log files and in-memory logs")

        return {
            "success": True,
            "message": "Đã xóa tất cả logs",
            "cleared_files": deleted_files,
            "cleared_memory_logs": True
        }

    except Exception as e:
        logger.error(f"Failed to delete logs: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể xóa logs: {e}")


@router.get(
    "/logs/file",
    response_model=list[FileLogEntry],
    summary="Lấy logs từ file",
    description="Đọc logs trực tiếp từ file log của AI service",
)
async def get_file_logs(
    limit: int = Query(100, ge=1, le=1000),
    level: Optional[str] = Query(None, description="Filter by log level"),
) -> list[FileLogEntry]:
    try:
        # Find log file
        log_dir = Path(__file__).parent.parent.parent / "logs"
        log_files = list(log_dir.glob("*.log"))

        if not log_files:
            logger.warning("No log files found")
            return []

        # Get most recent log file
        log_file = max(log_files, key=lambda p: p.stat().st_mtime)

        # Read last N lines
        logs = []
        try:
            with open(log_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            # Parse log lines (reverse to get newest first)
            for i, line in enumerate(reversed(lines[-limit*3:])):
                line = line.strip()
                if not line:
                    continue

                # Try format 1: 2026-04-24 16:42:06 | INFO     | src.utils.config | Config loaded...
                match = re.match(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s*\|\s*(\w+)\s*\|\s*([^\|]+)\s*\|\s*(.+)', line)

                if match:
                    timestamp, log_level, logger_name, message = match.groups()

                    # Skip INFO logs from config_router
                    if log_level.strip().upper() == "INFO" and "config_router" in logger_name.strip():
                        continue
                else:
                    # Try format 2: INFO:     127.0.0.1:50068 - "GET /api/ai/logs/file..." 200 OK
                    match2 = re.match(r'(\w+):\s+(.+)', line)
                    if match2:
                        log_level, message = match2.groups()
                        timestamp = "N/A"
                        logger_name = "uvicorn"
                    else:
                        continue

                # Filter by level if specified
                if level and level.upper() != "ALL" and log_level.strip().upper() != level.upper():
                    continue

                logs.append(FileLogEntry(
                    timestamp=timestamp.strip() if timestamp != "N/A" else "N/A",
                    level=log_level.strip(),
                    logger=logger_name.strip() if match else "uvicorn",
                    message=message.strip(),
                    line_number=len(lines) - i
                ))

                if len(logs) >= limit:
                    break

        except Exception as e:
            logger.error(f"Error reading log file: {e}")
            return []

        logger.info(f"Retrieved {len(logs)} file logs")
        return logs

    except Exception as e:
        logger.error(f"Failed to get file logs: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể lấy file logs: {e}")


# ─── Helper function to be called from ai_router ──────────────────────────────

def track_agent_request(agent: str, duration_ms: float, success: bool = True):
    """Call this from ai_router after each request"""
    _track_request(agent, duration_ms)
    level = "info" if success else "error"
    message = f"{agent.capitalize()} request {'successful' if success else 'failed'}"
    _add_log(agent, level, message, duration_ms)