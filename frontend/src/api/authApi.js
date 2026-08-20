import axiosClient from './axiosClient';

export const authApi = {
  register: (payload) => axiosClient.post('/auth/register', payload).then((res) => res.data),
  login: (payload) => axiosClient.post('/auth/login', payload).then((res) => res.data),
  forgotPassword: (email) => axiosClient.post('/auth/forgot-password', { email }).then((res) => res.data),
  resetPassword: (payload) => axiosClient.post('/auth/reset-password', payload).then((res) => res.data),
};
