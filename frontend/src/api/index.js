import http from './http'

/** 认证（§15.2） */
export const authApi = {
  me: () => http.get('/auth/me', { __skipAuthHandler: true }),
  login: (payload) => http.post('/auth/login', payload),
  register: (payload) => http.post('/auth/register', payload),
  logout: () => http.post('/auth/logout')
}

/** 公开接口（§15.1） */
export const publicApi = {
  jobs: (params) => http.get('/public/jobs', { params }),
  job: (id) => http.get(`/public/jobs/${id}`),
  categories: () => http.get('/public/job-categories'),
  locations: () => http.get('/public/job-locations'),
  jobOptions: () => http.get('/public/job-options')
}

/** 求职者接口（§15.3） */
export const candidateApi = {
  profile: () => http.get('/candidate/profile'),
  saveProfile: (payload) => http.put('/candidate/profile', payload),
  resume: () => http.get('/candidate/resume'),
  saveResume: (payload) => http.put('/candidate/resume', payload),
  currentFile: () => http.get('/candidate/resume/file'),
  deleteFile: () => http.delete('/candidate/resume/file'),
  uploadFile: (file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/candidate/resume/file', form)
  },
  eligibility: (jobId) => http.get(`/candidate/jobs/${jobId}/apply-eligibility`),
  apply: (jobId) => http.post(`/candidate/jobs/${jobId}/apply`),
  applications: (params) => http.get('/candidate/applications', { params }),
  application: (id) => http.get(`/candidate/applications/${id}`),
  withdraw: (id, payload) => http.post(`/candidate/applications/${id}/withdraw`, payload || {}),
  downloadCurrentUrl: () => '/api/candidate/resume/file/download',
  downloadSnapshotUrl: (id) => `/api/candidate/applications/${id}/resume/download`
}

/** HR 接口（§15.4） */
export const hrApi = {
  dashboard: (scope) => http.get('/hr/dashboard', { params: { scope } }),
  jobs: (params) => http.get('/hr/jobs', { params }),
  job: (id) => http.get(`/hr/jobs/${id}`),
  createJob: (payload) => http.post('/hr/jobs', payload),
  updateJob: (id, payload) => http.put(`/hr/jobs/${id}`, payload),
  updateJobStatus: (id, payload) => http.put(`/hr/jobs/${id}/status`, payload),
  transferOwner: (id, payload) => http.put(`/hr/jobs/${id}/owner`, payload),
  applications: (params) => http.get('/hr/applications', { params }),
  application: (id) => http.get(`/hr/applications/${id}`),
  logs: (id) => http.get(`/hr/applications/${id}/logs`),
  history: (id) => http.get(`/hr/applications/${id}/history`),
  changeStatus: (id, payload) => http.put(`/hr/applications/${id}/status`, payload),
  saveNote: (id, payload) => http.put(`/hr/applications/${id}/note`, payload),
  saveInterview: (id, payload) => http.put(`/hr/applications/${id}/interview`, payload),
  hrOptions: () => http.get('/hr/hr-users/options'),
  resumeDownloadUrl: (id) => `/api/hr/applications/${id}/resume/download`
}

/** 管理员接口（§15.5） */
export const adminApi = {
  categories: () => http.get('/admin/job-categories'),
  createCategory: (payload) => http.post('/admin/job-categories', payload),
  updateCategory: (id, payload) => http.put(`/admin/job-categories/${id}`, payload),
  updateCategoryStatus: (id, status) =>
    http.put(`/admin/job-categories/${id}/status`, null, { params: { status } }),
  locations: () => http.get('/admin/job-locations'),
  createLocation: (payload) => http.post('/admin/job-locations', payload),
  updateLocation: (id, payload) => http.put(`/admin/job-locations/${id}`, payload),
  updateLocationStatus: (id, status) =>
    http.put(`/admin/job-locations/${id}/status`, null, { params: { status } }),
  hrUsers: (params) => http.get('/admin/hr-users', { params }),
  createHrUser: (payload) => http.post('/admin/hr-users', payload),
  updateHrUser: (id, payload) => http.put(`/admin/hr-users/${id}`, payload),
  updateHrUserStatus: (id, status) =>
    http.put(`/admin/hr-users/${id}/status`, null, { params: { status } }),
  resetHrPassword: (id) => http.post(`/admin/hr-users/${id}/reset-password`),
  resetCandidatePassword: (email) => http.post('/admin/candidates/reset-password', { email })
}
