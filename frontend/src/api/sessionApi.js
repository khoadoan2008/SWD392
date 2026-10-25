import axiosClient from './axiosClient';

export const sessionApi = {
  search: (params) => axiosClient.get('/lecturer/sessions', { params }),
  getStats: () => axiosClient.get('/lecturer/sessions/stats'),
  getDetail: (id) => axiosClient.get(`/lecturer/sessions/${id}`),
  delete: (id) => axiosClient.delete(`/lecturer/sessions/${id}`),
  grade: (id, data) => axiosClient.post(`/lecturer/sessions/${id}/grade`, data),
  reEvaluate: (id) => axiosClient.post(`/lecturer/sessions/${id}/re-evaluate`),
};
