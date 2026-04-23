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
    ) -> str:
        return (
            f"## Mocked result\n"
            f"- before:{context_before}\n"
            f"- text:{text}\n"
            f"- after:{context_after}"
        )

    async def astream(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
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
    assert data["result"].startswith("- ")
    assert "#" not in data["result"]
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
    assert data["result"].startswith("- ")
    assert "#" not in data["result"]
    mock_agent_factory.assert_called_once_with("explain")


def test_explain_empty_text(client):
    """Test API explain với text bị thiếu rỗng"""
    payload = {"text": ""}
    response = client.post("/api/ai/explain", json=payload)
    assert response.status_code == 422



