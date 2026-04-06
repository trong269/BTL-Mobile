import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function decodeHtmlToText(value: string) {
  const htmlWithLineBreaks = value
    .replace(/<\s*br\s*\/?>/gi, "\n")
    .replace(/<\s*\/p\s*>/gi, "\n\n")
    .replace(/<\s*p[^>]*>/gi, "")

  if (typeof DOMParser === "undefined") {
    return htmlWithLineBreaks.replace(/<[^>]+>/g, "")
  }

  return new DOMParser().parseFromString(htmlWithLineBreaks, "text/html").body.textContent || ""
}

export function normalizeRichText(value?: string) {
  if (!value) return ""

  return decodeHtmlToText(value)
    .replace(/\r\n/g, "\n")
    .replace(/\\n/g, "\n")
    .replace(/\u00A0/g, " ")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim()
}
