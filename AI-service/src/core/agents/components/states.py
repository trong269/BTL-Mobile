from typing import TypedDict, Annotated
from langchain_core.messages import BaseMessage
import operator


class QAChatMessage(TypedDict):
    role: str
    content: str


class ExplainAgentState(TypedDict):
    text: str
    book_name: str
    context_before: str
    context_after: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str


class SummarizeAgentState(TypedDict):
    text: str
    book_name: str
    context_before: str
    context_after: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str


class QAAgentState(TypedDict):
    text: str
    book_name: str
    current_chapter_title: str
    context_chunks: list[str]
    chat_history: list[QAChatMessage]
    messages: Annotated[list[BaseMessage], operator.add]
    result: str


class SuggestionAgentState(TypedDict):
    text: str
    book_name: str
    current_chapter_title: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str
