#!/usr/bin/env python3
"""Génère docs/ARCHITECTURE.pdf à partir de docs/ARCHITECTURE.md.

    pip install markdown
    python3 docs/build-pdf.py

Le rendu passe par un navigateur sans interface (Chromium, Chrome ou
Chromium de Playwright, le premier trouvé) : la mise en page du PDF est donc
exactement celle décrite par la feuille de style ci-dessous.
"""

from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

try:
    import markdown
except ImportError:  # pragma: no cover - dépendance signalée à l'exécution
    sys.exit("Dépendance manquante : pip install markdown")

DOCS = Path(__file__).resolve().parent
SOURCE = DOCS / "ARCHITECTURE.md"
TARGET = DOCS / "ARCHITECTURE.pdf"

BROWSERS = (
    "/opt/pw-browsers/chromium-1194/chrome-linux/chrome",
    "chromium",
    "chromium-browser",
    "google-chrome",
    "google-chrome-stable",
)

# Document imprimé : fond clair, texte dense mais aéré, code lisible.
STYLE = """
@page { size: A4; margin: 17mm 16mm 18mm; }
:root {
  --ink: #16181f;
  --muted: #5b6070;
  --line: #d8dbe4;
  --accent: #5b4bd6;
  --accent-soft: #f1efff;
  --code-bg: #f5f6fa;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  color: var(--ink);
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 10.2pt;
  line-height: 1.55;
}
h1, h2, h3, h4 { line-height: 1.25; font-weight: 700; }
h1 {
  margin: 0 0 0.6rem;
  padding-bottom: 0.5rem;
  border-bottom: 3px solid var(--accent);
  font-size: 22pt;
}
h2 {
  margin: 1.9rem 0 0.7rem;
  padding-top: 0.7rem;
  border-top: 1px solid var(--line);
  font-size: 14pt;
  break-after: avoid;
}
h3 { margin: 1.2rem 0 0.4rem; font-size: 11.5pt; color: var(--accent); break-after: avoid; }
p, ul, ol { margin: 0 0 0.65rem; }
li { margin-bottom: 0.2rem; }
a { color: var(--accent); text-decoration: none; }
strong { font-weight: 700; }
blockquote {
  margin: 0 0 1rem;
  padding: 0.7rem 1rem;
  border-left: 3px solid var(--accent);
  background: var(--accent-soft);
  color: var(--muted);
}
blockquote p:last-child { margin-bottom: 0; }
code {
  padding: 0.08em 0.32em;
  border-radius: 3px;
  background: var(--code-bg);
  font-family: "SFMono-Regular", Menlo, Consolas, monospace;
  font-size: 0.86em;
}
pre {
  margin: 0 0 1rem;
  padding: 0.75rem 0.9rem;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: var(--code-bg);
  font-size: 8.4pt;
  line-height: 1.45;
  white-space: pre-wrap;
  break-inside: avoid;
}
pre code { padding: 0; background: none; font-size: inherit; }
table {
  width: 100%;
  margin: 0 0 1rem;
  border-collapse: collapse;
  font-size: 9pt;
  break-inside: avoid;
}
th, td { padding: 0.4rem 0.55rem; border: 1px solid var(--line); text-align: left; vertical-align: top; }
th { background: var(--accent-soft); font-weight: 700; }
hr { margin: 1.6rem 0; border: 0; border-top: 1px solid var(--line); }
"""


def find_browser() -> str:
    for candidate in BROWSERS:
        resolved = candidate if Path(candidate).exists() else shutil.which(candidate)
        if resolved:
            return resolved
    sys.exit("Aucun navigateur trouvé pour l'impression (Chromium ou Chrome).")


def render_html(source: Path) -> str:
    body = markdown.markdown(
        source.read_text(encoding="utf-8"),
        extensions=["tables", "fenced_code", "sane_lists", "toc"],
    )
    return (
        "<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\">"
        "<title>Architecture de cyberMans</title>"
        f"<style>{STYLE}</style></head><body>{body}</body></html>"
    )


def main() -> None:
    if not SOURCE.exists():
        sys.exit(f"Source introuvable : {SOURCE}")

    browser = find_browser()
    with tempfile.TemporaryDirectory() as workdir:
        page = Path(workdir) / "architecture.html"
        page.write_text(render_html(SOURCE), encoding="utf-8")
        subprocess.run(
            [
                browser,
                "--headless",
                "--disable-gpu",
                "--no-sandbox",
                f"--user-data-dir={workdir}/profile",
                "--no-pdf-header-footer",
                f"--print-to-pdf={TARGET}",
                page.as_uri(),
            ],
            check=True,
            capture_output=True,
        )
    print(f"{TARGET} ({TARGET.stat().st_size // 1024} Ko)")


if __name__ == "__main__":
    main()
