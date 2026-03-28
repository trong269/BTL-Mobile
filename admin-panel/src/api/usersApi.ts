import { axiosClient } from './axiosClient';
import type { PageResponse, PaginatedQuery } from './types';

export interface UserDto {
  id: number;
  name: string;
  email: string;
  role: string;
  plan?: string;
  lastActive?: string;
  avatar?: string;
}

export async function getUsers(params: PaginatedQuery = {}) {
  const response = await axiosClient.get<PageResponse<UserDto>>('/users', { params });
  return response.data;
}
