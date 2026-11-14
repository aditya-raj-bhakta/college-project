import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Spinner from './Spinner';

/**
 * Redirects to /login when logged out, and to /dashboard when the role doesn't allow the page.
 * The server enforces the same rules; this only keeps people off pages they can't use.
 */
export default function ProtectedRoute({ children, staff = false, admin = false }) {
  const { user, isAdmin, isStaff, checking } = useAuth();
  const location = useLocation();

  if (checking) {
    return <div className="page-center"><Spinner label="Checking your session…" /></div>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if ((admin && !isAdmin) || (staff && !isStaff)) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}
