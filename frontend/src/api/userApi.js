import axiosClient from './axiosClient';

export const userApi = {
  search: (params) => axiosClient.get('/admin/users', { params }),
  getStats: () => axiosClient.get('/admin/users/stats'),
  getById: (id) => axiosClient.get(`/admin/users/${id}`),
  create: (data) => axiosClient.post('/admin/users', data),
  update: (id, data) => axiosClient.put(`/admin/users/${id}`, data),
  delete: (id) => axiosClient.delete(`/admin/users/${id}`),
};
