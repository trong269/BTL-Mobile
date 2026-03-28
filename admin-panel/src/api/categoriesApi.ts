import { axiosClient } from './axiosClient';
import type { PageResponse, PaginatedQuery } from './types';

export interface CategoryDto {
  id: number;
  title: string;
  description: string;
  iconName: string;
}

export async function getCategories(params: PaginatedQuery = {}) {
  const response = await axiosClient.get<PageResponse<CategoryDto>>('/categories', { params });
  return response.data;
}
