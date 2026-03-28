import { axiosClient } from './axiosClient';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: string;
}

export async function login(payload: LoginRequest) {
  const response = await axiosClient.post<UserProfile>('/auth/login', payload);
  return response.data;
}

export async function logout() {
  await axiosClient.post('/auth/logout');
}

export async function getProfile() {
  const response = await axiosClient.get<UserProfile>('/auth/me');
  return response.data;
}
