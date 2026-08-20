import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function BrandMark() {
  return (
    <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M2 6.5V2h4.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="M13.5 2H18v4.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="M18 13.5V18h-4.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="M6.5 18H2v-4.5" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="10" cy="10" r="1.4" fill="currentColor" />
    </svg>
  );
}

function MenuIcon() {
  return (
    <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M2 4.5h14M2 9h14M2 13.5h14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function AppTopbar({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="topbar">
      <div className="topbar__left">
        <button className="sidebar-toggle" onClick={onToggleSidebar} aria-label="Basculer le menu">
          <MenuIcon />
        </button>

        <Link to="/" className="brand">
          <div className="brand__mark">
            <BrandMark />
          </div>
          <div className="brand__name">lab-platform</div>
        </Link>
      </div>

      <div className="topbar__user">
        <Link to="/profile" className="mono topbar__user-link">
          {user?.email}
        </Link>
        <button className="btn-ghost" onClick={handleLogout}>
          Déconnexion
        </button>
      </div>
    </header>
  );
}
