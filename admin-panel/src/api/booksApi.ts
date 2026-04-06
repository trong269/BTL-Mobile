import { normalizeRichText } from '../lib/utils';
import { axiosClient } from './axiosClient';

interface BackendBookCategory {
  id: string;
  name: string;
  description?: string;
}

interface BackendBook {
  id: string;
  sourceBookId?: string;
  title: string;
  author: string;
  description?: string;
  coverImage?: string;
  publisher?: string;
  publishDate?: string;
  status?: string;
  categoryId?: string;
  categories?: string[];
  categoryObjects?: BackendBookCategory[];
  tags?: string[];
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  avgRating?: number;
  featured?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BookCategoryObject {
  id: string;
  name: string;
  description?: string;
}

export interface BookDto {
  id: string;
  title: string;
  author: string;
  status: string;
  cover: string;
  description: string;
  publisher: string;
  publishDate: string;
  categories: string[];
  categoryIds: string[];
  categoryObjects: BookCategoryObject[];
  rating?: number;
  avgRating?: number;
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  featured?: boolean;
  sourceBookId?: string;
  categoryId?: string;
  tags?: string[];
}

export interface BookPayload {
  title: string;
  author: string;
  status: string;
  cover: string;
  description: string;
  publisher: string;
  publishDate: string;
  categoryIds: string[];
  sourceBookId?: string;
  categoryId?: string;
  tags?: string[];
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  avgRating?: number;
  featured?: boolean;
}

function getFormattedDescription(book: BackendBook): string {
  return normalizeRichText(book.description);
}

function toCategoryObjects(book: BackendBook): BookCategoryObject[] {
  return (book.categoryObjects || []).map((category) => ({
    id: category.id,
    name: category.name,
    description: category.description,
  }));
}

function toCategoryNames(book: BackendBook, categoryObjects: BookCategoryObject[]): string[] {
  if (categoryObjects.length > 0) {
    return categoryObjects.map((category) => category.name);
  }
  return book.categories || [];
}

function toCategoryIds(book: BackendBook, categoryObjects: BookCategoryObject[]): string[] {
  if (categoryObjects.length > 0) {
    return categoryObjects.map((category) => category.id);
  }

  const fallbackIds = [book.categoryId, ...(book.categories || [])]
    .filter((value): value is string => Boolean(value && value.trim()))
    .map((value) => value.trim());

  return Array.from(new Set(fallbackIds));
}

function toPrimaryCategoryId(book: BackendBook, categoryIds: string[]): string | undefined {
  const explicit = book.categoryId?.trim();
  return explicit || categoryIds[0];
}

function mapBook(book: BackendBook): BookDto {
  const categoryObjects = toCategoryObjects(book);
  const categories = toCategoryNames(book, categoryObjects);
  const categoryIds = toCategoryIds(book, categoryObjects);
  const categoryId = toPrimaryCategoryId(book, categoryIds);

  return {
    id: book.id,
    title: book.title,
    author: book.author,
    status: book.status || 'Sẵn sàng',
    cover: book.coverImage || 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400',
    description: getFormattedDescription(book),
    publisher: book.publisher || '',
    publishDate: book.publishDate || '',
    categories,
    categoryIds,
    categoryObjects,
    rating: book.avgRating,
    avgRating: book.avgRating,
    totalChapters: book.totalChapters,
    totalPages: book.totalPages,
    views: book.views,
    featured: book.featured,
    sourceBookId: book.sourceBookId,
    categoryId,
    tags: book.tags || [],
  };
}

function normalizeWriteCategoryIds(payload: BookPayload): string[] {
  const merged = [payload.categoryId, ...(payload.categoryIds || [])]
    .filter((value): value is string => Boolean(value && value.trim()))
    .map((value) => value.trim());

  return Array.from(new Set(merged));
}

function toBackendPayload(payload: BookPayload): Partial<BackendBook> {
  const categoryIds = normalizeWriteCategoryIds(payload);

  return {
    title: payload.title,
    author: payload.author,
    status: payload.status,
    coverImage: payload.cover,
    description: payload.description,
    publisher: payload.publisher,
    publishDate: payload.publishDate,
    sourceBookId: payload.sourceBookId,
    categoryId: categoryIds[0],
    categories: categoryIds,
    tags: payload.tags || [],
    totalChapters: payload.totalChapters ?? 0,
    totalPages: payload.totalPages ?? 0,
    views: payload.views ?? 0,
    avgRating: payload.avgRating ?? 0,
    featured: payload.featured ?? false,
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
