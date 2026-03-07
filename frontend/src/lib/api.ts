import axios from 'axios';
import { supabase } from './supabase';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://127.0.0.1:5000/api';

const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use(async (config) => {
  if (typeof window !== 'undefined') {
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (session?.access_token) {
      config.headers.Authorization = `Bearer ${session.access_token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      // Try refreshing the session once before giving up
      const {
        data: { session },
      } = await supabase.auth.refreshSession();
      if (!session) {
        // Session is truly gone — redirect to login without destroying storage
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export { API_URL };
export default api;
