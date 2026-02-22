import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { fetchOrdersByUser, Order } from '../api/orderApi';
import { ErrorDisplay } from '../components/ErrorBoundary';

export function OrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    const load = async () => {
      try {
        const data = await fetchOrdersByUser(user.id);
        if (!cancelled) setOrders(data);
      } catch (err: unknown) {
        if (!cancelled) setError((err as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [user?.id]);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (error) return <ErrorDisplay error={error} onRetry={() => window.location.reload()} />;

  return (
    <div>
      <h1 className="text-2xl font-bold">My Orders</h1>
      <div className="mt-6 space-y-4">
        {orders.map((o) => (
          <div key={o.id} className="rounded border bg-white p-6">
            <div className="flex justify-between">
              <span className="font-medium">Order #{o.id}</span>
              <span className={`rounded px-2 py-0.5 text-sm ${
                o.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' : 'bg-gray-100'
              }`}>
                {o.status}
              </span>
            </div>
            <p className="mt-2 text-sm text-gray-600">
              {new Date(o.createdAt).toLocaleString()}
            </p>
            <ul className="mt-4 space-y-1">
              {o.items.map((i, idx) => (
                <li key={idx}>
                  Product {i.productId} x {i.quantity} — ${i.subtotal.toFixed(2)}
                </li>
              ))}
            </ul>
            <p className="mt-4 font-bold">Total: ${o.totalAmount.toFixed(2)}</p>
          </div>
        ))}
      </div>
      {orders.length === 0 && (
        <p className="py-12 text-center text-gray-500">No orders yet.</p>
      )}
    </div>
  );
}
