import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import BooksPage from './pages/BooksPage';
import BookDetailPage from './pages/BookDetailPage';
import MyLoansPage from './pages/MyLoansPage';
import MyHoldsPage from './pages/MyHoldsPage';
import MyFinesPage from './pages/MyFinesPage';
import DeskPage from './pages/DeskPage';
import LoansPage from './pages/LoansPage';
import HoldsPage from './pages/HoldsPage';
import FinesPage from './pages/FinesPage';
import MembersPage from './pages/MembersPage';
import MemberDetailPage from './pages/MemberDetailPage';
import ReportsPage from './pages/ReportsPage';
import RulesPage from './pages/admin/RulesPage';
import StaffPage from './pages/admin/StaffPage';
import AuditPage from './pages/admin/AuditPage';
import NotFoundPage from './pages/NotFoundPage';

const staff = (page) => <ProtectedRoute staff>{page}</ProtectedRoute>;
const admin = (page) => <ProtectedRoute admin>{page}</ProtectedRoute>;

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/books" element={<BooksPage />} />

        {/* Members */}
        <Route path="/my-loans" element={<MyLoansPage />} />
        <Route path="/my-holds" element={<MyHoldsPage />} />
        <Route path="/my-fines" element={<MyFinesPage />} />

        {/* Librarians and admins */}
        <Route path="/desk" element={staff(<DeskPage />)} />
        <Route path="/loans" element={staff(<LoansPage />)} />
        <Route path="/holds" element={staff(<HoldsPage />)} />
        <Route path="/fines" element={staff(<FinesPage />)} />
        <Route path="/books/:id" element={staff(<BookDetailPage />)} />
        <Route path="/members" element={staff(<MembersPage />)} />
        <Route path="/members/:id" element={staff(<MemberDetailPage />)} />
        <Route path="/reports" element={staff(<ReportsPage />)} />

        {/* Admins */}
        <Route path="/admin/rules" element={admin(<RulesPage />)} />
        <Route path="/admin/staff" element={admin(<StaffPage />)} />
        <Route path="/admin/audit" element={admin(<AuditPage />)} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
