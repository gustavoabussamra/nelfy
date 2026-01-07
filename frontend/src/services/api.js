import axios from 'axios';
import { toast } from 'react-toastify';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  timeout: 30000, // 30 segundos de timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para adicionar o token em todas as requisições
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para tratar erros de resposta
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Erro de autenticação
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // Erro de timeout
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      console.error('Timeout na requisição:', error.config?.url);
      toast.error('Tempo de espera esgotado. Verifique sua conexão.');
      return Promise.reject(error);
    }

    // Erro de rede (sem conexão)
    if (!error.response) {
      if (error.message === 'Network Error' || error.code === 'ERR_NETWORK') {
        console.error('Erro de rede:', error);
        toast.error('Erro de conexão. Verifique se o servidor está rodando.');
        return Promise.reject(error);
      }
    }

    // Erro de CORS
    if (error.message?.includes('CORS') || error.message?.includes('cors')) {
      console.error('Erro de CORS:', error);
      toast.error('Erro de CORS. Verifique a configuração do servidor.');
      return Promise.reject(error);
    }

    // Outros erros
    return Promise.reject(error);
  }
);

export default api;

