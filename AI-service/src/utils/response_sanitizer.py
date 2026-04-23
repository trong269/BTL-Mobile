def sanitize_reader_ai_output(text: str, mode: str) -> str:
    raw = (text or "").strip()
    if not raw:
        return (
            "Không đủ thông tin để giải nghĩa đoạn văn bản này."
            if mode == "explain"
            else "Đoạn văn bản thiếu ngữ cảnh để tóm tắt."
        )

    # We now fully support rich Markdown in the Android client using Markwon.
    # Therefore, we no longer artificially strip headings, bold, or italic formatting,
    # nor do we force bullet points. We simply normalize line endings.
    normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
    
    return normalized.strip()
