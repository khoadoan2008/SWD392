import axiosClient from './axiosClient';

export const configApi = {
  getAll: () => axiosClient.get('/admin/configs'),
  update: (data) => axiosClient.put('/admin/configs', data),
};
