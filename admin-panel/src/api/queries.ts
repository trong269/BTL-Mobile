import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createBook, deleteBook, getBooks, updateBook, type BookPayload } from './booksApi';
import { createCategory, deleteCategory, getCategories, updateCategory, type CategoryPayload } from './categoriesApi';
import { createChapter, deleteChapter, getBookChapters, updateChapter, type ChapterPayload } from './chaptersApi';
import { createUser, deleteUser, getUsers, updateUser, type CreateUserPayload, type UserPayload } from './usersApi';
import { addReview, getBookReviews, type ReviewPayload } from './reviewsApi';
import { aiConfigApi, type LLMConfig, type LLMConfigUpdate, type APIKeysUpdate, type AgentInfo, type AILog, type AIStats, type LogQueryParams, type FileLogEntry } from './aiConfigApi';

export const queryKeys = {
  books: ['books'] as const,
  users: ['users'] as const,
  categories: ['categories'] as const,
  chapters: (bookId: string) => ['chapters', bookId] as const,
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

export function useBookChapters(bookId?: string) {
  return useQuery({
    queryKey: queryKeys.chapters(bookId || ''),
    queryFn: () => getBookChapters(bookId as string),
    enabled: Boolean(bookId),
  });
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
    onSuccess: (_, bookId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories });
      queryClient.removeQueries({ queryKey: queryKeys.chapters(bookId) });
      queryClient.removeQueries({ queryKey: queryKeys.reviews(bookId) });
    },
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateUserPayload) => createUser(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users });
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

export function useCreateChapter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, payload }: { bookId: string; payload: ChapterPayload }) => createChapter(bookId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.chapters(variables.bookId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useUpdateChapter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, chapterId, payload }: { bookId: string; chapterId: string; payload: ChapterPayload }) =>
      updateChapter(bookId, chapterId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.chapters(variables.bookId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}

export function useDeleteChapter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, chapterId }: { bookId: string; chapterId: string }) => deleteChapter(bookId, chapterId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.chapters(variables.bookId) });
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

// ─── AI Config Hooks ──────────────────────────────────────────────────────────

export const aiConfigKeys = {
  all: ['ai-config'] as const,
  llmConfig: () => [...aiConfigKeys.all, 'llm'] as const,
  agents: () => [...aiConfigKeys.all, 'agents'] as const,
  logs: (params?: LogQueryParams) => [...aiConfigKeys.all, 'logs', params] as const,
  stats: () => [...aiConfigKeys.all, 'stats'] as const,
};

export const useAIConfig = () => {
  return useQuery<LLMConfig>({
    queryKey: aiConfigKeys.llmConfig(),
    queryFn: aiConfigApi.getLLMConfig,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const useUpdateAIConfig = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (config: LLMConfigUpdate) => aiConfigApi.updateLLMConfig(config),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: aiConfigKeys.llmConfig() });
    },
  });
};

export const useUpdateAPIKeys = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keys: APIKeysUpdate) => aiConfigApi.updateAPIKeys(keys),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: aiConfigKeys.llmConfig() });
    },
  });
};

export const useAgents = () => {
  return useQuery<AgentInfo[]>({
    queryKey: aiConfigKeys.agents(),
    queryFn: aiConfigApi.getAgents,
    refetchInterval: 30 * 1000, // Refetch every 30 seconds
  });
};

export const useLogs = (params?: LogQueryParams) => {
  return useQuery<AILog[]>({
    queryKey: aiConfigKeys.logs(params),
    queryFn: () => aiConfigApi.getLogs(params),
    refetchInterval: 10 * 1000, // Refetch every 10 seconds
  });
};

export const useFileLogs = (limit?: number, level?: string) => {
  return useQuery<FileLogEntry[]>({
    queryKey: [...aiConfigKeys.all, 'file-logs', limit, level] as const,
    queryFn: () => aiConfigApi.getFileLogs(limit, level),
    refetchInterval: 5 * 1000, // Refetch every 5 seconds for real-time logs
  });
};

export const useStats = () => {
  return useQuery<AIStats>({
    queryKey: aiConfigKeys.stats(),
    queryFn: aiConfigApi.getStats,
    refetchInterval: 30 * 1000, // Refetch every 30 seconds
  });
};

export const useDeleteLogs = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => aiConfigApi.deleteLogs(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: aiConfigKeys.logs() });
      queryClient.invalidateQueries({ queryKey: [...aiConfigKeys.all, 'file-logs'] });
    },
  });
};
