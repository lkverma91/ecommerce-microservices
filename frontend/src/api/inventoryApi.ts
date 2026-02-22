import { axiosInstance } from './axiosInstance';

export const checkStock = async (productId: number, quantity: number): Promise<boolean> => {
  const res = await axiosInstance.get<boolean>('/inventory/check', {
    params: { productId, quantity },
  });
  return res.data;
};
