import axiosClient from './axiosClient';

export const sessionApi = {
  search: (params) => axiosClient.get('/lecturer/sessions', { params }),
  getStats: () => axiosClient.get('/lecturer/sessions/stats'),
  getDetail: (id) => axiosClient.get(`/lecturer/sessions/${id}`),
  delete: (id) => axiosClient.delete(`/lecturer/sessions/${id}`),
};
