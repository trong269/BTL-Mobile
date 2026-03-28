from abc import ABC, abstractmethod
from typing import AsyncGenerator
from langgraph.graph.state import CompiledStateGraph


class BaseBookAgent(ABC):
    """
    Class cơ sở (Base) cho tất cả các AI Agents trong hệ thống.
    """

    def __init__(self):
        self._graph: CompiledStateGraph = self.build_graph()

    def build_graph(self) -> CompiledStateGraph:
        """Xây dựng và compile LangGraph StateGraph."""
        ...

    @abstractmethod
    def run(self, text: str, **kwargs) -> str:
        """Thực thi query (chế độ đồng bộ)."""
        ...

    @abstractmethod
    async def arun(self, text: str, **kwargs) -> str:
        """Thực thi query (chế độ bất đồng bộ)."""
        ...

    @abstractmethod
    async def astream(self, text: str, **kwargs) -> AsyncGenerator[str, None]:
        """Thực thi query và trả về token từng phần (streaming)."""
        ...
