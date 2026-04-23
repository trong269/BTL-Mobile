from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.language_models import BaseChatModel
from src.core.agents.components.states import ExplainAgentState, SummarizeAgentState
from src.utils.logging import get_logger

logger = get_logger(__name__)


def _build_reader_context_payload(state: ExplainAgentState | SummarizeAgentState) -> str:
    book_name = state.get("book_name", "").strip() or "Không rõ"
    context_before = state.get("context_before", "").strip()
    selected_text = state.get("text", "").strip()
    context_after = state.get("context_after", "").strip()

    parts = [f"Tác phẩm: {book_name}"]
    if context_before:
        parts.append(f"Ngữ cảnh trước:\n{context_before}")
    parts.append(f"Đoạn được chọn:\n{selected_text}")
    if context_after:
        parts.append(f"Ngữ cảnh sau:\n{context_after}")
    return "\n\n".join(parts)


async def explain_node(state: ExplainAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        content = _build_reader_context_payload(state)
        human_message = HumanMessage(content=content)
        response = await llm.ainvoke([system_message, human_message])
        
        if isinstance(response.content, list):
            result_text = "".join(
                item.get("text", "") if isinstance(item, dict) else str(item)
                for item in response.content
            )
        else:
            result_text = str(response.content)
            
        return {
            "result": result_text,
            "messages": [AIMessage(content=result_text)],
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'explain': {e}") from e


async def summarize_node(state: SummarizeAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        content = _build_reader_context_payload(state)
        human_message = HumanMessage(content=content)
        response = await llm.ainvoke([system_message, human_message])
        
        if isinstance(response.content, list):
            result_text = "".join(
                item.get("text", "") if isinstance(item, dict) else str(item)
                for item in response.content
            )
        else:
            result_text = str(response.content)
            
        return {
            "result": result_text,
            "messages": [AIMessage(content=result_text)],
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'summarize': {e}") from e
