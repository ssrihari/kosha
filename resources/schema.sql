CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
CREATE EXTENSION IF NOT EXISTS unaccent;
-- source specific tables

-- karnatik
CREATE TABLE IF NOT EXISTS karnatik_data (
  id       BIGSERIAL PRIMARY KEY,
  kriti    VARCHAR(100),
  ragam    VARCHAR(50),
  composer VARCHAR(50),
  taalam   VARCHAR(30),
  language VARCHAR(30),
  lyrics   TEXT,
  meaning  TEXT,
  notation TEXT,
  url      VARCHAR(100));

CREATE INDEX karnatik_data_kriti ON karnatik_data USING gist(kriti gist_trgm_ops);
CREATE INDEX karnatik_data_ragam ON karnatik_data USING gist(ragam gist_trgm_ops);

-- sangeethapriya
CREATE TABLE IF NOT EXISTS sangeethapriya_tracks (
  id           BIGSERIAL PRIMARY KEY,
  concert_id   VARCHAR(25),
  concert_url  TEXT,
  track_number INTEGER,
  track_url    TEXT
);

CREATE TABLE IF NOT EXISTS sangeethapriya_renditions (
  id           BIGSERIAL PRIMARY KEY,
  concert_id   VARCHAR(25),
  concert_url  TEXT,
  track        INTEGER,
  kriti        TEXT,
  ragam        TEXT,
  composer     TEXT,
  main_artist  TEXT);

CREATE TABLE IF NOT EXISTS sangeethapriya_kritis (
  id           BIGSERIAL PRIMARY KEY,
  kriti        VARCHAR(100),
  ragam        VARCHAR(100),
  composer     TEXT);

CREATE INDEX sangeethapriya_kritis_kriti ON sangeethapriya_kritis USING gist(kriti gist_trgm_ops);
CREATE INDEX sangeethapriya_kritis_ragam ON sangeethapriya_kritis USING gist(ragam gist_trgm_ops);

-- wikipedia
CREATE TABLE IF NOT EXISTS wikipedia_ragas (
  id           BIGSERIAL PRIMARY KEY,
  raga_name    VARCHAR(100),
  raga_link    TEXT,
  mela_number  INTEGER,
  melakartha   BOOLEAN);

CREATE TABLE IF NOT EXISTS wikipedia_scales (
  id           BIGSERIAL PRIMARY KEY,
  raga_name    VARCHAR(100),
  arohanam     TEXT,
  avarohanam   TEXT);

PREPARE sang_search (VARCHAR, FLOAT) AS
SELECT * FROM sangeethapriya_kritis
WHERE kriti % $1
AND similarity (kriti, $1) > $2;

PREPARE kar_search (VARCHAR, FLOAT) AS
SELECT * FROM karnatik_data
WHERE kriti % $1
AND similarity (kriti, $1) > $2;

-- generic tables

CREATE TABLE IF NOT EXISTS ragams (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(100) UNIQUE,
  arohanam        VARCHAR(50),
  avarohanam      VARCHAR(50));
-- insert into ragams (name) select distinct(ragam) from karnatik_data where ragam is not null;

CREATE TABLE IF NOT EXISTS kritis (
  id                BIGSERIAL PRIMARY KEY,
  name              VARCHAR(100),
  karnatik_id       BIGINT,
  sangeethapriya_id BIGINT);

CREATE VIEW IF NOT EXISTS std_ragams AS
SELECT r.ragam AS ragam_name, ws.arohanam, ws.avarohanam
FROM ragams r
INNER JOIN wikipedia_scales ws ON r.ragam = ws.raga_name;

CREATE OR REPLACE FUNCTION similarity_score(a VARCHAR, b VARCHAR)
RETURNS FLOAT AS $$
DECLARE score FLOAT;
BEGIN
        SELECT (10 * similarity (a, b)) +
        (1.3 * (10 - levenshtein (a, b))) +
               (10 * (difference (a, b) /4 ))
	INTO score;
        RETURN score/3.3;
END;
$$ LANGUAGE plpgsql;

-- PREPARE rsearch (varchar) AS SELECT *, similarity_score(ragam_name, $1) score FROM std_ragams ORDER BY score DESC LIMIT 10;
-- EXECUTE rsearch ('punnagavarali');
