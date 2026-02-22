import { useState, useEffect } from 'react';
import { fetchProducts, createProduct, updateProduct, deleteProduct, Product, ProductCreate } from '../api/productApi';
import { ErrorDisplay } from '../components/ErrorBoundary';

export function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Product | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<ProductCreate>({ name: '', description: '', price: 0, category: '' });

  const load = async () => {
    try {
      setError('');
      const data = await fetchProducts();
      setProducts(data);
    } catch (err: unknown) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      if (editing) {
        await updateProduct(editing.id, form);
        setEditing(null);
      } else {
        await createProduct(form);
      }
      setForm({ name: '', description: '', price: 0, category: '' });
      setShowForm(false);
      await load();
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data;
      setError(data?.message || 'Operation failed.');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this product?')) return;
    try {
      await deleteProduct(id);
      await load();
    } catch (err: unknown) {
      setError((err as Error).message);
    }
  };

  const startEdit = (p: Product) => {
    setEditing(p);
    setForm({ name: p.name, description: p.description, price: p.price, category: p.category });
    setShowForm(true);
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between">
        <h1 className="text-2xl font-bold">Admin - Products</h1>
        <button
          onClick={() => { setShowForm(true); setEditing(null); setForm({ name: '', description: '', price: 0, category: '' }); }}
          className="rounded bg-primary-500 px-4 py-2 text-white hover:bg-primary-600"
        >
          Add Product
        </button>
      </div>
      {error && <ErrorDisplay error={error} onRetry={() => setError('')} />}

      {showForm && (
        <form onSubmit={handleSubmit} className="mt-6 rounded border bg-white p-6">
          <h3 className="font-semibold">{editing ? 'Edit Product' : 'New Product'}</h3>
          <div className="mt-4 space-y-4">
            <div>
              <label className="block text-sm font-medium">Name</label>
              <input
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                required
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Price</label>
              <input
                type="number"
                step="0.01"
                value={form.price || ''}
                onChange={(e) => setForm((f) => ({ ...f, price: parseFloat(e.target.value) || 0 }))}
                required
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Category</label>
              <input
                value={form.category}
                onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
                required
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <button type="submit" className="rounded bg-primary-500 px-4 py-2 text-white hover:bg-primary-600">
              {editing ? 'Update' : 'Create'}
            </button>
            <button type="button" onClick={() => { setShowForm(false); setEditing(null); }} className="rounded border px-4 py-2">
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="mt-6 overflow-x-auto rounded border">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">Name</th>
              <th className="px-4 py-2 text-left">Category</th>
              <th className="px-4 py-2 text-left">Price</th>
              <th className="px-4 py-2 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id} className="border-t">
                <td className="px-4 py-2">{p.id}</td>
                <td className="px-4 py-2">{p.name}</td>
                <td className="px-4 py-2">{p.category}</td>
                <td className="px-4 py-2">${p.price.toFixed(2)}</td>
                <td className="px-4 py-2 text-right">
                  <button onClick={() => startEdit(p)} className="text-primary-600 hover:underline">Edit</button>
                  {' | '}
                  <button onClick={() => handleDelete(p.id)} className="text-red-600 hover:underline">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
