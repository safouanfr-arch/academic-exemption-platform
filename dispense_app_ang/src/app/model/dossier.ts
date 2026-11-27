import {EtatDossier} from './etat-dossier';
import {Section} from './section';
import {User} from './user';

export interface Dossier {
  id: number;
  date: string;
  etat: EtatDossier;
  objetDemande: string;
  user: User;
  section: Section | null;
}
