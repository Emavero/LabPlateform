import { NavLink } from 'react-router-dom';
import { Icon, Logo } from '../design-system';
import { isGroup, PRIMARY_NAV, SECONDARY_NAV, type NavEntry, type NavItem } from '../navigation/navigation';
import { UserMenu } from './UserMenu';

function NavLinkItem({ item, onNavigate, nested = false }: { item: NavItem; onNavigate?: () => void; nested?: boolean }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      onClick={onNavigate}
      className={({ isActive }) =>
        ['nav-link', nested && 'nav-link--nested', isActive && 'nav-link--active'].filter(Boolean).join(' ')
      }
    >
      <Icon name={item.icon} size={nested ? 18 : 20} />
      <span>{item.label}</span>
    </NavLink>
  );
}

/** Une entrée simple est un lien ; un groupe est un intitulé suivi de ses sous-entrées. */
function NavList({ items, onNavigate }: { items: readonly NavEntry[]; onNavigate?: () => void }) {
  return (
    <ul className="nav-list">
      {items.map((entry) =>
        isGroup(entry) ? (
          <li key={entry.label} className="nav-group">
            <p className="nav-group__label">
              <Icon name={entry.icon} size={20} />
              <span>{entry.label}</span>
            </p>
            <ul className="nav-list nav-list--nested">
              {entry.children.map((child) => (
                <li key={child.to}>
                  <NavLinkItem item={child} onNavigate={onNavigate} nested />
                </li>
              ))}
            </ul>
          </li>
        ) : (
          <li key={entry.to}>
            <NavLinkItem item={entry} onNavigate={onNavigate} />
          </li>
        ),
      )}
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
