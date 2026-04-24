from pathlib import Path
from langchain_core.messages import SystemMessage
from src.utils.logging import get_logger

logger = get_logger(__name__)

_PROMPTS_DIR = Path(__file__).parent


def _load_prompt_text(filename: str) -> str:
    """
    Đọc file .md và trả về tòan bộ nội dung làm prompt.
    """
    path = _PROMPTS_DIR / filename
    try:
        content = path.read_text(encoding="utf-8").strip()
        logger.debug("Loaded prompt from '%s' (%d chars)", filename, len(content))
        return content
    except FileNotFoundError:
        logger.error("Prompt file not found: %s", path)
        raise


class PromptFactory:
    """
    Factory load và phân phối SystemMessage từ các file .md.
    Text người dùng truyền riêng thành HumanMessage trong node.

    Map:
        'summarize' → summarize.md
        'explain'   → explanation.md
        'qa'        → qa.md
        'suggestions' -> suggestions.md
    """

    _PROMPT_FILES: dict[str, str] = {
        "summarize":      "summarize.md",
        "explain":        "explanation.md",
        "qa":             "qa.md",
        "suggestions":    "suggestions.md",
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
        system_text = _load_prompt_text(filename)
        return SystemMessage(content=system_text)

    @staticmethod
    def get_summarize_prompt() -> SystemMessage:
        return PromptFactory.get("summarize")

    @staticmethod
    def get_explain_prompt() -> SystemMessage:
        return PromptFactory.get("explain")

    @staticmethod
    def get_qa_prompt() -> SystemMessage:
        return PromptFactory.get("qa")

    @staticmethod
    def get_suggestions_prompt() -> SystemMessage:
        return PromptFactory.get("suggestions")
