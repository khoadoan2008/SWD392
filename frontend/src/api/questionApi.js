import axiosClient from './axiosClient';

export const questionApi = {
  downloadImportTemplate: () => axiosClient.get('/lecturer/questions/import/template', { responseType: 'blob' }),
  importExcel: (file, { confirm = false, status = 'PENDING_REVIEW', previewHash, onUploadProgress } = {}) => {
    const form = new FormData();
    form.append('file', file);
    return axiosClient.post('/lecturer/questions/import', form, {
      params: { confirm, status, ...(previewHash ? { previewHash } : {}) },
      headers: { 'Content-Type': undefined },
      onUploadProgress,
      timeout: 60000,
    });
  },
  search: (params) => axiosClient.get('/lecturer/questions', { params }),
  getStats: () => axiosClient.get('/lecturer/questions/stats'),
  getById: (id) => axiosClient.get(`/lecturer/questions/${id}`),
  create: (data) => axiosClient.post('/lecturer/questions', data),
  update: (id, data) => axiosClient.put(`/lecturer/questions/${id}`, data),
  updateStatus: (id, to) => axiosClient.patch(`/lecturer/questions/${id}/status?to=${to}`),
  delete: (id) => axiosClient.delete(`/lecturer/questions/${id}`),
  generate: (data) => axiosClient.post('/lecturer/questions/generate', data),
  getGenerationSubjects: () => axiosClient.get('/lecturer/questions/generation-subjects'),
  generateFromMaterial: (form, requestId) => axiosClient.post('/lecturer/questions/generate', form, {
    headers: { 'Content-Type': undefined, 'Idempotency-Key': requestId },
    timeout: 120000,
  }),
};
