import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Logo from './Logo';
import Icon from './Icon';

const MEMBER_NAV = [
  {
    links: [
      { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
      { to: '/books', label: 'Browse books', icon: 'library' },
      { to: '/my-loans', label: 'My loans', icon: 'bookOpen' },
      { to: '/my-holds', label: 'Holds', icon: 'clock' },
      { to: '/my-fines', label: 'Fines', icon: 'receipt' },
    ],
  },
];

const STAFF_NAV = [
  {
    label: 'Circulation',
    links: [
      { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
      { to: '/desk', label: 'Desk', icon: 'scan' },
      { to: '/loans', label: 'Loans', icon: 'loans' },
      { to: '/holds', label: 'Holds', icon: 'clock' },
      { to: '/fines', label: 'Fines', icon: 'receipt' },
    ],
  },
  {
    label: 'Library',
    links: [
      { to: '/books', label: 'Catalogue', icon: 'library' },
      { to: '/members', label: 'Members', icon: 'users' },
      { to: '/reports', label: 'Reports', icon: 'chart' },
    ],
  },
];

const ADMIN_NAV = {
  label: 'Admin',
  links: [
    { to: '/admin/rules', label: 'Rules', icon: 'shield' },
    { to: '/admin/staff', label: 'Staff', icon: 'idCard' },
    { to: '/admin/audit', label: 'Audit log', icon: 'scroll' },
  ],
};

const ROLE_LABEL = { ADMIN: 'Admin', LIBRARIAN: 'Librarian', MEMBER: 'Member' };

export default function Layout() {
  const { user, isAdmin, isStaff, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const sections = isStaff ? (isAdmin ? [...STAFF_NAV, ADMIN_NAV] : STAFF_NAV) : MEMBER_NAV;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user.name
    .split(' ')
    .filter((p) => /[A-Za-z]/.test(p[0]) && !p.endsWith('.'))
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const close = () => setMenuOpen(false);
  const subtitle = user.role === 'MEMBER'
    ? [user.memberType?.name, user.cardNumber].filter(Boolean).join(' · ')
    : ROLE_LABEL[user.role];

  return (
    <div className="app-shell">
      <header className="mobile-bar">
        <NavLink to="/dashboard" className="brand" onClick={close}>
          <Logo />
          <span>Shelfwise</span>
        </NavLink>
        <button className="icon-btn" aria-label="Toggle menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((o) => !o)}>
          <Icon name={menuOpen ? 'x' : 'menu'} size={18} />
        </button>
      </header>

      {menuOpen && <div className="sidebar-backdrop" onClick={close} />}

      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <NavLink to="/dashboard" className="brand sidebar-brand" onClick={close}>
          <Logo />
          <span>Shelfwise</span>
        </NavLink>

        <nav className="nav">
          {sections.map((section, i) => (
            <div key={section.label || i} className="nav-section">
              {section.label && <span className="nav-section-label">{section.label}</span>}
              {section.links.map((l) => (
                <NavLink key={l.to} to={l.to} className="nav-link" onClick={close}>
                  <Icon name={l.icon} />
                  <span>{l.label}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="nav-user">
          <div className="avatar" aria-hidden="true">{initials}</div>
          <div className="nav-user-text">
            <span className="nav-user-name">{user.name}</span>
            <span className="nav-user-role">{subtitle}</span>
          </div>
          <button className="icon-btn" onClick={handleLogout} aria-label="Log out" title="Log out">
            <Icon name="logOut" />
          </button>
        </div>
      </aside>

      <div className="main">
        <main className="container">
          <Outlet />
        </main>
        <footer className="footer">Shelfwise library management · Spring Boot and React</footer>
      </div>
    </div>
  );
}
