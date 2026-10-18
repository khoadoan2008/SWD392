import axiosClient from './axiosClient';

export const subjectApi = {
  getAll: () => axiosClient.get('/admin/subjects'),
  getById: (id) => axiosClient.get(`/admin/subjects/${id}`),
  create: (data) => axiosClient.post('/admin/subjects', data),
  update: (id, data) => axiosClient.put(`/admin/subjects/${id}`, data),
  delete: (id) => axiosClient.delete(`/admin/subjects/${id}`),
};
