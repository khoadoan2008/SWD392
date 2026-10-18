import axiosClient from './axiosClient';

export const rubricApi = {
  getByQuestion: (questionId) => axiosClient.get(`/lecturer/rubrics/question/${questionId}`),
  create: (questionId, data) => axiosClient.post(`/lecturer/rubrics/question/${questionId}`, data),
  update: (id, questionId, data) => axiosClient.put(`/lecturer/rubrics/${id}?questionId=${questionId}`, data),
  delete: (id) => axiosClient.delete(`/lecturer/rubrics/${id}`),
};
