/** Déclenche l'enregistrement d'un fichier texte sur le poste de l'utilisateur. */
export function saveTextFile(fileName: string, content: string, mimeType = 'application/x-openvpn-profile'): void {
  const url = URL.createObjectURL(new Blob([content], { type: mimeType }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Laisse au navigateur le temps de démarrer le téléchargement avant de libérer l'objet.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
