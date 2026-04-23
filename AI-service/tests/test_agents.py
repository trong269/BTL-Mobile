import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
from langchain_core.messages import AIMessage
from src.core.agents.components.states import ExplainAgentState, SummarizeAgentState
from src.core.agents.components.nodes import explain_node, summarize_node
from src.core.agents.explain_agent import ExplainAgent
from src.core.agents.summarize_agent import SummarizeAgent
from src.utils.response_sanitizer import sanitize_reader_ai_output

def test_explain_node():
    async def run_test():
        mock_llm = MagicMock()
        mock_llm.ainvoke = AsyncMock(return_value=AIMessage(content="Test explanation"))
        
        mock_system_message = MagicMock()
        state = ExplainAgentState(
            text="test word",
            book_name="Test Book",
            context_before="before",
            context_after="after",
            messages=[],
            result="",
        )
        
        result = await explain_node(state, mock_llm, mock_system_message)
        
        assert result["result"] == "Test explanation"
        assert len(result["messages"]) == 1
        assert result["messages"][0].content == "Test explanation"
    
    asyncio.run(run_test())

def test_summarize_node():
    async def run_test():
        mock_llm = MagicMock()
        mock_llm.ainvoke = AsyncMock(return_value=AIMessage(content="Test summary"))
        
        mock_system_message = MagicMock()
        state = SummarizeAgentState(
            text="test paragraph",
            book_name="Test Book",
            context_before="before",
            context_after="after",
            messages=[],
            result="",
        )
        
        result = await summarize_node(state, mock_llm, mock_system_message)
        
        assert result["result"] == "Test summary"
        assert len(result["messages"]) == 1
        assert result["messages"][0].content == "Test summary"
        
    asyncio.run(run_test())

def test_explain_agent_build():
    with patch("src.core.agents.explain_agent.LLMFactory.create", return_value=MagicMock()), \
         patch("src.core.agents.explain_agent.PromptFactory.get_explain_prompt", return_value=MagicMock()):
        agent = ExplainAgent()
        graph = agent.build_graph()
        assert graph is not None

def test_summarize_agent_build():
    with patch("src.core.agents.summarize_agent.LLMFactory.create", return_value=MagicMock()), \
         patch("src.core.agents.summarize_agent.PromptFactory.get_summarize_prompt", return_value=MagicMock()):
        agent = SummarizeAgent()
        graph = agent.build_graph()
        assert graph is not None


def test_sanitize_reader_ai_output_for_summarize():
    raw = "## Title\n- **Ý 1**\r\n- **Ý 2**"
    result = sanitize_reader_ai_output(raw, mode="summarize")
    # Should normalize newlines and preserve Markdown
    assert result == "## Title\n- **Ý 1**\n- **Ý 2**"


def test_sanitize_reader_ai_output_for_explain():
    raw = ""
    result = sanitize_reader_ai_output(raw, mode="explain")
    assert result == "Không đủ thông tin để giải nghĩa đoạn văn bản này."
