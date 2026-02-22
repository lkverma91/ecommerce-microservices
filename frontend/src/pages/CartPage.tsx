import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

export function CartPage() {
  const { items, removeItem, updateQuantity, total } = useCart();

  if (items.length === 0) {
    return (
      <div className="py-12 text-center">
        <p className="text-lg text-gray-600">Your cart is empty.</p>
        <Link to="/products" className="mt-4 inline-block text-primary-600 hover:underline">
          Browse products
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">Shopping Cart</h1>
      <div className="mt-6 space-y-4">
        {items.map((item) => (
          <div
            key={item.productId}
            className="flex items-center justify-between rounded border bg-white p-4"
          >
            <div>
              <Link to={`/products/${item.productId}`} className="font-medium hover:text-primary-600">
                {item.name}
              </Link>
              <p className="text-sm text-gray-600">${item.price.toFixed(2)} each</p>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={1}
                value={item.quantity}
                onChange={(e) => updateQuantity(item.productId, Math.max(1, parseInt(e.target.value, 10) || 1))}
                className="w-16 rounded border px-2 py-1 text-center"
              />
              <span className="w-24 text-right font-medium">${(item.price * item.quantity).toFixed(2)}</span>
              <button
                onClick={() => removeItem(item.productId)}
                className="text-red-600 hover:text-red-800"
              >
                Remove
              </button>
            </div>
          </div>
        ))}
      </div>
      <div className="mt-8 flex justify-between border-t pt-6">
        <p className="text-xl font-bold">Total: ${total.toFixed(2)}</p>
        <Link
          to="/checkout"
          className="rounded bg-primary-500 px-6 py-2 text-white hover:bg-primary-600"
        >
          Proceed to Checkout
        </Link>
      </div>
    </div>
  );
}
