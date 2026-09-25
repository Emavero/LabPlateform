import { NavLink } from 'react-router-dom';
import { Icon, Logo } from '../design-system';
import { PRIMARY_NAV, SECONDARY_NAV, type NavItem } from '../navigation/navigation';
import { UserMenu } from './UserMenu';

function NavList({ items, onNavigate }: { items: readonly NavItem[]; onNavigate?: () => void }) {
  return (
    <ul className="nav-list">
      {items.map((item) => (
        <li key={item.to}>
          <NavLink
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) => ['nav-link', isActive && 'nav-link--active'].filter(Boolean).join(' ')}
          >
            <Icon name={item.icon} size={20} />
            <span>{item.label}</span>
          </NavLink>
        </li>
      ))}
    </ul>
  );
}

interface SidebarProps {
  open: boolean;
  onNavigate: () => void;
}

export function Sidebar({ open, onNavigate }: SidebarProps) {
  return (
    <aside className={['sidebar', open && 'sidebar--open'].filter(Boolean).join(' ')} aria-label="Navigation principale">
      <div className="sidebar__brand">
        <Logo />
      </div>
      <nav className="sidebar__nav">
        <NavList items={PRIMARY_NAV} onNavigate={onNavigate} />
        <div className="sidebar__divider" role="separator" />
        <NavList items={SECONDARY_NAV} onNavigate={onNavigate} />
      </nav>
      <div className="sidebar__footer">
        <UserMenu onNavigate={onNavigate} />
      </div>
    </aside>
  );
}
