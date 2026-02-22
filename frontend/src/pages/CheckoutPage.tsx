import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { createOrder } from '../api/orderApi';
import { ErrorDisplay } from '../components/ErrorBoundary';

export function CheckoutPage() {
  const { user } = useAuth();
  const { items, total, clearCart } = useCart();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  if (!user || items.length === 0) {
    navigate('/cart');
    return null;
  }

  const handlePlaceOrder = async () => {
    setError('');
    setLoading(true);
    try {
      await createOrder({
        userId: user.id,
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
      });
      clearCart();
      navigate('/orders');
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data;
      setError(data?.message || 'Failed to place order.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold">Checkout</h1>
      {error && <ErrorDisplay error={error} onRetry={() => setError('')} />}
      <div className="mt-6 rounded border bg-white p-6">
        <p className="font-medium">Order for {user.name}</p>
        <p className="text-sm text-gray-600">{user.email}</p>
        <ul className="mt-4 space-y-2">
          {items.map((i) => (
            <li key={i.productId} className="flex justify-between">
              <span>{i.name} x {i.quantity}</span>
              <span>${(i.price * i.quantity).toFixed(2)}</span>
            </li>
          ))}
        </ul>
        <p className="mt-4 border-t pt-4 text-xl font-bold">Total: ${total.toFixed(2)}</p>
        <button
          onClick={handlePlaceOrder}
          disabled={loading}
          className="mt-6 w-full rounded bg-primary-500 py-2 text-white hover:bg-primary-600 disabled:opacity-50"
        >
          {loading ? 'Placing order...' : 'Place Order'}
        </button>
      </div>
    </div>
  );
}
