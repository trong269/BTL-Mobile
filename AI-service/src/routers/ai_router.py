from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, field_validator
from src.utils.logging import get_logger
from src.core.agents.factory import AgentFactory


logger = get_logger(__name__)
router = APIRouter()


# ─── Request / Response Schemas ───────────────────────────────────────────────

class TextRequest(BaseModel):
    text: str
    book_name: str = ""

    @field_validator("text")
    @classmethod
    def text_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'text' không được để trống.")
        return v.strip()


class AIResponse(BaseModel):
    result: str
    task: str


# ─── Helper ───────────────────────────────────────────────────────────────────

def _get_factory(request: Request):
    try:
        if hasattr(request.app.state, "agent_factory"):
            return request.app.state.agent_factory
        return AgentFactory
    except Exception as e:
        logger.error("AgentFactory not found - startup may have failed")
        raise HTTPException(status_code=503, detail="AI Agent chưa sẵn sàng.")


# ─── Endpoints ────────────────────────────────────────────────────────────────

@router.post(
    "/summarize",
    response_model=AIResponse,
    summary="Tóm tắt đoạn văn bản",
    description="Nhận một đoạn văn bản và trả về bản tóm tắt ngắn gọn bằng tiếng Việt.",
)
async def summarize(body: TextRequest, request: Request) -> AIResponse:
    logger.info("POST /summarize | text length=%d chars", len(body.text))
    try:
        agent_factory = _get_factory(request)
        agent = agent_factory.get_agent("summarize")
        
        # Gọi agent giải quyết (bất đồng bộ)
        result = await agent.arun(text=body.text, book_name=body.book_name)
        
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
async def explain(body: TextRequest, request: Request) -> AIResponse:
    logger.info("POST /explain | text length=%d chars", len(body.text))
    try:
        agent_factory = _get_factory(request)
        agent = agent_factory.get_agent("explain")
        
        # Gọi agent giải quyết (bất đồng bộ)
        result = await agent.arun(text=body.text, book_name=body.book_name)
        
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


@router.post("/summarize/stream", summary="Tóm tắt nội dung sách (Streaming)")
async def summarize_stream(body: TextRequest, request: Request):
    """
    API Tóm tắt nội dung sách trả về Stream (chơi nối chữ để UI hiện dần dần).
    """
    logger.info("Received summarize_stream request | text_length: %d", len(body.text))
    try:
        agent_factory = _get_factory(request)
        agent = agent_factory.get_agent("summarize")

        async def token_generator():
            try:
                async for chunk in agent.astream(text=body.text, book_name=body.book_name):
                    if isinstance(chunk, str):
                        yield chunk
                    elif isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, str):
                                yield item
                            elif isinstance(item, dict) and "text" in item:
                                yield item["text"]
                    else:
                        yield str(chunk)
            except Exception as e:
                logger.error("Streaming error: %s", e)
                yield f"\n[Error: {str(e)}]"

        return StreamingResponse(token_generator(), media_type="text/plain")
        
    except ValueError as ve:
        logger.warning("Summarize_stream bad request: %s", ve)
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        logger.error("Summarize_stream error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi hệ thống khi stream summarize")


@router.post("/explain/stream", summary="Giải thích nội dung sách (Streaming)")
async def explain_stream(body: TextRequest, request: Request):
    """
    API Giải thích nội dung sách trả về Stream (từng token một).
    """
    logger.info("Received explain_stream request | text_length: %d", len(body.text))
    try:
        agent_factory = _get_factory(request)
        agent = agent_factory.get_agent("explain")

        async def token_generator():
            try:
                async for chunk in agent.astream(text=body.text, book_name=body.book_name):
                    if isinstance(chunk, str):
                        yield chunk
                    elif isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, str):
                                yield item
                            elif isinstance(item, dict) and "text" in item:
                                yield item["text"]
                    else:
                        yield str(chunk)
            except Exception as e:
                logger.error("Streaming error: %s", e)
                yield f"\n[Error: {str(e)}]"

        return StreamingResponse(token_generator(), media_type="text/plain")
        
    except ValueError as ve:
        logger.warning("Explain_stream bad request: %s", ve)
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        logger.error("Explain_stream error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi hệ thống khi stream explain")
