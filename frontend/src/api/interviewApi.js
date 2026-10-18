import axiosClient from './axiosClient';

export const interviewApi = {
  getHome: () => axiosClient.get('/student/interviews/home'),
  start: (subjectId) => axiosClient.post(`/student/interviews/start?subjectId=${subjectId}`),
  getTurn: (sessionId) => axiosClient.get(`/student/interviews/${sessionId}`),
  submit: (sessionId, turnId, transcript) => axiosClient.post(`/student/interviews/${sessionId}/submit`, { turnId, transcript }),
  abort: (sessionId) => axiosClient.post(`/student/interviews/${sessionId}/abort`),
};
