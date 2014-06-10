START TRANSACTION;

-- --------------------------------
-- Current database version
-- --------------------------------
INSERT INTO `update` (`version`) VALUE ('20140610_192_0.0.1');

-- --------------------------------
-- Add index on `status` and `import_error_type` columns of registry_record table
-- --------------------------------

CREATE INDEX `idx_registry_record__status_error` ON `registry_record` (`registry_id`, `status`, `import_error_type`);

COMMIT;