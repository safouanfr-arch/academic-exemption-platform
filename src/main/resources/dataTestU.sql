--sections
merge into TSECTION(CODE,NOM) VALUES ('INFO','Bachelier en Informatique Orientation: Développement d''applications');
merge into TSECTION(CODE,NOM) VALUES ('COMPTA','Bachelier en Comptabilité');
merge into TSECTION(CODE,NOM) VALUES ('MARK','Bachelier en Marketing');
merge into TSECTION(CODE,NOM) VALUES ('SECR','Bachelier en Assistant de direction');

--UE et ses acquis
--IPAP
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('IPAP',8,120,'PRINCIPES ALGORITHMIQUES ET PROGRAMMATION','752105U32D3',
cast('* d''identifier différents langages de programmation existants ;
 * de mettre en oeuvre une méthodologie de résolution de problème (observation,résolution, expérimentation, validation) et de la justifier en fonction de l’objectif
poursuivi ;
 * de concevoir, construire et représenter des algorithmes, en utilisant :
    o les types de données élémentaires,
    o les figures algorithmiques de base (séquence, alternative et répétitive),
    o les instructions,
    o les portées des variables,
    o les fonctions et procédures,
    o la récursivité,
    o les entrées/sorties,
    o les fichiers,
    o les structures de données de base (tableaux et enregistrements) ;
 * de traduire de manière adéquate des algorithmes en respectant les spécificités du langage utilisé (JAVA, PYTHON);
 * de documenter de manière complète et précise les programmes développés ;
 * de produire des tests pour valider les programmes développés.'as CLOB));

merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAP',1,'mettre en oeuvre une représentation algorithmique du problème posé',30,'IPAP');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAP',2,'de développer au moins un programme en respectant les spécificités du langage choisi',30,'IPAP');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAP',3,'de mettre en oeuvre des procédures de test',20,'IPAP');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAP',4,'de justifier la démarche mise en oeuvre dans l’élaboration du (ou des) programme(s)',20,'IPAP');
--association UE Section
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','IPAP');

