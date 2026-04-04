import os
import sys
import pytest
from fastapi.testclient import TestClient

# Thêm thư mục gốc vào `sys.path` để có thể import từ `src` và `main`
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from main import app

class MockAgent:
    """Mock thay thế AI Agent thật để không phụ thuộc vào LLM/API key"""
    async def arun(self, text: str, book_name: str = "") -> str:
        return f"Mocked result for: {text}"

    async def astream(self, text: str, book_name: str = ""):
        words = ["Mocked", "stream", "result", "for:", text]
        for word in words:
            yield word + " "

class MockAgentFactory:
    """Mock thay thế AgentFactory"""
    def get_agent(self, name: str):
        if name not in ["summarize", "explain"]:
            raise ValueError(f"Agent {name} not found")
        return MockAgent()

@pytest.fixture
def mock_app():
    # Override app.state để test không gọi LLM
    app.state.agent_factory = MockAgentFactory()
    return app

@pytest.fixture
def client(mock_app):
    return TestClient(mock_app)

# ----------------- TESTS -----------------

def test_health_check(client):
    """Test API health check"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "Book AI Service"}

def test_summarize_success(client):
    """Test API summarize với dữ liệu hợp lệ"""
    payload = {"text": "Đây là nội dung cần tóm tắt"}
    response = client.post("/api/ai/summarize", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "summarize"
    assert "Mocked result for" in data["result"]

def test_summarize_empty_text(client):
    """Test API summarize với text rỗng (Pydantic validator sẽ báo lỗi 422)"""
    payload = {"text": "   "}
    response = client.post("/api/ai/summarize", json=payload)
    # Lỗi pydantic validation sẽ trả về 422
    assert response.status_code == 422
    error_detail = response.json()["detail"][0]["msg"]
    assert "không được để trống" in error_detail.lower() or "value error" in error_detail.lower()

def test_explain_success(client):
    """Test API explain với dữ liệu hợp lệ"""
    payload = {"text": "Khái niệm này có nghĩa là gì"}
    response = client.post("/api/ai/explain", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["task"] == "explain"
    assert "Mocked result for" in data["result"]

def test_explain_empty_text(client):
    """Test API explain với text bị thiếu rỗng"""
    payload = {"text": ""}
    response = client.post("/api/ai/explain", json=payload)
    assert response.status_code == 422

def test_summarize_stream(client):
    """Test API stream summarize"""
    payload = {"text": "Nội dung stream tóm tắt"}
    with client.stream("POST", "/api/ai/summarize/stream", json=payload) as response:
        assert response.status_code == 200
        content = ""
        for chunk in response.iter_text():
            content += chunk
        assert "Mocked stream result" in content

def test_explain_stream(client):
    """Test API stream explain"""
    payload = {"text": "Nội dung stream giải thích"}
    with client.stream("POST", "/api/ai/explain/stream", json=payload) as response:
        assert response.status_code == 200
        content = ""
        for chunk in response.iter_text():
            content += chunk
        assert "Mocked stream result" in content
