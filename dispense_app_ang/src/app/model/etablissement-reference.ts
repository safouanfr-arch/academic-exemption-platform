export interface EtablissementReference {
  id: number;
  nom: string;
  siteWeb: string | null;
  statut: 'VALIDE' | 'A_VERIFIER';
}
