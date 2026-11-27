import {TypeCorrespondance} from './type-correspondance';

export interface CorrespondanceConnue {
  coursReussiId: number;
  coursCode: string;
  coursNom: string;
  etablissementNom: string;
  ueCode: string;
  ueNom: string;
  ueEcts: number;
  typeCorrespondance: TypeCorrespondance;
  commentaire: string | null;
}
