import json
import re
import unicodedata
from typing import AsyncGenerator

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, field_validator

from src.core.agents.factory import AgentFactory
from src.utils.logging import get_logger
from src.utils.response_sanitizer import sanitize_reader_ai_output

logger = get_logger(__name__)
router = APIRouter()


# ─── Request / Response Schemas ───────────────────────────────────────────────

class TextRequest(BaseModel):
    text: str
    book_name: str = ""
    context_before: str = ""
    context_after: str = ""

    @field_validator("text")
    @classmethod
    def text_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'text' không được để trống.")
        return v.strip()

    @field_validator("book_name", "context_before", "context_after")
    @classmethod
    def optional_fields_should_be_trimmed(cls, v: str) -> str:
        return v.strip() if isinstance(v, str) else ""

    model_config = {
        "json_schema_extra": {
            "example": {
                "text": "Lão Hạc là một người nông dân nghèo, sống cô độc với một con chó tên là Vàng.",
                "book_name": "Lão Hạc",
                "context_before": "Ông giáo nhớ lại những ngày tháng cuối đời của lão.",
                "context_after": "Từ đó, bi kịch của lão hiện ra rõ ràng hơn.",
            }
        }
    }


class QAChatMessage(BaseModel):
    role: str
    content: str

    @field_validator("role")
    @classmethod
    def role_must_be_user_or_assistant(cls, v: str) -> str:
        normalized = (v or "").strip().lower()
        if normalized not in {"user", "assistant"}:
            raise ValueError("role phải là 'user' hoặc 'assistant'.")
        return normalized

    @field_validator("content")
    @classmethod
    def content_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("content không được để trống.")
        return v.strip()


class QARequest(BaseModel):
    question: str
    book_name: str = ""
    current_chapter_title: str = ""
    context_chunks: list[str] = Field(default_factory=list)
    chat_history: list[QAChatMessage] = Field(default_factory=list)

    @field_validator("question")
    @classmethod
    def question_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'question' không được để trống.")
        return v.strip()

    @field_validator("book_name", "current_chapter_title")
    @classmethod
    def qa_optional_fields_should_be_trimmed(cls, v: str) -> str:
        return v.strip() if isinstance(v, str) else ""

    @field_validator("context_chunks")
    @classmethod
    def normalize_context_chunks(cls, chunks: list[str]) -> list[str]:
        normalized = [chunk.strip() for chunk in chunks if isinstance(chunk, str) and chunk.strip()]
        return normalized[:6]

    @field_validator("chat_history")
    @classmethod
    def limit_chat_history(cls, history: list[QAChatMessage]) -> list[QAChatMessage]:
        return history[-6:]

    model_config = {
        "json_schema_extra": {
            "example": {
                "question": "Nhân vật chính đang muốn điều gì ở đoạn này?",
                "book_name": "Lão Hạc",
                "current_chapter_title": "Chương 3",
                "context_chunks": [
                    "Lão Hạc nhìn con chó Vàng rất lâu rồi thở dài.",
                    "Ông giáo nhận ra lão đang giấu một nỗi dằn vặt rất sâu.",
                ],
                "chat_history": [
                    {"role": "user", "content": "Đoạn này có gì đáng chú ý?"},
                    {"role": "assistant", "content": "- Trọng tâm là sự giằng xé nội tâm của lão Hạc."},
                ],
            }
        }
    }


class SuggestionRequest(BaseModel):
    book_name: str = ""
    current_chapter_title: str = ""
    chapter_text: str
    max_questions: int = 5

    @field_validator("book_name", "current_chapter_title")
    @classmethod
    def suggestion_optional_fields_should_be_trimmed(cls, v: str) -> str:
        return v.strip() if isinstance(v, str) else ""

    @field_validator("chapter_text")
    @classmethod
    def chapter_text_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'chapter_text' không được để trống.")
        return v.strip()

    @field_validator("max_questions")
    @classmethod
    def max_questions_should_be_reasonable(cls, v: int) -> int:
        return max(1, min(v, 8))


class AIResponse(BaseModel):
    result: str
    task: str


class SuggestionResponse(BaseModel):
    questions: list[str]
    task: str


# ─── Internal Helpers ─────────────────────────────────────────────────────────

def _raise_task_exception(task: str, error: Exception) -> None:
    if isinstance(error, HTTPException):
        raise error
    if isinstance(error, ValueError):
        logger.warning("POST /%s bad request: %s", task, error)
        raise HTTPException(status_code=400, detail=str(error))
    logger.error("POST /%s internal error: %s", task, error)
    if task == "explain":
        detail = f"Lỗi hệ thống trong quá trình giải thích: {error}"
    elif task == "summarize":
        detail = f"Lỗi khi tóm tắt: {error}"
    elif task == "suggestions":
        detail = f"Lỗi khi sinh câu hỏi gợi ý: {error}"
    else:
        detail = f"Lỗi khi xử lý QA: {error}"
    raise HTTPException(status_code=500, detail=detail)


