import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchProduct, Product } from '../api/productApi';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { ErrorDisplay } from '../components/ErrorBoundary';

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [added, setAdded] = useState(false);
  const { isAuthenticated } = useAuth();
  const { addItem } = useCart();
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    const load = async () => {
      try {
        const data = await fetchProduct(id);
        if (!cancelled) setProduct(data);
      } catch (err: unknown) {
        if (!cancelled) setError((err as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [id]);

  const handleAddToCart = () => {
    if (!product) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/products/${id}` } } });
      return;
    }
    addItem(product.id, product.name, product.price, quantity);
    setAdded(true);
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (error || !product) return <ErrorDisplay error={error || 'Product not found'} />;

  return (
    <div className="flex flex-col gap-8 lg:flex-row">
      <div className="h-80 flex-1 rounded-lg bg-gray-200 lg:h-96" />
      <div className="flex-1">
        <h1 className="text-3xl font-bold">{product.name}</h1>
        <p className="mt-2 text-gray-600">{product.description}</p>
        <p className="mt-4 text-2xl font-bold text-primary-600">${product.price.toFixed(2)}</p>
        <p className="text-sm text-gray-500">{product.category}</p>
        <div className="mt-6 flex items-center gap-4">
          <label className="text-sm font-medium">Quantity</label>
          <input
            type="number"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value, 10) || 1))}
            className="w-20 rounded border px-2 py-1"
          />
          <button
            onClick={handleAddToCart}
            className="rounded bg-primary-500 px-6 py-2 text-white hover:bg-primary-600"
          >
            {added ? 'Added!' : 'Add to Cart'}
          </button>
        </div>
      </div>
    </div>
  );
}
