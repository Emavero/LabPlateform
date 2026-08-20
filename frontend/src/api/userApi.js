import axiosClient from './axiosClient';

export const userApi = {
  getProfile: () => axiosClient.get('/users/me').then((res) => res.data),
  changePassword: (payload) => axiosClient.put('/users/me/password', payload).then((res) => res.data),
};
