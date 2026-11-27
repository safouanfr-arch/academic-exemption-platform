import {Acquis} from './acquis';

export interface UeDetail {
  code: string;
  ref: string;
  nom: string;
  nbPeriodes: number;
  ects: number;
  prgm: string;
  acquis: Acquis[];
}
