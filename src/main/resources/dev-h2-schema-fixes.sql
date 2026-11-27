-- Correctifs de schema pour les bases H2 de developpement deja existantes.
-- Hibernate ddl-auto=update ajoute les colonnes, mais ne retire pas toujours les anciennes contraintes NOT NULL.

ALTER TABLE TCOURS_REUSSI ALTER COLUMN FKETABLISSEMENT DROP NOT NULL;
ALTER TABLE TCOURS_REUSSI ALTER COLUMN FKCOURSEXT DROP NOT NULL;
