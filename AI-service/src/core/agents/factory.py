from src.core.agents.base import BaseBookAgent
from src.core.agents.sumarize_agent import SummarizeAgent
from src.core.agents.explain_agent import ExplainAgent
from src.core.agents.analyze_image_agent import AnalyzeImageAgent
from src.utils.logging import get_logger

logger = get_logger(__name__)


class AgentFactory:
    """
    Factory dispatcher: khởi tạo và cache từng agent theo task_type.
    Không chứa logic nghiệp vụ — chỉ phân phối agent tương ứng.
    """

    _AGENT_MAP: dict[str, type[BaseBookAgent]] = {
        "summarize":      SummarizeAgent,
        "explain":        ExplainAgent,
        "analyze_image":  AnalyzeImageAgent,
    }
    _cache: dict[str, BaseBookAgent] = {}

    @classmethod
    def _build_all_agents(cls):
        """Tùy chọn: khởi tạo sẵn tất cả agents khi startup (eager init)."""
        for task_type in cls._AGENT_MAP:
            cls.get_agent(task_type)

    @classmethod
    def get_agent(cls, task_type: str) -> BaseBookAgent:
        """
        Trả về agent instance tương ứng với task_type (lazy init + cache).

        Args:
            task_type: 'summarize' | 'explain' | 'analyze_image'
        """
        task_type = task_type.lower().strip()

        if task_type not in cls._AGENT_MAP:
            supported = ", ".join(f"'{k}'" for k in cls._AGENT_MAP)
            logger.error("Unknown task_type requested: '%s'", task_type)
            raise ValueError(
                f"task_type '{task_type}' không được hỗ trợ. Chọn một trong: {supported}"
            )

        if task_type not in cls._cache:
            logger.info("Initializing agent for task_type='%s'", task_type)
            try:
                agent_class = cls._AGENT_MAP[task_type]
                cls._cache[task_type] = agent_class()
                logger.info("Agent '%s' cached successfully", task_type)
            except Exception as e:
                logger.error("Failed to initialize agent for task_type='%s': %s", task_type, e)
                raise

        return cls._cache[task_type]
