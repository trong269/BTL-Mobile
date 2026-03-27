from pathlib import Path
from langchain_core.messages import SystemMessage
from src.utils.logging import get_logger

logger = get_logger(__name__)

_PROMPTS_DIR = Path(__file__).parent


def _parse_system_text(filename: str) -> str:
    """
    Đọc file .md và trả về nội dung của section ## System.
    Text người dùng được truyền riêng thành HumanMessage trong node.
    """
    path = _PROMPTS_DIR / filename
    try:
        content = path.read_text(encoding="utf-8")
    except FileNotFoundError:
        logger.error("Prompt file not found: %s", path)
        raise

    system_lines: list[str] = []
    in_system = False

    for line in content.splitlines():
        stripped = line.strip()
        if stripped == "## System":
            in_system = True
            continue
        elif stripped.startswith("## ") or stripped.startswith("# "):
            in_system = False
            continue
        if in_system:
            system_lines.append(line)

    system_text = "\n".join(system_lines).strip()
    if not system_text:
        logger.warning("Prompt file '%s' has empty ## System section", filename)

    logger.debug("Loaded prompt from '%s' (%d chars)", filename, len(system_text))
    return system_text


class PromptFactory:
    """
    Factory load và phân phối SystemMessage từ các file .md.
    Text người dùng truyền riêng thành HumanMessage trong node.

    Map:
        'summarize' → summarize.md
        'explain'   → explanation.md
    """

    _PROMPT_FILES: dict[str, str] = {
        "summarize": "summarize.md",
        "explain":   "explanation.md",
    }
    
    @staticmethod
    def get(prompt_name: str) -> SystemMessage:
        if prompt_name not in PromptFactory._PROMPT_FILES:
            supported = ", ".join(f"'{k}'" for k in PromptFactory._PROMPT_FILES)
            raise ValueError(
                f"Prompt '{prompt_name}' không tồn tại. Chọn một trong: {supported}"
            )
        filename = PromptFactory._PROMPT_FILES[prompt_name]
        logger.info("Loading prompt: '%s' from %s", prompt_name, filename)
        system_text = _parse_system_text(filename)
        return SystemMessage(content=system_text)

    @staticmethod
    def get_summarize_prompt() -> SystemMessage:
        return PromptFactory.get("summarize")

    @staticmethod
    def get_explain_prompt() -> SystemMessage:
        return PromptFactory.get("explain")
