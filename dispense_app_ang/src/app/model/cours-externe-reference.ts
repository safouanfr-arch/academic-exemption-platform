export interface CoursExterneReference {
  id: number;
  etablissementId: number;
  code: string;
  nom: string;
  ects: number;
  prgm: string | null;
  lienProgramme: string | null;
}