--IPID
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('IPID',9,100,'PROJET D’INTEGRATION DE DEVELOPPEMENT','7534 35 U32 D2',
cast('* de décrire, de caractériser et de produire le cahier des charges du projet;
* d’identifier les acteurs (collaborateurs, prestataires de service, etc.) intervenant dans la
		réalisation d’un projet d’intégration d’une application, de caractériser leurs rôles, leurs
		droits et leurs responsabilités ;
* de construire un dossier technique reprenant les différentes étapes;
* de mettre en oeuvre le projet en développant, parmi les concepts suivants:
	o la gestion des contenus dynamiques au travers d’une interface administrateur sécurisé,
	o la pagination de l’affichage des résultats d’une requête,
	o l’intégration de services internes et tiers,
	o la gestion de sélections, de filtres et de recherches au sein de l’application,
	o la gestion de la sécurisation et des droits d’accès aux contenus (administrateur,
			utilisateur public, utilisateur enregistré, gestionnaire, etc.),
	o l’affichage différencié des contenus (accessibilité, langue, sécurité,
					  fonctionnalités, disponibilité de l’information, etc.), en fonction des profils utilisateurs,
	o la programmation asynchrone (AJAX…),
	o l’optimisation du code, du cache et des échanges avec la base de données,
	o l’interaction avec un système de gestion de bases de données (récupérer, ajouter, modifier, supprimer des enregistrements, etc.) ;;
	o la programmation orientée objet,
	o l’exploitation d’un framework backend et d’un framework frontend (par exemple React Native),
	o etc.;
* d’identifier des menaces et de sécuriser le site en exploitant par exemple :
	o l’utilisation des outils spécifiques de protection et d’identification,
	o la protection contre des injections SQL, des attaques XSS, des vols de session, par détournement de cookies, etc.,
	o la réécriture d’url,
	o les paramétrages et les restrictions d’accès au serveur,
	o etc. ;
* de gérer des erreurs de programmation au moyen d’outils ou de techniques de débogage et d’y apporter une solution pertinente ;
* d’utiliser à bon escient la documentation disponible.
'as CLOB));

merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPID',1,'de produire et défendre un cahier des charges et son dossier technique par rapport à la proposition du chargé de cours',50,'IPID');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPID',2,'d’implémenter une base de données et l’intégrité des données',30,'IPID');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPID',3,'de déployer et de justifier le site répondant aux consignes figurant dans le cahier des charges',20,'IPID');
--association UE Section
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','IPID');





--IPAI
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('IPAI',6,80,'INITIATION A L''ANALYSE INFORMATIQUE','752401U32D2',
cast('* d''analyser une situation problematique simple ;
 * de modeliser les besoins fonctionnels et les donnees utiles ;
 * de decomposer un probleme en sous-parties coherentes ;
 * de produire une solution analysee avant implementation.' as CLOB));
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAI',1,'identifier les besoins fonctionnels et les contraintes principales',35,'IPAI');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAI',2,'modeliser les donnees et les traitements dans un cas simple',35,'IPAI');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPAI',3,'justifier une analyse claire avant le passage au developpement',30,'IPAI');
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','IPAI');

--IPOO
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('IPOO',5,80,'PROGRAMMATION ORIENTEE OBJET','752205U32D3',
cast('* d''identifier les concepts fondamentaux de la programmation orientee objet ;
 * de concevoir des classes, des objets et leurs interactions ;
 * de mettre en oeuvre l''encapsulation, l''heritage et le polymorphisme ;
 * d''ecrire un code structure, reutilisable et testable.' as CLOB));
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPOO',1,'concevoir une solution a partir de classes et d''objets',35,'IPOO');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPOO',2,'appliquer correctement les grands principes de l''objet',35,'IPOO');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IPOO',3,'produire un code objet clair, structure et reutilisable',30,'IPOO');
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','IPOO');

--ISO2
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('ISO2',5,60,'STRUCTURE DES ORDINATEURS','755101U32D4',
cast('* d''expliquer les principaux composants d''un ordinateur ;
 * de comprendre le fonctionnement general d''une architecture materielle ;
 * de relier les choix techniques aux besoins d''un systeme informatique.' as CLOB));
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('ISO2',1,'decrire les composants principaux d''un ordinateur',35,'ISO2');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('ISO2',2,'expliquer le fonctionnement general d''une architecture materielle',35,'ISO2');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('ISO2',3,'justifier des choix techniques simples',30,'ISO2');
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','ISO2');

--IVTE
merge into TUE(CODE,ECTS,NB_PERIODES,NOM,REF,PRGM) values('IVTE',4,40,'VEILLE TECHNOLOGIQUE','753414U32D1',
cast('* de rechercher une technologie pertinente ;
 * de produire une synthese claire ;
 * de presenter et justifier les choix techniques retenus.' as CLOB));
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IVTE',1,'rechercher une information technologique pertinente',40,'IVTE');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IVTE',2,'produire une synthese claire et structuree',30,'IVTE');
merge into TACQUIS(FKUE,NUM,ACQUIS,POURCENTAGE,FK_UE) values ('IVTE',3,'presenter et justifier les choix techniques',30,'IVTE');
merge into TSEC_UE (FKSECTION,FKUE) values ('INFO','IVTE');

MERGE into TUSER(username,email,nom,prenom) values
('demo-admin','demo-admin@example.test','Demo','Admin'),
('et1','et1@example.test','Nom Et1','Prénom Et1'),
('et2','et2@example.test','Nom Et2','Prénom Et2');

INSERT INTO TDOSSIER(DATE,ETAT,OBJET_DEMANDE,FKUSER) VALUES ('2025-01-10','CLOTURE','Demande de dispense','et1');
INSERT INTO TDOSSIER(DATE,ETAT,OBJET_DEMANDE,FKUSER) VALUES ('2026-01-19','CLOTURE','Demande de dispense','et1');
INSERT INTO TDOSSIER(DATE,ETAT,OBJET_DEMANDE,FKUSER) VALUES ('2026-01-22','DEMANDE_EN_COURS','Demande de dispense pour 3 cours','et1');
INSERT INTO TDOSSIER(DATE,ETAT,OBJET_DEMANDE,FKUSER) VALUES ('2026-01-18','DEMANDE_EN_COURS','Demande de dispense en venant de l''ULB','et2');
