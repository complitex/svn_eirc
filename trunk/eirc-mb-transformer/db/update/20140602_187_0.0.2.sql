START TRANSACTION;

-- --------------------------------
-- Current database version
-- --------------------------------
INSERT INTO `update` (`version`) VALUE ('20140602_187_0.0.2');

-- --------------------------------
-- Delete sequence for registry_number
-- --------------------------------

DELETE FROM `sequence` WHERE `sequence_name` = 'registry_number';

-- ------------------------------
-- Registry number
-- ------------------------------

DROP TABLE IF EXISTS `registry_number`;

CREATE TABLE `registry_number` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Автоинкремент',
  `value` INTEGER NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Используется для генерации номера реестра';

COMMIT;