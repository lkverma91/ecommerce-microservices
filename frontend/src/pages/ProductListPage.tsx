import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchProducts, Product } from '../api/productApi';
import { ErrorDisplay } from '../components/ErrorBoundary';

export function ProductListPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [category, setCategory] = useState('');

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        setError('');
        const data = await fetchProducts(category || undefined);
        if (!cancelled) setProducts(data);
      } catch (err: unknown) {
        if (!cancelled) setError((err as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    setLoading(true);
    load();
    return () => { cancelled = true; };
  }, [category]);

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
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Products</h1>
        <input
          type="text"
          placeholder="Filter by category"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="rounded border border-gray-300 px-3 py-2"
        />
      </div>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {products.map((p) => (
          <Link
            key={p.id}
            to={`/products/${p.id}`}
            className="overflow-hidden rounded-lg border bg-white shadow transition hover:shadow-md"
          >
            <div className="h-48 bg-gray-100" />
            <div className="p-4">
              <h3 className="font-semibold">{p.name}</h3>
              <p className="text-sm text-gray-600">{p.category}</p>
              <p className="mt-2 text-lg font-bold text-primary-600">${p.price.toFixed(2)}</p>
            </div>
          </Link>
        ))}
      </div>
      {products.length === 0 && (
        <p className="py-12 text-center text-gray-500">No products found.</p>
      )}
    </div>
  );
}
