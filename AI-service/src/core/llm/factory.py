from langchain_core.language_models import BaseChatModel
from src.utils.config import config_manager
from src.utils.logging import get_logger

logger = get_logger(__name__)


class LLMFactory:
    """
    Factory tạo LLM instance theo provider cấu hình trong app.yaml.

    Supported providers:
        - "gemini" : Google Gemini via langchain-google-genai
        - "openai" : OpenAI GPT via langchain-openai
        - "local"  : Local model (LM Studio, vLLM, ...) dùng ChatOpenAI
                     với base_url trỏ tới server local
        - "ollama" : Ollama local models
    """

    SUPPORTED_PROVIDERS = ("gemini", "openai", "local", "ollama")

    @staticmethod
    def create() -> BaseChatModel:
        provider = config_manager.llm_provider
        logger.info("Creating LLM | provider='%s'", provider)

        try:
            if provider == "gemini":
                return LLMFactory._create_gemini()
            elif provider == "openai":
                return LLMFactory._create_openai()
            elif provider == "local":
                return LLMFactory._create_local()
            elif provider == "ollama":
                return LLMFactory._create_ollama()
            else:
                raise ValueError(
                    f"LLM provider '{provider}' không được hỗ trợ. "
                    f"Chọn một trong: {', '.join(LLMFactory.SUPPORTED_PROVIDERS)}"
                )
        except ValueError:
            raise
        except Exception as e:
            logger.error("Failed to create LLM (provider=%s): %s", provider, e)
            raise RuntimeError(f"Không thể khởi tạo LLM provider '{provider}': {e}") from e

    # ─── Gemini ───────────────────────────────────────────────────────────────

    @staticmethod
    def _create_gemini() -> BaseChatModel:
        from langchain_google_genai import ChatGoogleGenerativeAI
        if not config_manager.gemini_api_key:
            raise ValueError("GOOGLE_API_KEY chưa được cấu hình trong .env")
        logger.debug("Gemini | model=%s | temperature=%s | top_p=%s | max_tokens=%s",
                     config_manager.gemini_model, config_manager.llm_temperature,
                     config_manager.llm_top_p, config_manager.llm_max_tokens)
        return ChatGoogleGenerativeAI(
            model=config_manager.gemini_model,
            google_api_key=config_manager.gemini_api_key,
            temperature=config_manager.llm_temperature,
            top_p=config_manager.llm_top_p,
            max_output_tokens=config_manager.llm_max_tokens,
        )

    # ─── OpenAI ───────────────────────────────────────────────────────────────

    @staticmethod
    def _create_openai() -> BaseChatModel:
        from langchain_openai import ChatOpenAI
        if not config_manager.openai_api_key:
            raise ValueError("OPENAI_API_KEY chưa được cấu hình trong .env")
        logger.debug("OpenAI | model=%s | temperature=%s | top_p=%s | max_tokens=%s",
                     config_manager.openai_model, config_manager.llm_temperature,
                     config_manager.llm_top_p, config_manager.llm_max_tokens)
        return ChatOpenAI(
            model=config_manager.openai_model,
            api_key=config_manager.openai_api_key,
            temperature=config_manager.llm_temperature,
            top_p=config_manager.llm_top_p,
            max_tokens=config_manager.llm_max_tokens,
        )

    # ─── Local model (LM Studio / vLLM / any OpenAI-compatible server) ────────

    @staticmethod
    def _create_local() -> BaseChatModel:
        from langchain_openai import ChatOpenAI
        logger.debug(
            "Local model | base_url=%s | model=%s | temperature=%s | top_p=%s | max_tokens=%s",
            config_manager.local_base_url, config_manager.local_model,
            config_manager.llm_temperature, config_manager.llm_top_p, config_manager.llm_max_tokens,
        )
        return ChatOpenAI(
            model=config_manager.local_model,
            base_url=config_manager.local_base_url,
            api_key=config_manager.local_api_key or "not-needed",
            temperature=config_manager.llm_temperature,
            top_p=config_manager.llm_top_p,
            max_tokens=config_manager.llm_max_tokens,
        )

    # ─── Ollama ───────────────────────────────────────────────────────────────

    @staticmethod
    def _create_ollama() -> BaseChatModel:
        from langchain_ollama import ChatOllama
        logger.debug(
            "Ollama | model=%s | base_url=%s | temperature=%s | top_p=%s",
            config_manager.ollama_model, config_manager.ollama_base_url,
            config_manager.llm_temperature, config_manager.llm_top_p,
        )
        return ChatOllama(
            model=config_manager.ollama_model,
            base_url=config_manager.ollama_base_url,
            temperature=config_manager.llm_temperature,
            top_p=config_manager.llm_top_p,
            num_predict=config_manager.llm_max_tokens,
        )
