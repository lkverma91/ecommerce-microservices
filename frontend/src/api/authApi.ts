import { axiosInstance } from './axiosInstance';

export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string;
  active: boolean;
  roles?: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  phone?: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// Register: POST /api/auth/register (returns token + user, no separate login needed)
export const register = async (data: RegisterRequest): Promise<AuthResponse> => {
  const res = await axiosInstance.post<AuthResponse>('/auth/register', data);
  return res.data;
};

// Login: POST /api/auth/login
export const login = async (data: LoginRequest): Promise<AuthResponse> => {
  const res = await axiosInstance.post<AuthResponse>('/auth/login', data);
  return res.data;
};

// Current user from JWT (e.g. after OAuth callback). Requires Authorization header.
export const getMe = async (): Promise<User> => {
  const res = await axiosInstance.get<User>('/auth/me');
  return res.data;
};

/** Base URL for OAuth2 authorization (redirect to backend). */
export const getOAuthLoginUrl = (provider: string): string => {
  const base = import.meta.env.VITE_API_BASE_URL || '/api';
  return `${base}/auth/oauth2/authorization/${provider}`;
};
