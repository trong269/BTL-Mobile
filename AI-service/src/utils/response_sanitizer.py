import re


def _clean_line(line: str) -> str:
    cleaned = line.replace("**", "").strip()
    cleaned = re.sub(r"^[#>*\-\s]+", "", cleaned).strip()
    cleaned = re.sub(r"\s+", " ", cleaned)
    return cleaned


def _dedupe_preserve_order(lines: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for line in lines:
        key = line.casefold()
        if key in seen:
            continue
        seen.add(key)
        result.append(line)
    return result


def sanitize_reader_ai_output(text: str, mode: str) -> str:
    raw = (text or "").strip()
    if not raw:
        return (
            "Không đủ thông tin để giải nghĩa đoạn văn bản này."
            if mode == "explain"
            else "Đoạn văn bản thiếu ngữ cảnh để tóm tắt."
        )

    normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
    parts = [_clean_line(part) for part in normalized.splitlines()]
    parts = [part for part in parts if part]
    parts = _dedupe_preserve_order(parts)

    if not parts:
        fallback = _clean_line(normalized)
        return (
            fallback
            if fallback
            else (
                "Không đủ thông tin để giải nghĩa đoạn văn bản này."
                if mode == "explain"
                else "Đoạn văn bản thiếu ngữ cảnh để tóm tắt."
            )
        )

    if mode == "summarize":
        trimmed = parts[:3]
        return "\n".join(f"- {part}" for part in trimmed)

    if len(parts) == 1:
        return parts[0]

    trimmed = parts[:2]
    return "\n".join(f"- {part}" for part in trimmed)
