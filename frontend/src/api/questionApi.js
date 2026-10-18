import axiosClient from './axiosClient';

export const questionApi = {
  search: (params) => axiosClient.get('/lecturer/questions', { params }),
  getStats: () => axiosClient.get('/lecturer/questions/stats'),
  getById: (id) => axiosClient.get(`/lecturer/questions/${id}`),
  create: (data) => axiosClient.post('/lecturer/questions', data),
  update: (id, data) => axiosClient.put(`/lecturer/questions/${id}`, data),
  updateStatus: (id, to) => axiosClient.patch(`/lecturer/questions/${id}/status?to=${to}`),
  delete: (id) => axiosClient.delete(`/lecturer/questions/${id}`),
  generate: (data) => axiosClient.post('/lecturer/questions/generate', data),
};
