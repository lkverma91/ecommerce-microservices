import { Link } from 'react-router-dom';

export function HomePage() {
  return (
    <div className="py-12">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900">Welcome to E-commerce Store</h1>
        <p className="mt-2 text-lg text-gray-600">Shop the best products at great prices</p>
        <div className="mt-8 flex justify-center gap-4">
          <Link
            to="/products"
            className="rounded bg-primary-500 px-6 py-3 text-white hover:bg-primary-600"
          >
            Browse Products
          </Link>
          <Link to="/register" className="rounded border border-primary-500 px-6 py-3 text-primary-600 hover:bg-primary-50">
            Get Started
          </Link>
        </div>
      </div>
    </div>
  );
}
