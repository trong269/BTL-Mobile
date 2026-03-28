export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
}

export interface PaginatedQuery {
  page?: number;
  size?: number;
  sort?: string;
}
