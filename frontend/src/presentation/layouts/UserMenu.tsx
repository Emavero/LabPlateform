import { useEffect, useId, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { displayNameOf, initialsOf, ROLE_LABELS } from '@/domain/models/User';
import { Avatar, Icon } from '../design-system';
import { useAuth } from '../state/AuthContext';

/** Profil ancré en bas de la sidebar, avec menu déroulant (s'ouvre vers le haut). */
export function UserMenu({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
        trigger.current?.focus();
      }
    };
    document.addEventListener('pointerdown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!user) return null;

  const go = (path: string) => {
    setOpen(false);
    onNavigate?.();
    navigate(path);
  };

  const signOut = async () => {
    setOpen(false);
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="user-menu" ref={root}>
      {open && (
        <div className="user-menu__popover" id={menuId} role="menu">
          <p className="user-menu__email">{user.email}</p>
          <button type="button" role="menuitem" className="user-menu__item" onClick={() => go('/settings')}>
            <Icon name="user" size={16} />
            Paramètres du compte
          </button>
          <button type="button" role="menuitem" className="user-menu__item user-menu__item--danger" onClick={signOut}>
            <Icon name="logout" size={16} />
            Se déconnecter
          </button>
        </div>
      )}
      <button
        ref={trigger}
        type="button"
        className="user-menu__trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => setOpen((v) => !v)}
      >
        <Avatar initials={initialsOf(user)} />
        <span className="user-menu__identity">
          <span className="user-menu__name">{displayNameOf(user)}</span>
          <span className="user-menu__role">{ROLE_LABELS[user.role]}</span>
        </span>
        <Icon name="chevronsUpDown" size={16} className="user-menu__chevron" />
      </button>
    </div>
  );
}
