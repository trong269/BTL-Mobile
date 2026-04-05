import { axiosClient } from './axiosClient';

interface BackendCategory {
  id: string;
  name: string;
  description?: string;
}

export interface CategoryDto {
  id: string;
  title: string;
  description: string;
  iconName: string;
}

export interface CategoryPayload {
  title: string;
  description: string;
}

const iconNames = ['Heart', 'Sun', 'Coffee', 'Map'] as const;

function mapCategory(category: BackendCategory, index = 0): CategoryDto {
  return {
    id: category.id,
    title: category.name,
    description: category.description || '',
    iconName: iconNames[index % iconNames.length],
  };
}

export async function getCategories() {
  const response = await axiosClient.get<BackendCategory[]>('/categories');
  return response.data.map((category, index) => mapCategory(category, index));
}

export async function createCategory(payload: CategoryPayload) {
  const response = await axiosClient.post<BackendCategory>('/categories', {
    name: payload.title,
    description: payload.description,
  });
  return mapCategory(response.data);
}

export async function updateCategory(id: string, payload: CategoryPayload) {
  const response = await axiosClient.put<BackendCategory>(`/categories/${id}`, {
    name: payload.title,
    description: payload.description,
  });
  return mapCategory(response.data);
}

export async function deleteCategory(id: string) {
  await axiosClient.delete(`/categories/${id}`);
}
