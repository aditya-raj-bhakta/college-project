import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../api/client';
import AuthLayout from './AuthLayout';

const DEMO_ACCOUNTS = [
  { label: 'Admin', email: 'admin@library.com', password: 'admin123' },
  { label: 'Librarian', email: 'librarian@library.com', password: 'librarian123' },
  { label: 'Student', email: 'student@library.com', password: 'student123' },
  { label: 'Faculty', email: 'faculty@library.com', password: 'faculty123' },
  { label: 'Blocked student', email: 'priya@library.com', password: 'priya123' },
];

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(params.get('expired') ? 'Your session expired. Please log in again.' : '');
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login(email, password);
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Log in" subtitle="Borrow books and keep track of your loans">
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <div className="alert alert-error">{error}</div>}
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
        </label>
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? 'Logging in…' : 'Log in'}
        </button>
      </form>

      <p className="auth-switch">
        New here? <Link to="/register">Create a member account</Link>
      </p>

      <div className="demo-box">
        <span className="demo-title">Demo accounts</span>
        {DEMO_ACCOUNTS.map((a) => (
          <button
            key={a.email}
            type="button"
            className="demo-account"
            onClick={() => { setEmail(a.email); setPassword(a.password); }}
          >
            <strong>{a.label}</strong>
            <code>{a.email}</code>
          </button>
        ))}
      </div>
    </AuthLayout>
  );
}
