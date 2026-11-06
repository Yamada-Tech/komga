-- Remove the duplicate page detection feature and the historical event log.
-- Book.FILE_HASH (file level) is kept: it is used for scanning optimizations and duplicate file detection.
-- Page level hashes (MEDIA_PAGE.FILE_HASH), the PAGE_HASH tables, the LIBRARY.HASH_PAGES option,
-- and the HISTORICAL_EVENT tables are all dropped.

DROP TABLE IF EXISTS PAGE_HASH_THUMBNAIL;
DROP TABLE IF EXISTS PAGE_HASH;

ALTER TABLE MEDIA_PAGE
    DROP COLUMN FILE_HASH;

ALTER TABLE LIBRARY
    DROP COLUMN HASH_PAGES;

-- Historical event log
DROP TABLE IF EXISTS HISTORICAL_EVENT_PROPERTIES;
DROP TABLE IF EXISTS HISTORICAL_EVENT;
