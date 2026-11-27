import {TypeDocument} from './type-document';

export interface DocumentDossier {
  id: number;
  nomFichier: string;
  typeDocument: TypeDocument | null;
  cheminFichier: string | null;
  urlDocument: string | null;
  coursReussiId: number | null;
}
