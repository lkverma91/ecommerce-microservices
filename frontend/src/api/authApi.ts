import { axiosInstance } from './axiosInstance';

export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string;
  active: boolean;
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

// Register: POST /api/users
export const register = async (data: RegisterRequest): Promise<User> => {
  const res = await axiosInstance.post('/users', data);
  return res.data;
};

// Login: POST /api/auth/login (backend to add) or dev fallback
export const login = async (data: LoginRequest): Promise<AuthResponse> => {
  try {
    const res = await axiosInstance.post<AuthResponse>('/auth/login', data);
    return res.data;
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status;
    // Dev fallback: /auth/login not implemented - validate user exists
    if (import.meta.env.DEV && (status === 404 || status === 501)) {
      const res = await axiosInstance.get<User>(`/users/email/${encodeURIComponent(data.email)}`);
      return { token: `dev-${res.data.id}-${Date.now()}`, user: res.data };
    }
    throw err;
  }
};
