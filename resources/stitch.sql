-------------------------------- stitch kritis
--------------------------------------------------------------------------

-- stitch #1
select k.kriti as kar_kriti, s.kriti as sang_kriti, similarity (k.kriti, s.kriti) as similarity, k.ragam as kragam, s.ragam as sragam from karnatik_data k inner join sangeethapriya_kritis s on k.kriti % s.kriti and similarity (k.kriti, s.kriti) > 0.4 order by similarity desc;

-- stitch #2
SELECT k.kriti AS kar_kriti, s.kriti AS sang_kriti,
       similarity (k.kriti, s.kriti) AS k_similarity,
       levenshtein (k.kriti, s.kriti) AS lev_diff,
       k.ragam AS kragam, s.ragam AS sragam,
       similarity (k.ragam, s.ragam) AS r_similarity
FROM karnatik_data k
INNER JOIN sangeethapriya_kritis s
  ON k.kriti % s.kriti AND similarity (k.kriti, s.kriti) > 0.4
ORDER BY lev_diff, r_similarity DESC;

-- stitch #3
SELECT k.kriti AS kar_kriti, s.kriti AS sang_kriti,
       similarity (k.kriti, s.kriti) +
       similarity (k.ragam, s.ragam) AS score,
       k.ragam AS kragam, s.ragam AS sragam,
       levenshtein (k.kriti, s.kriti) AS lev_diff
FROM karnatik_data k
INNER JOIN sangeethapriya_kritis s
  ON k.kriti % s.kriti AND similarity (k.kriti, s.kriti) > 0.4
ORDER BY score DESC;

-- stitch #4
SELECT kar_kriti, sang_kriti, kragam, sragam, k_similarity, r_similarity, k_similarity + r_similarity from (
SELECT k.kriti AS kar_kriti, s.kriti AS sang_kriti,
       similarity (k.kriti, s.kriti) AS k_similarity
       similarity (k.ragam, s.ragam) AS r_similarity
       k.ragam AS kragam, s.ragam AS sragam,
       levenshtein (k.kriti, s.kriti) AS lev_diff
FROM karnatik_data k
INNER JOIN sangeethapriya_kritis s
  ON k.kriti % s.kriti AND similarity (k.kriti, s.kriti) > 0.4
  ) res
  WHERE score > 0.85
ORDER BY score DESC;

-- stitch #5
SELECT kar_kriti, sang_kriti, kragam, sragam, k_similarity, r_similarity, k_similarity + r_similarity as score into adhoc_kritis from (
SELECT k.kriti AS kar_kriti, s.kriti AS sang_kriti,
       similarity (k.kriti, s.kriti) AS k_similarity,
       similarity (k.ragam, s.ragam) AS r_similarity,
       k.ragam AS kragam, s.ragam AS sragam,
       levenshtein (k.kriti, s.kriti) AS lev_diff
FROM karnatik_data k
INNER JOIN sangeethapriya_kritis s
  ON k.kriti % s.kriti AND similarity (k.kriti, s.kriti) > 0.4
  ) res
ORDER BY score DESC;

-------------------------------- stitch ragams
--------------------------------------------------------------------------
WITH kragams AS (SELECT DISTINCT ragam FROM karnatik_data),
     sragams AS (SELECT DISTINCT lower(ragam) as ragam FROM sangeethapriya_kritis)
SELECT k.ragam AS kragam, s.ragam AS sragam,
       similarity (k.ragam, s.ragam) AS similarity,
       levenshtein (k.ragam, s.ragam) AS lev_diff,
       difference (k.ragam, s.ragam) AS soundex_diff
FROM kragams k
INNER JOIN sragams s
ON k.ragam % s.ragam AND similarity (k.ragam, s.ragam) > 0;

-- sum of 3 diffs should be low
SELECT kragam, sragam, r_similarity, lev_diff, soundex_diff,
       r_similarity + lev_diff + soundex_diff as score
FROM (
WITH kragams AS (SELECT DISTINCT ragam FROM karnatik_data),
     sragams AS (SELECT DISTINCT lower(ragam) as ragam FROM sangeethapriya_kritis)
SELECT k.ragam AS kragam, s.ragam AS sragam,
       1-(similarity (k.ragam, s.ragam)) AS r_similarity,
       (levenshtein (k.ragam, s.ragam)/10.0) AS lev_diff,
       ( (4 - difference (k.ragam, s.ragam)) / 4) AS soundex_diff
FROM kragams k
INNER JOIN sragams s
ON k.ragam % s.ragam AND similarity (k.ragam, s.ragam) > 0
) AS res
ORDER BY score;


-- weighted sum should be high
SELECT kragam, sragam, r_similarity, lev_diff, soundex_diff, score
FROM (
SELECT distinct on (kragam) kragam, sragam, r_similarity, lev_diff, soundex_diff,
       r_similarity + lev_diff + soundex_diff as score
FROM (
WITH kragams AS (SELECT DISTINCT ragam FROM karnatik_data),
     sragams AS (SELECT DISTINCT lower(ragam) as ragam FROM sangeethapriya_kritis)
SELECT k.ragam AS kragam, s.ragam AS sragam,
       (10 * similarity (k.ragam, s.ragam)) AS r_similarity,
       (10 - levenshtein (k.ragam, s.ragam)) AS lev_diff,
       (10 * (difference (k.ragam, s.ragam) /4 )) AS soundex_diff
FROM kragams k
INNER JOIN sragams s
ON k.ragam % s.ragam AND similarity (k.ragam, s.ragam) > 0
) AS result_one
ORDER BY kragam, score DESC
) result_two
ORDER BY score DESC;

