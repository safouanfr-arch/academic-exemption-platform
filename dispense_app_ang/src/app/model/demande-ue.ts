export interface DemandeUe {
  id: number;
  ueCode: string;
  ueNom: string;
  ueEcts: number;
  commentaire: string | null;
  coursReussisIds: number[];
}
