import api from './client';

const data = (r) => r.data;

export const authApi = {
  login: (body) => api.post('/auth/login', body).then(data),
  register: (body) => api.post('/auth/register', body).then(data),
  me: () => api.get('/me').then(data),
};

/** The logged-in member's own account. */
export const meApi = {
  dashboard: () => api.get('/me/dashboard').then(data),
  loans: () => api.get('/me/loans').then(data),
  borrow: (bookId) => api.post('/me/loans', { bookId }).then(data),
  renew: (loanId) => api.post(`/me/loans/${loanId}/renew`).then(data),
  holds: () => api.get('/me/holds').then(data),
  placeHold: (bookId) => api.post('/me/holds', { bookId }).then(data),
  cancelHold: (holdId) => api.delete(`/me/holds/${holdId}`).then(data),
  fines: () => api.get('/me/fines').then(data),
  notifications: () => api.get('/me/notifications').then(data),
};

export const booksApi = {
  search: (params) => api.get('/books', { params }).then(data),
  get: (id) => api.get(`/books/${id}`).then(data),
  categories: () => api.get('/books/categories').then(data),
  create: (body) => api.post('/books', body).then(data),
  update: (id, body) => api.put(`/books/${id}`, body).then(data),
  remove: (id) => api.delete(`/books/${id}`),
  copies: (id) => api.get(`/books/${id}/copies`).then(data),
  addCopies: (id, body) => api.post(`/books/${id}/copies`, body).then(data),
  queue: (id) => api.get(`/books/${id}/holds`).then(data),
};

export const copiesApi = {
  byBarcode: (barcode) => api.get(`/copies/barcode/${encodeURIComponent(barcode)}`).then(data),
  update: (id, body) => api.patch(`/copies/${id}`, body).then(data),
};

/** The circulation desk. */
export const deskApi = {
  patron: (card) => api.get(`/desk/patrons/${encodeURIComponent(card)}`).then(data),
  checkout: (cardNumber, barcode) => api.post('/desk/checkout', { cardNumber, barcode }).then(data),
  checkin: (barcode) => api.post('/desk/checkin', { barcode }).then(data),
};

export const loansApi = {
  list: (status = 'all') => api.get('/loans', { params: { status } }).then(data),
  checkin: (id) => api.post(`/loans/${id}/checkin`).then(data),
  renew: (id) => api.post(`/loans/${id}/renew`).then(data),
  markLost: (id) => api.post(`/loans/${id}/lost`).then(data),
};

export const holdsApi = {
  list: (status = 'active') => api.get('/holds', { params: { status } }).then(data),
  cancel: (id) => api.delete(`/holds/${id}`).then(data),
};

export const finesApi = {
  list: (status = 'open') => api.get('/fines', { params: { status } }).then(data),
  pay: (id, amount, note) => api.post(`/fines/${id}/payments`, { amount, note }).then(data),
  waive: (id, amount, reason) => api.post(`/fines/${id}/waivers`, { amount, reason }).then(data),
};

export const membersApi = {
  search: (params) => api.get('/members', { params }).then(data),
  types: () => api.get('/members/types').then(data),
  get: (id) => api.get(`/members/${id}`).then(data),
  changeType: (id, memberTypeId) => api.put(`/members/${id}/type`, { memberTypeId }).then(data),
  suspend: (id, reason) => api.post(`/members/${id}/suspend`, { reason }).then(data),
  reactivate: (id) => api.post(`/members/${id}/reactivate`).then(data),
};

export const dashboardApi = {
  staff: () => api.get('/dashboard/staff').then(data),
};

export const reportsApi = {
  summary: (from, to) => api.get('/reports/summary', { params: { from, to } }).then(data),
  /** Fetches a CSV with the auth header and hands it to the browser as a download. */
  download: async (path, params, filename) => {
    const res = await api.get(`/reports/${path}`, { params, responseType: 'blob' });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};

export const adminApi = {
  settings: () => api.get('/admin/settings').then(data),
  updateSettings: (body) => api.put('/admin/settings', body).then(data),
  updateMemberType: (id, body) => api.put(`/admin/member-types/${id}`, body).then(data),
  staff: () => api.get('/admin/staff').then(data),
  createStaff: (body) => api.post('/admin/staff', body).then(data),
  audit: (params) => api.get('/admin/audit', { params }).then(data),
  runDailyJobs: () => api.post('/admin/jobs/daily').then(data),
};