def _format_sse_event(payload: dict) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


def _default_suggestions() -> list[str]:
    return [
        "Đoạn này nói gì theo 2-3 ý chính?",
        "Nhân vật chính đang muốn điều gì ở phần này?",
        "Chi tiết nào trong đoạn này đáng chú ý nhất?",
        "Tâm trạng của nhân vật ở đoạn này thay đổi ra sao?",
        "Nếu tóm tắt nhanh phần vừa đọc thì gồm những ý nào?",
    ]


def _extract_suggestions(raw: str, max_questions: int) -> list[str]:
    if not raw or not raw.strip():
        return []

    cleaned_questions: list[str] = []
    lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    for line in lines:
        item = re.sub(r"^\s*(?:[-*•]+|[0-9]+[.)])\s*", "", line).strip(" \t\"'")
        if not item:
            continue
        if len(item) < 10:
            continue
        if not item.endswith("?"):
            item = f"{item}?"
        cleaned_questions.append(item)

    deduped: list[str] = []
    seen = set()
    for question in cleaned_questions:
        normalized = question.lower()
        if normalized in seen:
            continue
        seen.add(normalized)
        deduped.append(question)
        if len(deduped) >= max_questions:
            break
    return deduped


def _strip_accents(text: str) -> str:
    if not text:
        return ""
    return "".join(
        ch for ch in unicodedata.normalize("NFD", text)
        if unicodedata.category(ch) != "Mn"
    )


def _normalize_text_for_match(text: str) -> str:
    if not text:
        return ""
    normalized = _strip_accents(text).lower()
    normalized = re.sub(r"[^a-z0-9\s]", " ", normalized)
    return re.sub(r"\s+", " ", normalized).strip()


def _tokenize_for_match(text: str) -> set[str]:
    normalized = _normalize_text_for_match(text)
    if not normalized:
        return set()
    return {token for token in normalized.split(" ") if len(token) >= 2}


def _extract_evidence_lines(answer: str) -> list[str]:
    if not answer or not answer.strip():
        return []

    evidences: list[str] = []
    in_evidence_section = False
    lines = answer.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    for line in lines:
        stripped = line.strip()
        normalized = _normalize_text_for_match(stripped)
        if normalized.startswith("bang chung"):
            in_evidence_section = True
            continue

        if not in_evidence_section:
            continue

        if not stripped:
            if evidences:
                break
            continue

        cleaned = re.sub(r"^\s*(?:[-*•]+|[0-9]+[.)])\s*", "", stripped).strip(" \t\"'")
        if not cleaned:
            continue
        evidences.append(cleaned)
        if len(evidences) >= 2:
            break

    if evidences:
        return evidences

    quoted_spans = re.findall(r"[\"“](.{20,220}?)[\"”]", answer)
    for quoted in quoted_spans:
        candidate = quoted.strip()
        if candidate:
            evidences.append(candidate)
        if len(evidences) >= 2:
            break
    return evidences


def _is_evidence_grounded(evidence: str, context_chunks: list[str]) -> bool:
    if not evidence or not context_chunks:
        return False

    evidence_norm = _normalize_text_for_match(evidence)
    evidence_tokens = _tokenize_for_match(evidence)
    if not evidence_norm or not evidence_tokens:
        return False

    for chunk in context_chunks:
        chunk_norm = _normalize_text_for_match(chunk)
        if not chunk_norm:
            continue

        if len(evidence_norm) >= 20 and evidence_norm in chunk_norm:
            return True
        if len(chunk_norm) >= 20 and chunk_norm in evidence_norm:
            return True

        chunk_tokens = _tokenize_for_match(chunk)
        if not chunk_tokens:
            continue

        overlap = evidence_tokens.intersection(chunk_tokens)
        overlap_ratio = len(overlap) / max(len(evidence_tokens), 1)
        if len(overlap) >= 3 and overlap_ratio >= 0.38:
            return True

    return False


def _validate_qa_grounding(raw_result: str, context_chunks: list[str]) -> str:
    fallback = "Mình chưa đủ dữ kiện trong phần đã đọc để trả lời chắc chắn câu này."
    sanitized = sanitize_reader_ai_output(raw_result, mode="qa")
    if not context_chunks:
        return fallback

    evidence_lines = _extract_evidence_lines(sanitized)
    if not evidence_lines:
        return fallback

    grounded_count = sum(
        1 for evidence in evidence_lines
        if _is_evidence_grounded(evidence, context_chunks)
    )
    if grounded_count < 1:
        return fallback

    return sanitized


