-- Role: playnomm
-- DROP ROLE IF EXISTS playnomm;

CREATE ROLE playnomm WITH
  LOGIN
  NOSUPERUSER
  INHERIT
  CREATEDB
  CREATEROLE
  NOREPLICATION
  VALID UNTIL 'infinity';
  
GRANT rds_superuser TO playnomm;
