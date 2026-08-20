import { Link, useLocation } from 'react-router-dom';

const NAV_ITEMS = [
  { to: '/', label: 'Tableau de bord', icon: 'brd' },
  { to: '/labs', label: 'Mes Labs', icon: 'vm' },
  { to: '/profile', label: 'Mon profil', icon: 'usr' },
];

export function Sidebar({ open, onClose }) {
  const location = useLocation();

  const isActive = (to) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to));

  return (
    <>
      {open && <div className="sidebar-backdrop" onClick={onClose} />}

      <aside className={`sidebar ${open ? '' : 'sidebar--closed'}`}>
        <div className="sidebar__mobile-head">
          <span className="mono">Menu</span>
          <button className="sidebar__close" onClick={onClose} aria-label="Fermer le menu">
            ×
          </button>
        </div>

        <nav className="sidebar__nav">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`sidebar__link ${isActive(item.to) ? 'active' : ''}`}
              onClick={onClose}
            >
              <span className="sidebar__link-icon mono">{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
    </>
  );
}
