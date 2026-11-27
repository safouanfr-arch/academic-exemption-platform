export interface CoursReussi {
  id: number;
  etablissementNom: string;
  code: string;
  nom: string;
  ects: number;
  note: string | null;
  prgm: string | null;
  lienProgramme: string | null;
}
