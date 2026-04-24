import { axiosClient } from './axiosClient';

export interface SendNotificationRequest {
  title: string;
  body: string;
  userIds?: string[];
  sendToAll: boolean;
}

export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  avatar?: string;
  role: string;
  plan: string;
}

export const notificationsApi = {
  sendNotification: async (data: SendNotificationRequest) => {
    const response = await axiosClient.post('/admin/notifications/send', data);
    return response.data;
  },

  getAllUsers: async (): Promise<User[]> => {
    const response = await axiosClient.get('/users');
    return response.data;
  },

  deleteAllAdminNotifications: async () => {
    const response = await axiosClient.delete('/admin/notifications');
    return response.data;
  },
};
