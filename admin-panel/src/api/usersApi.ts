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
  username?: string;
  name: string;
  email: string;
  role: string;
  plan: string;
  lastActive?: string;
  avatar?: string;
}

export interface UserPayload {
  username: string;
  name: string;
  email: string;
  password?: string;
  role: string;
  plan: string;
  avatar?: string;
}

export interface CreateUserPayload {
  username: string;
  name: string;
  email: string;
  password: string;
  role: string;
  plan: string;
  avatar?: string;
}

function mapUser(user: BackendUser): UserDto {
  return {
    id: user.id,
    username: user.username,
    name: user.fullName || user.username,
    email: user.email,
    role: (user.role || 'USER').toUpperCase(),
    plan: user.plan || 'Cơ bản',
    lastActive: user.updatedAt ? new Date(user.updatedAt).toLocaleString('vi-VN') : 'Chưa cập nhật',
    avatar: user.avatar || 'https://cdn.jsdelivr.net/gh/alohe/avatars/png/memo_35.png',
  };
}

function toBackendPayload(payload: UserPayload) {
  return {
    username: payload.username,
    fullName: payload.name,
    email: payload.email,
    password: payload.password || '',
    role: payload.role,
    plan: payload.plan,
    avatar: payload.avatar || '',
  };
}

function toBackendCreatePayload(payload: CreateUserPayload) {
  return {
    username: payload.username,
    fullName: payload.name,
    email: payload.email,
    password: payload.password,
    role: payload.role,
    plan: payload.plan,
    avatar: payload.avatar || '',
  };
}

export async function getUsers() {
  const response = await axiosClient.get<BackendUser[]>('/users');
  return response.data.map(mapUser);
}

export async function createUser(payload: CreateUserPayload) {
  const response = await axiosClient.post<BackendUser>('/users', toBackendCreatePayload(payload));
  return mapUser(response.data);
}

export async function updateUser(id: string, payload: UserPayload) {
  const response = await axiosClient.put<BackendUser>(`/users/${id}`, toBackendPayload(payload));
  return mapUser(response.data);
}

export async function deleteUser(id: string) {
  await axiosClient.delete(`/users/${id}`);
}
