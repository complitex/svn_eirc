SET autocommit=0;

-- --------------------------------
-- Current database version
-- --------------------------------
INSERT INTO `update` (`version`) VALUE ('20140702_201_0.0.3');

-- --------------------------------
-- Registry record indexes
-- --------------------------------

DROP INDEX `idx_registry_record__status_error` on `registry_record`;

COMMIT;
SET autocommit=1;