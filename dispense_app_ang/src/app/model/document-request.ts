import {TypeDocument} from './type-document';

export interface DocumentRequest {
  nomFichier: string;
  typeDocument: TypeDocument | null;
  cheminFichier: string | null;
  urlDocument: string | null;
  coursReussiId: number | null;
}
