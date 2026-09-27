import type { Language } from '@/domain/models/Language';

/**
 * Traduction des messages d'erreur, indexés par leur texte français.
 * <p>
 * Un message d'erreur n'arrive pas ici sous forme de clé mais de phrase : le
 * domaine et le client HTTP les formulent en français — la langue de référence
 * du projet — et le serveur fait de même pour les siens. Les traduire par leur
 * texte évite de faire remonter une clé à travers toutes les validations, au
 * prix d'un couplage au libellé français : le test de dérive vérifie qu'un
 * message du domaine a toujours sa traduction.
 */
const EXACT: Readonly<Record<string, string>> = {
  // Saisie d'un compte
  "L'adresse e-mail est obligatoire.": 'An e-mail address is required.',
  "Le format de l'adresse e-mail est invalide.": 'The e-mail address format is invalid.',
  'Le mot de passe est obligatoire.': 'A password is required.',
  'Confirmez le mot de passe.': 'Confirm the password.',
  'Les deux mots de passe ne correspondent pas.': 'The two passwords do not match.',
  'Saisissez votre mot de passe actuel.': 'Enter your current password.',
  'Le lien de réinitialisation est incomplet.': 'The reset link is incomplete.',

  // Machines et flags
  'Le nom est obligatoire.': 'A name is required.',
  "L'adresse de la machine dans le réseau du lab est obligatoire.":
    'The address of the machine on the lab network is required.',
  'Le flag est obligatoire.': 'The flag is required.',

  // Cours
  'Le titre est obligatoire.': 'A title is required.',
  'Le sujet est obligatoire.': 'A subject is required.',
  'Le titre de la section est obligatoire.': 'The section title is required.',
  'La vidéo doit être une adresse https:// ou un fichier téléversé.':
    'The video must be an https:// address or an uploaded file.',
  'Une section porte au moins un texte, une vidéo ou un quiz.':
    'A section carries at least a text, a video or a quiz.',
  'Un cours comporte au moins une section.': 'A course has at least one section.',

  // Comptes rendus
  'Le compte rendu est vide.': 'The write-up is empty.',

  // Assistance
  'Le message est vide.': 'The message is empty.',

  // Téléversement
  'Format non pris en charge. Attendu : MP4, WebM ou Ogg.': 'Unsupported format. Expected: MP4, WebM or Ogg.',

  // Paiement
  'Référence de paiement invalide.': 'Invalid payment reference.',

  // Transport et erreurs générales
  'Certains champs sont invalides.': 'Some fields are invalid.',
  'Une erreur inattendue est survenue. Réessayez.': 'Something went wrong. Try again.',
  'Le serveur est injoignable. Vérifiez votre connexion puis réessayez.':
    'The server is unreachable. Check your connection, then try again.',
  'Le serveur a rencontré une erreur. Réessayez dans un instant.':
    'The server hit an error. Try again in a moment.',
  'Requête refusée.': 'Request refused.',
};

/**
 * Messages qui portent un nombre : la limite vient du domaine, la phrase du
 * catalogue. Le groupe capturé est réinséré tel quel.
 */
const PATTERNS: readonly (readonly [RegExp, string])[] = [
  [/^Au moins (\d+) caractères\.$/, 'At least $1 characters.'],
  [/^Au plus (\d+) caractères\.$/, 'At most $1 characters.'],
  [/^Le titre est limité à (\d+) caractères\.$/, 'The title is limited to $1 characters.'],
  [/^Le résumé est limité à (\d+) caractères\.$/, 'The summary is limited to $1 characters.'],
  [/^Le compte rendu est limité à (\d+) caractères\.$/, 'The write-up is limited to $1 characters.'],
  [/^Le sujet est limité à (\d+) caractères\.$/, 'The subject is limited to $1 characters.'],
  [/^Le message est limité à (\d+) caractères\.$/, 'The message is limited to $1 characters.'],
  [/^La durée va de 0 à (\d+) minutes\.$/, 'The length ranges from 0 to $1 minutes.'],
  [/^Un flag est une suite de (\d+) caractères hexadécimaux\.$/, 'A flag is a string of $1 hexadecimal characters.'],
  [/^Fichier trop lourd : (\d+) Mo au plus\.$/, 'File too large: $1 MB at most.'],
  [/^Question (\d+) : l'énoncé est obligatoire\.$/, 'Question $1: the statement is required.'],
  [/^Question (\d+) : au moins deux propositions\.$/, 'Question $1: at least two answer options.'],
  [/^Question (\d+) : une proposition ne peut pas être vide\.$/, 'Question $1: an answer option cannot be empty.'],
  [/^Question (\d+) : cochez au moins une bonne réponse\.$/, 'Question $1: tick at least one correct answer.'],
];

/**
 * Message affiché dans la langue demandée. Un message inconnu est rendu tel
 * quel : mieux vaut une phrase en français qu'un champ vide.
 */
export function translateMessage(message: string, language: Language): string {
  if (language === 'fr') return message;
  const exact = EXACT[message];
  if (exact) return exact;
  for (const [pattern, replacement] of PATTERNS) {
    if (pattern.test(message)) return message.replace(pattern, replacement);
  }
  return message;
}
