from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.language_models import BaseChatModel
from src.core.agents.components.states import ExplainAgentState, SummarizeAgentState
from src.utils.logging import get_logger

logger = get_logger(__name__)


async def explain_node(state: ExplainAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        book_name = state.get("book_name", "Không rõ")
        content = f"Tác phẩm: {book_name}\nNội dung: {state['text']}"
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
        book_name = state.get("book_name", "Không rõ")
        content = f"Tác phẩm: {book_name}\nNội dung: {state['text']}"
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
