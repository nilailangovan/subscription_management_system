import axios from 'axios';

/**
 * Global Axios instance for LoopPay API requests.
 * Points to Spring Boot backend at http://localhost:8080.
 */
const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request Interceptor:
 * Automatically extracts JWT token from localStorage and attaches it
 * as Authorization: Bearer <token> for all outgoing requests.
 */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('looppay_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response Interceptor:
 * If a 401 Unauthorized occurs, clean up token and redirect to login
 * if not already on an auth page.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token expired or invalid
      const currentPath = window.location.pathname;
      if (!currentPath.startsWith('/login') && !currentPath.startsWith('/signup')) {
        localStorage.removeItem('looppay_token');
        localStorage.removeItem('looppay_user');
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

