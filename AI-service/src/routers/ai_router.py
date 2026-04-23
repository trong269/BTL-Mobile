from fastapi import APIRouter, HTTPException

from pydantic import BaseModel, field_validator
from src.utils.logging import get_logger
from src.core.agents.factory import AgentFactory
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
                "text": "Lão Hạc là một người nông dân nghèo, sống cô độc với một con chó tên là Vàng. Cuộc đời lão đầy rẫy những bi kịch và nghèo túng đến cùng cực...",
                "book_name": "Lão Hạc",
                "context_before": "Ông giáo nhớ lại những ngày tháng cuối đời của lão.",
                "context_after": "Từ đó, bi kịch của lão hiện ra rõ ràng hơn."
            }
        }
    }
    

class AIResponse(BaseModel):
    result: str
    task: str




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
        
        # Gọi agent giải quyết (bất đồng bộ)
        result = await agent.arun(
            text=body.text,
            book_name=body.book_name,
            context_before=body.context_before,
            context_after=body.context_after,
        )
        result = sanitize_reader_ai_output(result, mode="summarize")
        
        logger.info("Summarize request successful")
        return AIResponse(result=result, task="summarize")
    except HTTPException:
        raise
    except ValueError as e:
        logger.warning("POST /summarize bad request: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("POST /summarize internal error: %s", e)
        raise HTTPException(status_code=500, detail=f"Lỗi khi tóm tắt: {str(e)}")


@router.post(
    "/explain",
    response_model=AIResponse,
    summary="Giải thích đoạn văn bản",
    description="Nhận một đoạn văn bản và trả về phần giải thích chi tiết bằng tiếng Việt.",
)
async def explain(body: TextRequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("explain")
        
        # Gọi agent giải quyết (bất đồng bộ)
        result = await agent.arun(
            text=body.text,
            book_name=body.book_name,
            context_before=body.context_before,
            context_after=body.context_after,
        )
        result = sanitize_reader_ai_output(result, mode="explain")
        
        logger.info("Explain request successful")
        return AIResponse(result=result, task="explain")
    except HTTPException:
        raise
    except ValueError as e:
        logger.warning("POST /explain bad request: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("POST /explain internal error: %s", e)
        raise HTTPException(status_code=500,            detail=f"Lỗi hệ thống trong quá trình giải thích: {str(e)}"
        )


