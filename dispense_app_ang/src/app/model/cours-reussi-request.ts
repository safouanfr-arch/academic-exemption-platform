export interface CoursReussiRequest {
  etablissementId: number | null;
  coursExterneId: number | null;
  etablissementNom: string;
  code: string | null;
  nom: string | null;
  ects: number | null;
  note: string | null;
  prgm: string | null;
  lienProgramme: string | null;
  hasPedagogicalUpload: boolean;
}
