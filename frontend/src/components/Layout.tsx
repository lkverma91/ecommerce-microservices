import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="sticky top-0 z-10 border-b bg-white shadow-sm">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to="/" className="text-xl font-bold text-primary-600">
            E-commerce
          </Link>
          <nav className="flex items-center gap-4">
            <Link to="/products" className="text-gray-700 hover:text-primary-600">
              Products
            </Link>
            {isAuthenticated ? (
              <>
                <Link to="/cart" className="relative text-gray-700 hover:text-primary-600">
                  Cart
                  {itemCount > 0 && (
                    <span className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-primary-500 text-xs text-white">
                      {itemCount}
                    </span>
                  )}
                </Link>
                <Link to="/orders" className="text-gray-700 hover:text-primary-600">
                  Orders
                </Link>
                <Link to="/admin/products" className="text-gray-700 hover:text-primary-600">
                  Admin
                </Link>
                <span className="text-sm text-gray-600">{user?.name}</span>
                <button onClick={handleLogout} className="rounded bg-gray-200 px-3 py-1 text-sm hover:bg-gray-300">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-gray-700 hover:text-primary-600">
                  Login
                </Link>
                <Link
                  to="/register"
                  className="rounded bg-primary-500 px-3 py-1.5 text-sm text-white hover:bg-primary-600"
                >
                  Register
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</main>
    </div>
  );
};
