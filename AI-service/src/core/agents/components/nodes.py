from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.language_models import BaseChatModel
from src.core.agents.components.states import ExplainAgentState, SummarizeAgentState, VisionAgentState
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

async def analyze_image_node(state: VisionAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        book_name = state.get("book_name", "Không rõ")
        question = state.get("question", "")
        image_data = state.get("image_url", "") 
        
        # 1. Format image URL/Base64
        formatted_image = image_data
        if not image_data.startswith(("http://", "https://", "data:")):
            formatted_image = f"data:image/jpeg;base64,{image_data}"
        
        # 2. Tách riêng phần Prompt (Text)
        prompt_text = f"Tác phẩm: {book_name}\nCâu hỏi: {question}"
        
        # 2. Đưa vào cấu trúc Multimodal chuẩn của LangChain
        human_message = HumanMessage(
            content=[
                {"type": "text", "text": prompt_text},
                {"type": "image_url", "image_url": {"url": formatted_image}} 
            ]
        )
        
        # 3. Gọi model bất đồng bộ (ainvoke)
        response = await llm.ainvoke([system_message, human_message])
        
        # 4. Xử lý kết quả trả về
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
        raise RuntimeError(f"LLM thất bại khi xử lý task 'analyze image': {e}") from e