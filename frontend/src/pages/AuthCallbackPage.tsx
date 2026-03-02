import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMe } from '../api/authApi';
import { ErrorDisplay } from '../components/ErrorBoundary';

/**
 * Handles redirect after OAuth2 login. Reads ?token= or ?error= from URL,
 * stores token and fetches user, then redirects to home.
 */
export function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const err = searchParams.get('error');
    if (err) {
      setError(err === 'missing_attributes' ? 'Could not get email from provider.' : err);
      return;
    }
    if (!token) {
      setError('No token received.');
      return;
    }
    localStorage.setItem('token', token);
    getMe()
      .then((user) => {
        setAuth(token, user);
        navigate('/', { replace: true });
      })
      .catch(() => {
        setError('Failed to load user.');
      });
  }, [searchParams, navigate, setAuth]);

  if (error) {
    return (
      <div className="mx-auto max-w-md text-center">
        <ErrorDisplay error={error} />
        <a href="/login" className="mt-4 inline-block text-primary-600 hover:underline">
          Back to login
        </a>
      </div>
    );
  }
  return (
    <div className="mx-auto max-w-md text-center">
      <p className="text-gray-600">Completing sign in...</p>
    </div>
  );
}
