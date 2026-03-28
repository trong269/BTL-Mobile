import { useQuery } from '@tanstack/react-query';
import { getBooks } from './booksApi';
import { getUsers } from './usersApi';
import { getCategories } from './categoriesApi';
import type { PaginatedQuery } from './types';

export const queryKeys = {
  books: (params: PaginatedQuery) => ['books', params] as const,
  users: (params: PaginatedQuery) => ['users', params] as const,
  categories: (params: PaginatedQuery) => ['categories', params] as const,
};

export function useBooks(params: PaginatedQuery) {
  return useQuery({
    queryKey: queryKeys.books(params),
    queryFn: () => getBooks(params),
  });
}

export function useUsers(params: PaginatedQuery) {
  return useQuery({
    queryKey: queryKeys.users(params),
    queryFn: () => getUsers(params),
  });
}

export function useCategories(params: PaginatedQuery) {
  return useQuery({
    queryKey: queryKeys.categories(params),
    queryFn: () => getCategories(params),
  });
}
