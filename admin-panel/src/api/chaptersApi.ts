import { axiosClient } from './axiosClient';

interface BackendChapter {
  id: string;
  bookId: string;
  chapterNumber: number;
  title: string;
  content: string;
}

export interface ChapterDto {
  id: string;
  bookId: string;
  chapterNumber: number;
  title: string;
  content: string;
}

export interface ChapterPayload {
  chapterNumber: number;
  title: string;
  content: string;
}

function mapChapter(chapter: BackendChapter): ChapterDto {
  return {
    id: chapter.id,
    bookId: chapter.bookId,
    chapterNumber: chapter.chapterNumber,
    title: chapter.title,
    content: chapter.content || '',
  };
}

export async function getBookChapters(bookId: string) {
  const response = await axiosClient.get<BackendChapter[]>(`/books/${bookId}/chapters`);
  return response.data.map(mapChapter);
}

export async function createChapter(bookId: string, payload: ChapterPayload) {
  const response = await axiosClient.post<BackendChapter>(`/books/${bookId}/chapters`, payload);
  return mapChapter(response.data);
}

export async function updateChapter(bookId: string, chapterId: string, payload: ChapterPayload) {
  const response = await axiosClient.put<BackendChapter>(`/books/${bookId}/chapters/${chapterId}`, payload);
  return mapChapter(response.data);
}

export async function deleteChapter(bookId: string, chapterId: string) {
  await axiosClient.delete(`/books/${bookId}/chapters/${chapterId}`);
}
