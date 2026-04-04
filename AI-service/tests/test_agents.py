import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock
from langchain_core.messages import AIMessage
from src.core.agents.components.states import ExplainAgentState, SummarizeAgentState
from src.core.agents.components.nodes import explain_node, summarize_node
from src.core.agents.explain_agent import ExplainAgent
from src.core.agents.sumarize_agent import SummarizeAgent

def test_explain_node():
    async def run_test():
        mock_llm = MagicMock()
        mock_llm.ainvoke = AsyncMock(return_value=AIMessage(content="Test explanation"))
        
        mock_system_message = MagicMock()
        state = ExplainAgentState(text="test word", book_name="Test Book", messages=[], result="")
        
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
        state = SummarizeAgentState(text="test paragraph", book_name="Test Book", messages=[], result="")
        
        result = await summarize_node(state, mock_llm, mock_system_message)
        
        assert result["result"] == "Test summary"
        assert len(result["messages"]) == 1
        assert result["messages"][0].content == "Test summary"
        
    asyncio.run(run_test())

def test_explain_agent_build():
    agent = ExplainAgent()
    graph = agent.build_graph()
    assert graph is not None

def test_summarize_agent_build():
    agent = SummarizeAgent()
    graph = agent.build_graph()
    assert graph is not None
