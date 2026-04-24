import os
import sys
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

# Thêm thư mục gốc vào `sys.path` để có thể import từ `src` và `main`
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from main import app


class MockAgent:
    """Mock thay thế AI Agent thật để không phụ thuộc vào LLM/API key"""

    async def arun(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
        current_chapter_title: str = "",
        context_chunks: list[str] | None = None,
        chat_history: list[dict] | None = None,
        max_questions: int = 5,
        **kwargs,
    ) -> str:
        if kwargs.get("task_type") == "suggestions" or "max_questions" in kwargs:
            return (
                "- Đoạn này tập trung vào mâu thuẫn nào?\n"
                "- Nhân vật chính đang theo đuổi điều gì?\n"
                "- Chi tiết nào báo hiệu thay đổi sắp tới?\n"
                "- Tâm trạng nhân vật biến chuyển ra sao?\n"
                "- Nếu tóm nhanh, 3 ý quan trọng là gì?"
            )

        return (
            f"## Mocked result\n"
            f"- text:{text}\n"
            f"- book:{book_name}\n"
            f"- chapter:{current_chapter_title}\n"
            f"- before:{context_before}\n"
            f"- after:{context_after}\n"
            f"- chunks:{len(context_chunks or [])}\n"
            f"- history:{len(chat_history or [])}"
        )

    async def astream(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
        current_chapter_title: str = "",
        context_chunks: list[str] | None = None,
        chat_history: list[dict] | None = None,
        **kwargs,
    ):
        words = ["Mocked", "stream", "result", "for:", text]
        for word in words:
            yield word + " "


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def mock_agent_factory():
    with patch("src.routers.ai_router.AgentFactory.get_agent", return_value=MockAgent()) as mocked:
        yield mocked


# ----------------- TESTS -----------------


def test_health_check(client):
    """Test API health check"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "Book AI Service"}


def test_summarize_success(client, mock_agent_factory):
    """Test API summarize với dữ liệu hợp lệ"""
    payload = {
        "text": "Đây là nội dung cần tóm tắt",
        "book_name": "Lão Hạc",
        "context_before": "Ông giáo đang hồi tưởng.",
        "context_after": "Bi kịch dần hiện ra.",
    }
    response = client.post("/api/ai/summarize", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "summarize"
    assert "Mocked result" in data["result"]
    mock_agent_factory.assert_called_once_with("summarize")


def test_summarize_empty_text(client):
    """Test API summarize với text rỗng (Pydantic validator sẽ báo lỗi 422)"""
    payload = {"text": "   "}
    response = client.post("/api/ai/summarize", json=payload)
    assert response.status_code == 422
    error_detail = response.json()["detail"][0]["msg"]
    assert "không được để trống" in error_detail.lower() or "value error" in error_detail.lower()


def test_explain_success(client, mock_agent_factory):
    """Test API explain với dữ liệu hợp lệ"""
    payload = {
        "text": "Khái niệm này có nghĩa là gì",
        "book_name": "Sapiens",
        "context_before": "Đoạn trước nói về nông nghiệp.",
        "context_after": "Đoạn sau nói về hệ quả.",
    }
    response = client.post("/api/ai/explain", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "explain"
    assert "Mocked result" in data["result"]
    mock_agent_factory.assert_called_once_with("explain")


def test_explain_empty_text(client):
    """Test API explain với text bị thiếu rỗng"""
    payload = {"text": ""}
    response = client.post("/api/ai/explain", json=payload)
    assert response.status_code == 422


def test_qa_success(client, mock_agent_factory):
    payload = {
        "question": "Nhân vật này đang muốn gì?",
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
    response = client.post("/api/ai/qa", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "qa"
    assert "Mocked result" in data["result"]
    mock_agent_factory.assert_called_once_with("qa")


def test_qa_empty_question(client):
    payload = {
        "question": "   ",
        "context_chunks": [],
        "chat_history": [],
    }
    response = client.post("/api/ai/qa", json=payload)
    assert response.status_code == 422


def test_qa_stream_success(client, mock_agent_factory):
    payload = {
        "question": "Giải thích mâu thuẫn ở đoạn này",
        "book_name": "Lão Hạc",
        "current_chapter_title": "Chương 2",
        "context_chunks": ["Đoạn 1", "Đoạn 2"],
        "chat_history": [],
    }
    response = client.post("/api/ai/qa/stream", json=payload)
    assert response.status_code == 200
    assert "data:" in response.text
    assert "Mocked" in response.text
    assert '"done": true' in response.text
    mock_agent_factory.assert_called_once_with("qa")


def test_suggestions_success(client, mock_agent_factory):
    payload = {
        "book_name": "Lão Hạc",
        "current_chapter_title": "Chương 2",
        "chapter_text": "Lão Hạc nhìn con chó Vàng và thở dài.",
        "max_questions": 5,
    }
    response = client.post("/api/ai/suggestions", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "suggestions"
    assert len(data["questions"]) == 5
    mock_agent_factory.assert_called_once_with("suggestions")


def test_suggestions_empty_chapter_text(client):
    payload = {
        "book_name": "Lão Hạc",
        "current_chapter_title": "Chương 2",
        "chapter_text": "   ",
    }
    response = client.post("/api/ai/suggestions", json=payload)
    assert response.status_code == 422