-- weighted sum should be high
WITH sims as (
SELECT kragam, sragam, r.id, r_similarity, lev_diff, soundex_diff, score
FROM (
SELECT distinct on (sragam) sragam, kragam, r_similarity, lev_diff, soundex_diff,
       r_similarity + lev_diff + soundex_diff as score
FROM (
WITH kragams AS (SELECT DISTINCT ragam FROM karnatik_data),
     sragams AS (SELECT DISTINCT lower(ragam) as ragam FROM sangeethapriya_kritis)
     SELECT k.ragam AS kragam, s.ragam AS sragam,
     (10 * similarity (k.ragam, s.ragam)) AS r_similarity,
     (10 - levenshtein (k.ragam, s.ragam)) AS lev_diff,
     (10 * (difference (k.ragam, s.ragam) /4 )) AS soundex_diff
FROM kragams k
INNER JOIN sragams s
ON k.ragam % s.ragam AND similarity (k.ragam, s.ragam) > 0
) AS result_one
ORDER BY sragam, score DESC
) result_two
INNER JOIN ragams r on r.name = kragam
ORDER BY score DESC)
UPDATE sangeethapriya_renditions
SET ragam_id = r.id
FROM sims r where r.sragam = lower(ragam);




--- search query

SELECT * FROM
(
SELECT *,
(10 * similarity (kriti, ?)) +
(10 - levenshtein (kriti, ?)) +
(10 * (difference (kriti, ?) /4 )) AS score
FROM sangeethapriya_renditions
ORDER BY score DESC
LIMIT 20
) AS renditions sr
INNER JOIN sangeethapriya_tracks st
ON sr.concert_id = st.concert_id
AND sr.track = st.track_number;


SELECT * FROM
(
SELECT *,
(10 * similarity (kriti, ?)) +
(10 - levenshtein (kriti, ?)) +
(10 * (difference (kriti, ?) /4 )) AS similarity_score
FROM sangeethapriya_renditions
ORDER BY similarity_score DESC
LIMIT 20
) AS sr
INNER JOIN sangeethapriya_tracks st
ON sr.concert_id = st.concert_id
AND sr.track = st.track_number;







------ form stardard list of ragams

WITH sims as (
SELECT kragam, sragam, r.id, r_similarity, lev_diff, soundex_diff, score
FROM (
SELECT distinct on (sragam) sragam, kragam, r_similarity, lev_diff, soundex_diff,
       r_similarity + lev_diff + soundex_diff as score
FROM (
     SELECT w.raga_name AS w_ragam, k.ragam AS k_ragam
       (10 * similarity (w_ragam, k_ragam)) +
       (10 - levenshtein (w_ragam, k_ragam)) +
       (10 * (difference (w_ragam, k_ragam) /4 )) AS similarity_score
     FROM wiki_ragams w
     INNER JOIN karnatik_ragams k
     ON k.ragam % s.ragam AND similarity (k.ragam, s.ragam) > 0
) AS result_one
ORDER BY sragam, score DESC
) result_two
INNER JOIN ragams r on r.name = kragam
ORDER BY score DESC)
UPDATE sangeethapriya_renditions
SET ragam_id = r.id
FROM sims r where r.sragam = lower(ragam);


SELECT w.raga_name AS w_ragam, k.ragam AS k_ragam,
(10 * similarity (lower(unaccent((w.raga_name))), lower(k.ragam))) +
(10 - levenshtein (lower(unaccent(w.raga_name)), lower(k.ragam))) +
(10 * (difference (lower(unaccent(w.raga_name)), lower(k.ragam))) /4) AS similarity_score
INTO w_k_ragams
FROM wiki_ragams w
INNER JOIN karnatik_ragams k
ON k.ragam % w.raga_name AND similarity (k.ragam, w.raga_name) > 0
ORDER BY similarity_score DESC;

SELECT w.raga_name AS w_ragam, k.ragam AS s_ragam,
(10 * similarity (lower(unaccent((w.raga_name))), lower(k.ragam))) +
(10 - levenshtein (lower(unaccent(w.raga_name)), lower(k.ragam))) +
(10 * (difference (lower(unaccent(w.raga_name)), lower(k.ragam))) /4) AS similarity_score
INTO w_s_ragams
FROM wiki_ragams w
INNER JOIN sang_ragams k
ON k.ragam % w.raga_name AND similarity (k.ragam, w.raga_name) > 0
ORDER BY similarity_score DESC;

WITH sang AS (select w_ragam, array_agg(s_ragam) s_ragams from w_s_ragams where similarity_score > 20 group by w_ragam),
     karn AS (select w_ragam, array_agg(k_ragam) k_ragams from w_k_ragams where similarity_score > 20 group by w_ragam)
SELECT w.raga_name AS ragam, s_ragams, k_ragams into ragams
FROM wikipedia_ragas w
LEFT OUTER JOIN sang on w.raga_name = sang.w_ragam
LEFT OUTER JOIN karn on w.raga_name = karn.w_ragam;
