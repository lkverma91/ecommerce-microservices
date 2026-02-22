import { axiosInstance } from './axiosInstance';

export interface OrderItem {
  productId: number;
  quantity: number;
}

export interface CreateOrderRequest {
  userId: number;
  items: OrderItem[];
}

export interface OrderItemResponse {
  productId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: number;
  userId: number;
  status: string;
  totalAmount: number;
  items: OrderItemResponse[];
  createdAt: string;
}

export const createOrder = async (data: CreateOrderRequest): Promise<Order> => {
  const res = await axiosInstance.post<Order>('/orders', data);
  return res.data;
};

export const fetchOrdersByUser = async (userId: number): Promise<Order[]> => {
  const res = await axiosInstance.get<Order[]>(`/orders/user/${userId}`);
  return res.data;
};
