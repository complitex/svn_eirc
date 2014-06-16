START TRANSACTION;

-- --------------------------------
-- Current database version
-- --------------------------------
INSERT INTO `update` (`version`) VALUE ('20140610_195_0.0.2');

-- --------------------------------
-- Status can be nullable
-- --------------------------------

ALTER TABLE `registry` MODIFY COLUMN `status` BIGINT(20) comment 'Record status reference';

COMMIT;