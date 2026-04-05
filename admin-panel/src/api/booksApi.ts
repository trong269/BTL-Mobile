import { axiosClient } from './axiosClient';
import type { PageResponse, PaginatedQuery } from './types';

export interface BookDto {
  id: number;
  title: string;
  author: string;
  status: string;
  cover: string;
  summary: string;
  publisher: string;
  publishDate: string;
  categories: string[];
  rating?: number;
  color?: string;
}

export async function getBooks(params: PaginatedQuery = {}) {
  const response = await axiosClient.get<PageResponse<BookDto>>('/books', { params });
  return response.data;
}

export async function createBook(payload: Partial<BookDto>) {
  const response = await axiosClient.post<BookDto>('/books', payload);
  return response.data;
}

export async function updateBook(id: number, payload: Partial<BookDto>) {
  const response = await axiosClient.put<BookDto>(`/books/${id}`, payload);
  return response.data;
}

export async function deleteBook(id: number) {
  const response = await axiosClient.delete<void>(`/books/${id}`);
  return response.data;
}