def _iter_stream_tokens(text: str):
    chunks = re.findall(r"\S+\s*", text or "")
    if not chunks and text:
        chunks = [text]
    for chunk in chunks:
        yield chunk


# ─── Endpoints ────────────────────────────────────────────────────────────────

@router.post(
    "/summarize",
    response_model=AIResponse,
    summary="Tóm tắt đoạn văn bản",
    description="Nhận một đoạn văn bản và trả về bản tóm tắt ngắn gọn bằng tiếng Việt.",
)
async def summarize(body: TextRequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("summarize")
        result = await agent.arun(
            text=body.text,
            book_name=body.book_name,
            context_before=body.context_before,
            context_after=body.context_after,
        )
        result = sanitize_reader_ai_output(result, mode="summarize")
        logger.info("Summarize request successful")
        return AIResponse(result=result, task="summarize")
    except Exception as error:
        _raise_task_exception("summarize", error)


@router.post(
    "/explain",
    response_model=AIResponse,
    summary="Giải thích đoạn văn bản",
    description="Nhận một đoạn văn bản và trả về phần giải thích chi tiết bằng tiếng Việt.",
)
async def explain(body: TextRequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("explain")
        result = await agent.arun(
            text=body.text,
            book_name=body.book_name,
            context_before=body.context_before,
            context_after=body.context_after,
        )
        result = sanitize_reader_ai_output(result, mode="explain")
        logger.info("Explain request successful")
        return AIResponse(result=result, task="explain")
    except Exception as error:
        _raise_task_exception("explain", error)


@router.post(
    "/qa",
    response_model=AIResponse,
    summary="Hỏi đáp theo ngữ cảnh sách",
    description="Nhận câu hỏi trong chế độ đọc và trả về câu trả lời ngắn gọn, anti-spoiler.",
)
async def qa(body: QARequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("qa")
        raw_result = await agent.arun(
            text=body.question,
            book_name=body.book_name,
            current_chapter_title=body.current_chapter_title,
            context_chunks=body.context_chunks,
            chat_history=[item.model_dump() for item in body.chat_history],
        )
        result = _validate_qa_grounding(raw_result, body.context_chunks)
        logger.info("QA request successful")
        return AIResponse(result=result, task="qa")
    except Exception as error:
        _raise_task_exception("qa", error)


@router.post(
    "/qa/stream",
    summary="Stream câu trả lời QA theo token",
    description="Nhận câu hỏi và stream dần câu trả lời để hiển thị realtime trên mobile.",
)
async def qa_stream(body: QARequest) -> StreamingResponse:
    agent = AgentFactory.get_agent("qa")

    async def stream_generator() -> AsyncGenerator[str, None]:
        try:
            collected_tokens: list[str] = []
            async for token in agent.astream(
                text=body.question,
                book_name=body.book_name,
                current_chapter_title=body.current_chapter_title,
                context_chunks=body.context_chunks,
                chat_history=[item.model_dump() for item in body.chat_history],
            ):
                if token:
                    collected_tokens.append(token)

            raw_result = "".join(collected_tokens)
            final_result = _validate_qa_grounding(raw_result, body.context_chunks)

            for token in _iter_stream_tokens(final_result):
                yield _format_sse_event({"token": token})
            yield _format_sse_event({"done": True})
        except Exception as error:
            logger.error("POST /qa/stream internal error: %s", error)
            yield _format_sse_event(
                {
                    "error": "Không thể stream câu trả lời lúc này.",
                    "done": True,
                }
            )

    return StreamingResponse(
        stream_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.post(
    "/suggestions",
    response_model=SuggestionResponse,
    summary="Sinh câu hỏi gợi ý theo chương",
    description="Sinh tối đa 5 câu hỏi gợi ý để người dùng hỏi nhanh trong chế độ đọc.",
)
async def suggestions(body: SuggestionRequest) -> SuggestionResponse:
    try:
        agent = AgentFactory.get_agent("suggestions")
        raw_result = await agent.arun(
            text=body.chapter_text,
            book_name=body.book_name,
            current_chapter_title=body.current_chapter_title,
            max_questions=body.max_questions,
            task_type="suggestions",
        )

        suggestions_list = _extract_suggestions(raw_result, max_questions=body.max_questions)
        defaults = _default_suggestions()
        if len(suggestions_list) < body.max_questions:
            for fallback in defaults:
                if fallback.lower() in {item.lower() for item in suggestions_list}:
                    continue
                suggestions_list.append(fallback)
                if len(suggestions_list) >= body.max_questions:
                    break

        return SuggestionResponse(
            questions=suggestions_list[:body.max_questions],
            task="suggestions",
        )
    except Exception as error:
        _raise_task_exception("suggestions", error)
