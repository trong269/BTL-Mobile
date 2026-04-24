import axios from 'axios';
import { toast } from 'sonner';

const aiBaseURL = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000';

export const aiAxiosClient = axios.create({
  baseURL: aiBaseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false, // AI service không cần credentials
});

aiAxiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

aiAxiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;

    if (status === 401) {
      toast.error('Không có quyền truy cập AI service.');
    }
    if (status === 403) {
      toast.error('Bạn không có quyền thực hiện thao tác này.');
    }
    if (status === 500) {
      toast.error('Lỗi AI service. Vui lòng thử lại sau.');
    }
    return Promise.reject(error);
  },
);
