from functools import partial

from langgraph.graph import StateGraph, START, END
from langgraph.graph.state import CompiledStateGraph
from src.core.agents.base import BaseBookAgent
from src.core.agents.components.states import SummarizeAgentState
from src.core.agents.components.nodes import summarize_node
from src.core.llm.factory import LLMFactory
from src.core.prompts.factory import PromptFactory
from src.utils.logging import get_logger
from src.utils.tracing import tracer

logger = get_logger(__name__)


class SummarizeAgent(BaseBookAgent):
    """
    Agent tóm tắt văn bản.
    Graph: START → summarize_node → END
    """

    def build_graph(self) -> CompiledStateGraph:
        try:
            logger.info("Building SummarizeAgent graph...")
            llm = LLMFactory.create()
            system_message = PromptFactory.get_summarize_prompt()
            summarize_node_bound = partial(
                summarize_node,
                llm=llm,
                system_message=system_message,
            )

            graph = StateGraph(SummarizeAgentState)
            graph.add_node("summarize_node", summarize_node_bound)
            graph.add_edge(START, "summarize_node")
            graph.add_edge("summarize_node", END)

            compiled = graph.compile()
            logger.info("SummarizeAgent graph built successfully")
            return compiled
        except Exception as e:
            logger.error("Failed to build SummarizeAgent graph: %s", e)
            raise

    def run(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "context_before": context_before,
                "context_after": context_after,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="SummarizeAgent.run")
            config = {"callbacks": [handler], "run_name": "SummarizeAgent.run"} if handler else {}

            final_state = self._graph.invoke(initial_state, config=config)
            result = final_state["result"]
            return result
        except Exception as e:
            logger.error("SummarizeAgent.run() failed: %s", e)
            raise

    async def arun(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "context_before": context_before,
                "context_after": context_after,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="SummarizeAgent.arun")
            config = {"callbacks": [handler], "run_name": "SummarizeAgent.arun"} if handler else {}

            final_state = await self._graph.ainvoke(initial_state, config=config)
            result = final_state["result"]
            return result
        except Exception as e:
            logger.error("SummarizeAgent.arun() failed: %s", e)
            raise

    async def astream(
        self,
        text: str,
        book_name: str = "",
        context_before: str = "",
        context_after: str = "",
        **kwargs,
    ):
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "context_before": context_before,
                "context_after": context_after,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="SummarizeAgent.astream")
            config = {"callbacks": [handler], "run_name": "SummarizeAgent.astream"} if handler else {}

            # Lặp qua các event để stream từng token của LLM trả về
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
            logger.error("SummarizeAgent.astream() failed: %s", e)
            raise
