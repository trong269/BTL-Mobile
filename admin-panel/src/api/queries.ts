import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createBook, deleteBook, getBooks, updateBook, type BookPayload } from './booksApi';
import { createCategory, deleteCategory, getCategories, updateCategory, type CategoryPayload } from './categoriesApi';
import { deleteUser, getUsers, updateUser, type UserPayload } from './usersApi';
import { addReview, getBookReviews, type ReviewPayload } from './reviewsApi';

export const queryKeys = {
  books: ['books'] as const,
  users: ['users'] as const,
  categories: ['categories'] as const,
  reviews: (bookId: string) => ['reviews', bookId] as const,
};

export function useBooks() {
  return useQuery({ queryKey: queryKeys.books, queryFn: getBooks });
}

export function useUsers() {
  return useQuery({ queryKey: queryKeys.users, queryFn: getUsers });
}

export function useCategories() {
  return useQuery({ queryKey: queryKeys.categories, queryFn: getCategories });
}

export function useBookReviews(bookId?: string) {
  return useQuery({
    queryKey: queryKeys.reviews(bookId || ''),
    queryFn: () => getBookReviews(bookId as string),
    enabled: Boolean(bookId),
  });
}

export function useCreateBook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: BookPayload) => createBook(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useUpdateBook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: BookPayload }) => updateBook(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories });
    },
  });
}

export function useDeleteBook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteBook(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UserPayload }) => updateUser(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryPayload) => createCategory(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories });
    },
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CategoryPayload }) => updateCategory(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories });
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories });
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useAddReview() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ payload, bookId }: { payload: ReviewPayload; bookId: string }) => addReview(payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reviews(variables.bookId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}
