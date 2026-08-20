import axios from 'axios';

/**
 * Client HTTP centralisé. Toute nouvelle couche d'API module (authApi,
 * labApi, futurs modules) réutilise cette instance : le token est injecté
 * automatiquement, et une éventuelle réponse 401 peut être interceptée ici
 * une seule fois pour toute l'application.
 */
const axiosClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('lab_platform_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('lab_platform_token');
      localStorage.removeItem('lab_platform_user');
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
