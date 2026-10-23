import axiosClient from './axiosClient';

export const rubricApi = {
  getBySubject: (subjectId) => axiosClient.get(`/lecturer/rubrics/subject/${subjectId}`),
  create: (subjectId, data) => axiosClient.post(`/lecturer/rubrics/subject/${subjectId}`, data),
  update: (id, subjectId, data) => axiosClient.put(`/lecturer/rubrics/${id}?subjectId=${subjectId}`, data),
  delete: (id) => axiosClient.delete(`/lecturer/rubrics/${id}`),
};
