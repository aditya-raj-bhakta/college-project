import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage, getFieldErrors } from '../api/client';
import AuthLayout from './AuthLayout';

export default function RegisterPage() {
  const { user, register } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setErrors({});
    if (form.password !== form.confirm) {
      setErrors({ confirm: 'Passwords do not match' });
      return;
    }
    setLoading(true);
    try {
      const u = await register(form.name, form.email, form.password);
      toast(`Welcome to the library, ${u.name.split(' ')[0]}`);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setErrors(getFieldErrors(err));
      setError(getErrorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  const input = (name, label, type = 'text', autoComplete) => (
    <label className="field">
      <span>{label}</span>
      <input
        type={type}
        value={form[name]}
        onChange={update(name)}
        required
        autoComplete={autoComplete}
        className={errors[name] ? 'invalid' : ''}
      />
      {errors[name] && <small className="field-error">{errors[name]}</small>}
    </label>
  );

  return (
    <AuthLayout title="Join the library" subtitle="Create a free member account">
      <form onSubmit={handleSubmit} className="auth-form" noValidate>
        {error && <div className="alert alert-error">{error}</div>}
        {input('name', 'Full name', 'text', 'name')}
        {input('email', 'Email', 'email', 'email')}
        {input('password', 'Password (min. 6 characters)', 'password', 'new-password')}
        {input('confirm', 'Confirm password', 'password', 'new-password')}
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="auth-switch">
        Already a member? <Link to="/login">Log in</Link>
      </p>
    </AuthLayout>
  );
}
