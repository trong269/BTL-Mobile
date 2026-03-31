from functools import partial
from typing import AsyncGenerator

from langgraph.graph import StateGraph, START, END
from langgraph.graph.state import CompiledStateGraph

from src.core.agents.base import BaseBookAgent
from src.core.agents.components.states import VisionAgentState
from src.core.agents.components.nodes import analyze_image_node
from src.core.llm.factory import LLMFactory
from src.core.prompts.factory import PromptFactory
from src.utils.logging import get_logger
from src.utils.tracing import tracer

logger = get_logger(__name__)


class AnalyzeImageAgent(BaseBookAgent):
    """
    Agent phân tích hình ảnh và trả lời câu hỏi.
    Graph: START → analyze_image_node → END
    """

    def build_graph(self) -> CompiledStateGraph:
        try:
            logger.info("Building AnalyzeImageAgent graph...")
            llm = LLMFactory.create()
            system_message = PromptFactory.get_analyze_image_prompt()
            
            analyze_node_bound = partial(
                analyze_image_node,
                llm=llm,
                system_message=system_message,
            )

            graph = StateGraph(VisionAgentState)
            graph.add_node("analyze_image_node", analyze_node_bound)
            graph.add_edge(START, "analyze_image_node")
            graph.add_edge("analyze_image_node", END)

            compiled = graph.compile()
            logger.info("AnalyzeImageAgent graph built successfully")
            return compiled
        except Exception as e:
            logger.error("Failed to build AnalyzeImageAgent graph: %s", e)
            raise

    def run(self, text: str, image_url: str = "", book_name: str = "", **kwargs) -> str:
        try:
            initial_state = {
                "question": text,
                "image_url": image_url,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="AnalyzeImageAgent.run")
            config = {"callbacks": [handler], "run_name": "AnalyzeImageAgent.run"} if handler else {}

            final_state = self._graph.invoke(initial_state, config=config)
            result = final_state["result"]
            return result
        except Exception as e:
            logger.error("AnalyzeImageAgent.run() failed: %s", e)
            raise

    async def arun(self, text: str, image_url: str = "", book_name: str = "", **kwargs) -> str:
        try:
            initial_state = {
                "question": text,
                "image_url": image_url,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="AnalyzeImageAgent.arun")
            config = {"callbacks": [handler], "run_name": "AnalyzeImageAgent.arun"} if handler else {}

            final_state = await self._graph.ainvoke(initial_state, config=config)
            result = final_state["result"]
            return result
        except Exception as e:
            logger.error("AnalyzeImageAgent.arun() failed: %s", e)
            raise

    async def astream(self, text: str, image_url: str = "", book_name: str = "", **kwargs) -> AsyncGenerator[str, None]:
        try:
            initial_state = {
                "question": text,
                "image_url": image_url,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            # Tích hợp Langfuse Tracing
            handler = tracer.get_handler(trace_name="AnalyzeImageAgent.astream")
            config = {"callbacks": [handler], "run_name": "AnalyzeImageAgent.astream"} if handler else {}

            # Lặp qua các event để stream từng token của LLM
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
            logger.error("AnalyzeImageAgent.astream() failed: %s", e)
            raise
