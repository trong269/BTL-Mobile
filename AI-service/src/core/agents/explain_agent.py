from functools import partial

from langgraph.graph import StateGraph, START, END
from langgraph.graph.state import CompiledStateGraph
from src.core.agents.base import BaseBookAgent
from src.core.agents.components.states import ExplainAgentState
from src.core.agents.components.nodes import explain_node
from src.core.llm.factory import LLMFactory
from src.core.prompts.factory import PromptFactory
from src.utils.logging import get_logger

logger = get_logger(__name__)


class ExplainAgent(BaseBookAgent):
    """
    Agent giải thích văn bản.
    Graph: START → explain_node → END
    """

    def build_graph(self) -> CompiledStateGraph:
        try:
            logger.info("Building ExplainAgent graph...")
            llm = LLMFactory.create()
            system_message = PromptFactory.get_explain_prompt()
            explain_node_bound = partial(
                explain_node,
                llm=llm,
                system_message=system_message,
            )

            graph = StateGraph(ExplainAgentState)
            graph.add_node("explain_node", explain_node_bound)
            graph.add_edge(START, "explain_node")
            graph.add_edge("explain_node", END)

            compiled = graph.compile()
            logger.info("ExplainAgent graph built successfully")
            return compiled
        except Exception as e:
            logger.error("Failed to build ExplainAgent graph: %s", e)
            raise

    def run(self, text: str, book_name: str = "", **kwargs) -> str:
        logger.info("ExplainAgent.run() | text length=%d chars", len(text))
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            final_state = self._graph.invoke(initial_state)
            result = final_state["result"]
            logger.info("ExplainAgent completed | result length=%d chars", len(result))
            return result
        except Exception as e:
            logger.error("ExplainAgent.run() failed: %s", e)
            raise

    async def arun(self, text: str, book_name: str = "", **kwargs) -> str:
        logger.info("ExplainAgent.arun() | text length=%d chars", len(text))
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            final_state = await self._graph.ainvoke(initial_state)
            result = final_state["result"]
            logger.info("ExplainAgent.arun() completed | result length=%d chars", len(result))
            return result
        except Exception as e:
            logger.error("ExplainAgent.arun() failed: %s", e)
            raise

    async def astream(self, text: str, book_name: str = "", **kwargs):
        logger.info("ExplainAgent.astream() | text length=%d chars", len(text))
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "messages": [],
                "result": "",
            }
            # Lặp qua các event để stream từng token của LLM
            async for event in self._graph.astream_events(initial_state, version="v2"):
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
            logger.error("ExplainAgent.astream() failed: %s", e)
            raise
