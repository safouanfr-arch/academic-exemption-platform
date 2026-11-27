export enum EtatDossier {
  DEMANDE_EN_COURS = 'DEMANDE_EN_COURS',
  TRAITEMENT_DIRECTION = 'TRAITEMENT_DIRECTION',
  TRAITEMENT_PROFESSEUR = 'TRAITEMENT_PROFESSEUR',
  ATTENTE_COMPLEMENT = 'ATTENTE_COMPLEMENT',
  TRAITE = 'TRAITE',
  CLOTURE = 'CLOTURE'
}

export const ETAT_DOSSIER_LABELS: Record<EtatDossier, string> = {
  [EtatDossier.DEMANDE_EN_COURS]: 'Demande en cours',
  [EtatDossier.TRAITEMENT_DIRECTION]: 'Direction',
  [EtatDossier.TRAITEMENT_PROFESSEUR]: 'Professeur',
  [EtatDossier.ATTENTE_COMPLEMENT]: 'Attente de complement',
  [EtatDossier.TRAITE]: 'Traite',
  [EtatDossier.CLOTURE]: 'Cloture'
};
