import axiosClient from './axiosClient';

export const labApi = {
  listVms: () => axiosClient.get('/labs/vms').then((res) => res.data),
  getVm: (id) => axiosClient.get(`/labs/vms/${id}`).then((res) => res.data),
  getLogs: (id) => axiosClient.get(`/labs/vms/${id}/logs`).then((res) => res.data),
  startVm: (id) => axiosClient.post(`/labs/vms/${id}/start`).then((res) => res.data),
  stopVm: (id) => axiosClient.post(`/labs/vms/${id}/stop`).then((res) => res.data),
};
