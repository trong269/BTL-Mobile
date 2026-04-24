from functools import partial

from langgraph.graph import StateGraph, START, END
from langgraph.graph.state import CompiledStateGraph
from src.core.agents.base import BaseBookAgent
from src.core.agents.components.states import SuggestionAgentState
from src.core.agents.components.nodes import suggestion_node
from src.core.llm.factory import LLMFactory
from src.core.prompts.factory import PromptFactory
from src.utils.logging import get_logger
from src.utils.tracing import tracer

logger = get_logger(__name__)


class SuggestionsAgent(BaseBookAgent):
    """
    Agent sinh câu hỏi gợi ý theo chương đang đọc.
    Graph: START -> suggestion_node -> END
    """

    def build_graph(self) -> CompiledStateGraph:
        try:
            logger.info("Building SuggestionsAgent graph...")
            llm = LLMFactory.create()
            system_message = PromptFactory.get_suggestions_prompt()
            suggestion_node_bound = partial(
                suggestion_node,
                llm=llm,
                system_message=system_message,
            )

            graph = StateGraph(SuggestionAgentState)
            graph.add_node("suggestion_node", suggestion_node_bound)
            graph.add_edge(START, "suggestion_node")
            graph.add_edge("suggestion_node", END)
            compiled = graph.compile()

            logger.info("SuggestionsAgent graph built successfully")
            return compiled
        except Exception as e:
            logger.error("Failed to build SuggestionsAgent graph: %s", e)
            raise

    def run(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="SuggestionsAgent.run")
            config = {"callbacks": [handler], "run_name": "SuggestionsAgent.run"} if handler else {}
            final_state = self._graph.invoke(initial_state, config=config)
            return final_state["result"]
        except Exception as e:
            logger.error("SuggestionsAgent.run() failed: %s", e)
            raise

    async def arun(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        **kwargs,
    ) -> str:
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="SuggestionsAgent.arun")
            config = {"callbacks": [handler], "run_name": "SuggestionsAgent.arun"} if handler else {}
            final_state = await self._graph.ainvoke(initial_state, config=config)
            return final_state["result"]
        except Exception as e:
            logger.error("SuggestionsAgent.arun() failed: %s", e)
            raise

    async def astream(
        self,
        text: str,
        book_name: str = "",
        current_chapter_title: str = "",
        **kwargs,
    ):
        try:
            initial_state = {
                "text": text,
                "book_name": book_name,
                "current_chapter_title": current_chapter_title,
                "messages": [],
                "result": "",
            }
            handler = tracer.get_handler(trace_name="SuggestionsAgent.astream")
            config = {"callbacks": [handler], "run_name": "SuggestionsAgent.astream"} if handler else {}

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
            logger.error("SuggestionsAgent.astream() failed: %s", e)
            raise
