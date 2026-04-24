from functools import partial

from langgraph.graph import StateGraph, START, END
from langgraph.graph.state import CompiledStateGraph
from src.core.agents.base import BaseBookAgent
from src.core.agents.components.states import QAAgentState
from src.core.agents.components.nodes import qa_node
from src.core.llm.factory import LLMFactory
from src.core.prompts.factory import PromptFactory
from src.utils.logging import get_logger
from src.utils.tracing import tracer

logger = get_logger(__name__)


class QAAgent(BaseBookAgent):
    """
    Agent hỏi đáp theo ngữ cảnh sách.
    Graph: START -> qa_node -> END
    """

    def build_graph(self) -> CompiledStateGraph:
        try:
            logger.info("Building QAAgent graph...")
            llm = LLMFactory.create()
            system_message = PromptFactory.get_qa_prompt()
            qa_node_bound = partial(
                qa_node,
                llm=llm,
                system_message=system_message,
            )

            graph = StateGraph(QAAgentState)
            graph.add_node("qa_node", qa_node_bound)
            graph.add_edge(START, "qa_node")
            graph.add_edge("qa_node", END)
            compiled = graph.compile()

            logger.info("QAAgent graph built successfully")
            return compiled
        except Exception as e:
            logger.error("Failed to build QAAgent graph: %s", e)
            raise

    def run(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        context_chunks: list[str] | None = None,
        chat_history: list[dict] | None = None,
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "context_chunks": context_chunks or [],
                "chat_history": chat_history or [],
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="QAAgent.run")
            config = {"callbacks": [handler], "run_name": "QAAgent.run"} if handler else {}
            final_state = self._graph.invoke(initial_state, config=config)
            return final_state["result"]
        except Exception as e:
            logger.error("QAAgent.run() failed: %s", e)
            raise

    async def arun(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        context_chunks: list[str] | None = None,
        chat_history: list[dict] | None = None,
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "context_chunks": context_chunks or [],
                "chat_history": chat_history or [],
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="QAAgent.arun")
            config = {"callbacks": [handler], "run_name": "QAAgent.arun"} if handler else {}
            final_state = await self._graph.ainvoke(initial_state, config=config)
            return final_state["result"]
        except Exception as e:
            logger.error("QAAgent.arun() failed: %s", e)
            raise

    async def astream(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        context_chunks: list[str] | None = None,
        chat_history: list[dict] | None = None,
        **kwargs,
    ):
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "context_chunks": context_chunks or [],
                "chat_history": chat_history or [],
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="QAAgent.astream")
            config = {"callbacks": [handler], "run_name": "QAAgent.astream"} if handler else {}

            async for event in self._graph.astream_events(initial_state, version="v2", config=config):
                if event["event"] == "on_chat_model_stream":
                    chunk = event["data"]["chunk"]
                    if chunk.content:
                        if isinstance(chunk.content, str):
                            yield chunk.content
                        elif isinstance(chunk.content, list):
                            for block in chunk.content:
                                if isinstance(block, str):
                                    yield block
                                elif isinstance(block, dict) and "text" in block:
                                    yield block["text"]
                        else:
                            yield str(chunk.content)
        except Exception as e:
            logger.error("QAAgent.astream() failed: %s", e)
            raise
