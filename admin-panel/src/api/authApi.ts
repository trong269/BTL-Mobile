import { axiosClient } from './axiosClient';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: string;
  avatar?: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    username: string;
    email: string;
    fullName?: string;
    avatar?: string;
    role: string;
  };
  role: string;
}

export async function login(payload: LoginRequest) {
  const response = await axiosClient.post<LoginResponse>('/auth/login', payload);
  const { token, user } = response.data;

  localStorage.setItem('token', token);

  return {
    id: user.id,
    name: user.fullName || user.username,
    email: user.email,
    role: user.role,
    avatar: user.avatar,
  } as UserProfile;
}

export async function logout() {
  localStorage.removeItem('token');
  await axiosClient.post('/auth/logout');
}

export async function getProfile() {
  const response = await axiosClient.get<UserProfile>('/auth/me');
  return response.data;
}
