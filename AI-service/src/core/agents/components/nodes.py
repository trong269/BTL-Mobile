from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.language_models import BaseChatModel
from src.core.agents.components.states import (
    ExplainAgentState,
    SummarizeAgentState,
    QAAgentState,
    QAChatMessage,
    SuggestionAgentState,
)
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


def _normalize_chat_history(chat_history: list[QAChatMessage]) -> list[QAChatMessage]:
    normalized: list[QAChatMessage] = []
    for item in chat_history:
        role = str(item.get("role", "")).strip().lower()
        content = str(item.get("content", "")).strip()
        if role not in {"user", "assistant"} or not content:
            continue
        normalized.append({"role": role, "content": content})
    return normalized


def _build_qa_payload(state: QAAgentState) -> str:
    book_name = state.get("book_name", "").strip() or "Không rõ"
    chapter_title = state.get("current_chapter_title", "").strip() or "Không rõ"
    question = state.get("text", "").strip()
    context_chunks = [chunk.strip() for chunk in state.get("context_chunks", []) if chunk and chunk.strip()]
    chat_history = _normalize_chat_history(state.get("chat_history", []))

    parts = [
        f"Tác phẩm: {book_name}",
        f"Chương hiện tại: {chapter_title}",
    ]

    if context_chunks:
        chunk_lines = [f"[{idx}] {chunk}" for idx, chunk in enumerate(context_chunks, start=1)]
        parts.append("Ngữ liệu truy xuất liên quan:\n" + "\n\n".join(chunk_lines))
    else:
        parts.append("Ngữ liệu truy xuất liên quan: (không có)")

    if chat_history:
        history_lines = [
            f"{'Người dùng' if item['role'] == 'user' else 'Trợ lý'}: {item['content']}"
            for item in chat_history
        ]
        parts.append("Lịch sử hội thoại gần nhất:\n" + "\n".join(history_lines))

    parts.append(f"Câu hỏi mới của người dùng:\n{question}")
    return "\n\n".join(parts)


def _build_suggestion_payload(state: SuggestionAgentState) -> str:
    book_name = state.get("book_name", "").strip() or "Không rõ"
    chapter_title = state.get("current_chapter_title", "").strip() or "Không rõ"
    chapter_text = state.get("text", "").strip()

    parts = [
        f"Tác phẩm: {book_name}",
        f"Chương hiện tại: {chapter_title}",
        "Nội dung chương:\n" + chapter_text,
    ]
    return "\n\n".join(parts)


async def _invoke_and_unpack(
    llm: BaseChatModel,
    system_message: SystemMessage,
    content: str,
) -> tuple[str, list[AIMessage]]:
    human_message = HumanMessage(content=content)
    response = await llm.ainvoke([system_message, human_message])

    if isinstance(response.content, list):
        result_text = "".join(
            item.get("text", "") if isinstance(item, dict) else str(item)
            for item in response.content
        )
    else:
        result_text = str(response.content)

    return result_text, [AIMessage(content=result_text)]


async def explain_node(state: ExplainAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        content = _build_reader_context_payload(state)
        result_text, messages = await _invoke_and_unpack(llm, system_message, content)

        return {
            "result": result_text,
            "messages": messages,
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'explain': {e}") from e


async def summarize_node(state: SummarizeAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        content = _build_reader_context_payload(state)
        result_text, messages = await _invoke_and_unpack(llm, system_message, content)

        return {
            "result": result_text,
            "messages": messages,
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'summarize': {e}") from e


async def qa_node(state: QAAgentState, llm: BaseChatModel, system_message: SystemMessage) -> dict:
    try:
        content = _build_qa_payload(state)
        result_text, messages = await _invoke_and_unpack(llm, system_message, content)

        return {
            "result": result_text,
            "messages": messages,
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'qa': {e}") from e


async def suggestion_node(
    state: SuggestionAgentState,
    llm: BaseChatModel,
    system_message: SystemMessage,
) -> dict:
    try:
        content = _build_suggestion_payload(state)
        result_text, messages = await _invoke_and_unpack(llm, system_message, content)
        return {
            "result": result_text,
            "messages": messages,
        }
    except Exception as e:
        raise RuntimeError(f"LLM thất bại khi xử lý task 'suggestions': {e}") from e
