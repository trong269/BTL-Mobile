import { axiosClient } from './axiosClient';

interface BackendUser {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  avatar?: string;
  role?: string;
  plan?: string;
  updatedAt?: string;
}

export interface UserDto {
  id: string;
  name: string;
  email: string;
  role: string;
  plan: string;
  lastActive?: string;
  avatar?: string;
}

export interface UserPayload {
  name: string;
  email: string;
  role: string;
  plan: string;
  avatar?: string;
}

function mapUser(user: BackendUser): UserDto {
  return {
    id: user.id,
    name: user.fullName || user.username,
    email: user.email,
    role: (user.role || 'USER').toLowerCase(),
    plan: user.plan || 'Cơ bản',
    lastActive: user.updatedAt ? new Date(user.updatedAt).toLocaleString('vi-VN') : 'Chưa cập nhật',
    avatar: user.avatar || 'https://i.pravatar.cc/150?img=32',
  };
}

function toBackendPayload(payload: UserPayload) {
  return {
    username: payload.email.split('@')[0],
    fullName: payload.name,
    email: payload.email,
    role: payload.role.toUpperCase(),
    plan: payload.plan,
    avatar: payload.avatar || '',
  };
}

export async function getUsers() {
  const response = await axiosClient.get<BackendUser[]>('/users');
  return response.data.map(mapUser);
}

export async function updateUser(id: string, payload: UserPayload) {
  const response = await axiosClient.put<BackendUser>(`/users/${id}`, toBackendPayload(payload));
  return mapUser(response.data);
}

export async function deleteUser(id: string) {
  await axiosClient.delete(`/users/${id}`);
}
