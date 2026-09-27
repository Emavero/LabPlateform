import { NavLink } from 'react-router-dom';
import { Icon, Logo } from '../design-system';
import { isGroup, navigationFor, PRIMARY_NAV, SECONDARY_NAV, type NavEntry, type NavItem } from '../navigation/navigation';
import { useI18n } from '../i18n/I18nContext';
import { useAuth } from '../state/AuthContext';
import { UserMenu } from './UserMenu';

function NavLinkItem({ item, onNavigate, nested = false }: { item: NavItem; onNavigate?: () => void; nested?: boolean }) {
  const { t } = useI18n();
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
      <span>{t(item.label)}</span>
    </NavLink>
  );
}

/** Une entrée simple est un lien ; un groupe est un intitulé suivi de ses sous-entrées. */
function NavList({ items, onNavigate }: { items: readonly NavEntry[]; onNavigate?: () => void }) {
  const { t } = useI18n();
  return (
    <ul className="nav-list">
      {items.map((entry) =>
        isGroup(entry) ? (
          <li key={entry.label} className="nav-group">
            <p className="nav-group__label">
              <Icon name={entry.icon} size={20} />
              <span>{t(entry.label)}</span>
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
  const { user } = useAuth();
  const { t } = useI18n();
  // Un utilisateur ne voit pas les entrées d'administration ; le serveur les
  // refuse de toute façon, le menu ne fait que ne pas les proposer.
  const primary = navigationFor(PRIMARY_NAV, user?.role);
  const secondary = navigationFor(SECONDARY_NAV, user?.role);

  return (
    <aside className={['sidebar', open && 'sidebar--open'].filter(Boolean).join(' ')} aria-label={t('nav.main')}>
      <div className="sidebar__brand">
        <Logo />
      </div>
      <nav className="sidebar__nav">
        <NavList items={primary} onNavigate={onNavigate} />
        <div className="sidebar__divider" role="separator" />
        <NavList items={secondary} onNavigate={onNavigate} />
      </nav>
      <div className="sidebar__footer">
        <UserMenu onNavigate={onNavigate} />
      </div>
    </aside>
  );
}
