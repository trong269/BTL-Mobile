import axios from 'axios';
import { toast } from 'sonner';

const baseURL = import.meta.env.VITE_API_BASE_URL;

export const axiosClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    if (status === 403) {
      toast.error('Bạn không có quyền thực hiện thao tác này.');
    }
    return Promise.reject(error);
  },
);
