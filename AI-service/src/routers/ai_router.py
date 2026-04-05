from fastapi import APIRouter, HTTPException
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

    model_config = {
        "json_schema_extra": {
            "example": {
                "text": "Lão Hạc là một người nông dân nghèo, sống cô độc với một con chó tên là Vàng. Cuộc đời lão đầy rẫy những bi kịch và nghèo túng đến cùng cực...",
                "book_name": "Lão Hạc"
            }
        }
    }
    

class ImageRequest(BaseModel):
    text: str # Câu hỏi cho ảnh
    image_url: str # Base64 hoặc URL ảnh
    book_name: str = ""

    @field_validator("text")
    @classmethod
    def text_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'text' (câu hỏi) không được để trống.")
        return v.strip()

    @field_validator("image_url")
    @classmethod
    def image_url_must_not_be_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Trường 'image_url' không được để trống.")
        return v.strip()

    model_config = {
        "json_schema_extra": {
            "example": {
                "book_name": "sách học tiếng anh",
                "image_url": "https://encrypted-tbn2.gstatic.com/shopping?q=tbn:ANd9GcRaWJBmMZjMwMaEewidknDxM_IDZCo9KxkDF3l6TGKmOfRfLyYAfaUXH6FzjemDbotfWPycx1VTM87k-_NLacyrZB7BchBB",
                "text": "giải thích hình trên"
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
async def explain(body: TextRequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("explain")
        
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
async def summarize_stream(body: TextRequest):
    """
    API Tóm tắt nội dung sách trả về Stream (chơi nối chữ để UI hiện dần dần).
    """
    try:
        agent = AgentFactory.get_agent("summarize")

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
async def explain_stream(body: TextRequest):
    """
    API Giải thích nội dung sách trả về Stream (từng token một).
    """
    try:
        agent = AgentFactory.get_agent("explain")

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


@router.post(
    "/analyze-image",
    response_model=AIResponse,
    summary="Phân tích hình ảnh",
    description="Nhận câu hỏi và hình ảnh (URL/Base64), sau đó trả về phần phân tích nội dung.",
)
async def analyze_image(body: ImageRequest) -> AIResponse:
    try:
        agent = AgentFactory.get_agent("analyze_image")
        
        result = await agent.arun(
            text=body.text, 
            image_url=body.image_url, 
            book_name=body.book_name
        )
        return AIResponse(result=result, task="analyze_image")
    except HTTPException:
        raise
    except ValueError as e:
        logger.warning("POST /analyze-image bad request: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("POST /analyze-image internal error: %s", e)
        raise HTTPException(status_code=500, detail=f"Lỗi khi phân tích ảnh: {str(e)}")


@router.post("/analyze-image/stream", summary="Phân tích hình ảnh (Streaming)")
async def analyze_image_stream(body: ImageRequest):
    """
    API Phân tích hình ảnh trả về Stream từng token.
    """
    try:
        agent = AgentFactory.get_agent("analyze_image")

        async def token_generator():
            try:
                async for chunk in agent.astream(
                    text=body.text, 
                    image_url=body.image_url, 
                    book_name=body.book_name
                ):
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
        logger.warning("Analyze_image_stream bad request: %s", ve)
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        logger.error("Analyze_image_stream error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi hệ thống khi stream analyze-image")
