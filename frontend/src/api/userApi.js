import axiosClient from './axiosClient';

export const userApi = {
  search: (params) => axiosClient.get('/admin/users', { params }),
  getStats: () => axiosClient.get('/admin/users/stats'),
  getById: (id) => axiosClient.get(`/admin/users/${id}`),
  create: (data) => axiosClient.post('/admin/users', data),
  update: (id, data) => axiosClient.put(`/admin/users/${id}`, data),
  delete: (id) => axiosClient.delete(`/admin/users/${id}`),
  downloadImportTemplate: () =>
    axiosClient.get('/admin/users/import-template', {
      responseType: 'blob',
    }),
  importUsers: (file, subjectId = null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (subjectId) {
      formData.append('subjectId', subjectId);
    }
    return axiosClient.post('/admin/users/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};
