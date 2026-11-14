import axios from 'axios';

export const TOKEN_KEY = 'library_token';
export const USER_KEY = 'library_user';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the token has expired, log out and go to the login page
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const isAuthCall = error.config?.url?.startsWith('/auth/');
    if (status === 401 && !isAuthCall && localStorage.getItem(TOKEN_KEY)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.location.href = '/login?expired=1';
    }
    return Promise.reject(error);
  }
);

/** Turns any API error into a message that can be shown to the user. */
export function getErrorMessage(error, fallback = 'Something went wrong') {
  if (!error.response) {
    return 'Cannot reach the server. Is the backend running on port 8080?';
  }
  return error.response.data?.message || fallback;
}

/** Field-level validation errors from the backend, e.g. { email: "Enter a valid email" } */
export function getFieldErrors(error) {
  return error.response?.data?.fieldErrors || {};
}

export default api;
