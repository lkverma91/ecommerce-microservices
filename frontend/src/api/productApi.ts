import { axiosInstance } from './axiosInstance';

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  active: boolean;
  createdAt?: string;
}

export interface ProductCreate {
  name: string;
  description: string;
  price: number;
  category: string;
}

export const fetchProducts = async (category?: string): Promise<Product[]> => {
  const params = category ? { category } : {};
  const res = await axiosInstance.get<Product[]>('/products', { params });
  return res.data;
};

export const fetchProduct = async (id: string): Promise<Product> => {
  const res = await axiosInstance.get<Product>(`/products/${id}`);
  return res.data;
};

export const createProduct = async (data: ProductCreate): Promise<Product> => {
  const res = await axiosInstance.post<Product>('/products', data);
  return res.data;
};

export const updateProduct = async (id: number, data: Partial<ProductCreate>): Promise<Product> => {
  const res = await axiosInstance.put<Product>(`/products/${id}`, data);
  return res.data;
};

export const deleteProduct = async (id: number): Promise<void> => {
  await axiosInstance.delete(`/products/${id}`);
};
