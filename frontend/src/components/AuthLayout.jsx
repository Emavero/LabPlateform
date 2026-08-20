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

/**
 * Schéma d'accès : Vous → Plateforme (auth) → Labs (VM). C'est l'élément
 * signature de l'interface — il explique le produit dans le vocabulaire
 * d'un schéma réseau plutôt que par une illustration décorative.
 */
function AccessDiagram() {
  return (
    <svg viewBox="0 0 340 200" xmlns="http://www.w3.org/2000/svg">
      <g stroke="var(--line-strong)" strokeWidth="1" fill="none">
        <line x1="76" y1="100" x2="128" y2="100" />
        <polyline points="228,80 258,80 258,52 296,52" />
        <polyline points="228,120 258,120 258,148 296,148" />
      </g>
      <g>
        <circle cx="258" cy="80" r="2" fill="var(--cyan)" />
        <circle cx="258" cy="120" r="2" fill="var(--cyan)" />
      </g>

      <g>
        <rect x="6" y="80" width="70" height="40" rx="3" fill="var(--navy-2)" stroke="var(--line-strong)" />
        <text x="41" y="104" textAnchor="middle" fontFamily="'IBM Plex Mono', monospace" fontSize="11" fill="var(--ink)">VOUS</text>
      </g>

      <g>
        <rect x="128" y="65" width="100" height="70" rx="3" fill="var(--navy-2)" stroke="var(--amber)" />
        <text x="178" y="96" textAnchor="middle" fontFamily="'IBM Plex Mono', monospace" fontSize="10" fill="var(--amber)" letterSpacing="0.05em">LAB</text>
        <text x="178" y="112" textAnchor="middle" fontFamily="'IBM Plex Mono', monospace" fontSize="10" fill="var(--ink-dim)">plateforme</text>
      </g>

      <g>
        <rect x="296" y="32" width="38" height="40" rx="3" fill="var(--navy-2)" stroke="var(--line-strong)" />
        <circle cx="330" cy="38" r="2.5" fill="var(--green)" />
        <text x="315" y="56" textAnchor="middle" fontFamily="'IBM Plex Mono', monospace" fontSize="9" fill="var(--ink)">WIN</text>
      </g>
      
      <g>
        <rect x="296" y="128" width="38" height="40" rx="3" fill="var(--navy-2)" stroke="var(--line-strong)" />
        <circle cx="330" cy="134" r="2.5" fill="var(--green)" />
        <text x="315" y="152" textAnchor="middle" fontFamily="'IBM Plex Mono', monospace" fontSize="9" fill="var(--ink)">LNX</text>
      </g>
    </svg>
  );
}

export function AuthLayout({ children }) {
  return (
    <div className="auth-shell">
      <div className="auth-shell__panel">
        <div className="brand">
          <div className="brand__mark">
            <BrandMark />
          </div>
          <div className="brand__name">lab-platform</div>
        </div>

        <div>
          <div className="auth-shell__diagram">
            <AccessDiagram />
          </div>
        </div>
      </div>

      <div className="auth-shell__form-area">{children}</div>
    </div>
  );
}
