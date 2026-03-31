from typing import TypedDict, Annotated
from langchain_core.messages import BaseMessage
import operator


class ExplainAgentState(TypedDict):
    text: str
    book_name: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str


class SummarizeAgentState(TypedDict):
    text: str
    book_name: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str

class VisionAgentState(TypedDict):
    question: str
    image_url: str
    book_name: str
    messages: Annotated[list[BaseMessage], operator.add]
    result: str