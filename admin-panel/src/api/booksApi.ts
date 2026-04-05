import { axiosClient } from './axiosClient';

interface BackendBook {
  id: string;
  title: string;
  author: string;
  description?: string;
  summary?: string;
  coverImage?: string;
  publisher?: string;
  publishDate?: string;
  status?: string;
  categoryId?: string;
  categories?: string[];
  tags?: string[];
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  avgRating?: number;
  featured?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BookDto {
  id: string;
  title: string;
  author: string;
  status: string;
  cover: string;
  summary: string;
  publisher: string;
  publishDate: string;
  categories: string[];
  rating?: number;
  totalChapters?: number;
}

export interface BookPayload {
  title: string;
  author: string;
  status: string;
  cover: string;
  summary: string;
  publisher: string;
  publishDate: string;
  categories: string[];
}

function mapBook(book: BackendBook): BookDto {
  return {
    id: book.id,
    title: book.title,
    author: book.author,
    status: book.status || 'Sẵn sàng',
    cover: book.coverImage || 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400',
    summary: book.summary || book.description || '',
    publisher: book.publisher || '',
    publishDate: book.publishDate || '',
    categories: book.categories || [],
    rating: book.avgRating,
    totalChapters: book.totalChapters,
  };
}

function toBackendPayload(payload: BookPayload): Partial<BackendBook> {
  return {
    title: payload.title,
    author: payload.author,
    status: payload.status,
    coverImage: payload.cover,
    summary: payload.summary,
    description: payload.summary,
    publisher: payload.publisher,
    publishDate: payload.publishDate,
    categories: payload.categories,
  };
}

export async function getBooks() {
  const response = await axiosClient.get<BackendBook[]>('/books');
  return response.data.map(mapBook);
}

export async function createBook(payload: BookPayload) {
  const response = await axiosClient.post<BackendBook>('/books', toBackendPayload(payload));
  return mapBook(response.data);
}

export async function updateBook(id: string, payload: BookPayload) {
  const response = await axiosClient.put<BackendBook>(`/books/${id}`, toBackendPayload(payload));
  return mapBook(response.data);
}

export async function deleteBook(id: string) {
  await axiosClient.delete(`/books/${id}`);
}
