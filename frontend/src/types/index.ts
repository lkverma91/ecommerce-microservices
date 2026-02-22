export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string;
  active: boolean;
}

export interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
}
