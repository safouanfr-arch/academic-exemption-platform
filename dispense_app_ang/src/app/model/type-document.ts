export enum TypeDocument {
  LETTRE_MOTIVATION = 'LETTRE_MOTIVATION',
  BULLETIN = 'BULLETIN',
  DOSSIER_PEDAGOGIQUE = 'DOSSIER_PEDAGOGIQUE',
  AUTRE = 'AUTRE'
}

export const TYPE_DOCUMENT_LABELS: Record<TypeDocument, string> = {
  [TypeDocument.LETTRE_MOTIVATION]: 'Lettre de motivation',
  [TypeDocument.BULLETIN]: 'Bulletin',
  [TypeDocument.DOSSIER_PEDAGOGIQUE]: 'Dossier pedagogique',
  [TypeDocument.AUTRE]: 'Autre'
};
