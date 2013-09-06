/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Service provider account --
DROP TABLE IF EXISTS `eirc_account`;
CREATE TABLE `eirc_account` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `object_id` BIGINT(20) NOT NULL,
  `account_number` VARCHAR(255) NOT NULL,
  `address_id` BIGINT(20) NOT NULL,
  `address_entity_id` BIGINT(20) NOT NULL,
  `first_name` VARCHAR(255),
  `last_name` VARCHAR(255),
  `middle_name` VARCHAR(255),
  `begin_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_date` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`begin_date`),
  KEY `key_object_id` (object_id),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  CONSTRAINT `fk_eirc_account__entity` FOREIGN KEY (`address_entity_id`) REFERENCES `entity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Service --
DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(128),
  `parent_id` BIGINT(20),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_parent_service` FOREIGN KEY (`parent_id`) REFERENCES `service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `service_string_culture`;
CREATE TABLE `service_string_culture` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_id` BIGINT(20) NOT NULL,
  `locale_id` BIGINT(20) NOT NULL,
  `value` VARCHAR(1000),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_id__locale` (`service_id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`),
  CONSTRAINT `fk_service_string_culture__service` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_service_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- Service provider account --
DROP TABLE IF EXISTS `service_provider_account`;
CREATE TABLE `service_provider_account` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `object_id` BIGINT(20) NOT NULL,
  `eirc_account_id` BIGINT(20) NOT NULL,
  `organization_id` BIGINT(20) NOT NULL,
  `account_number` VARCHAR(255) NOT NULL,
  `service_id` BIGINT(20) NOT NULL,
  `first_name` VARCHAR(255),
  `last_name` VARCHAR(255),
  `middle_name` VARCHAR(255),
  `begin_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_date` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`begin_date`),
  KEY `key_object_id` (object_id),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  CONSTRAINT `fk_service_provider_account__eirc_account` FOREIGN KEY (`eirc_account_id`) REFERENCES `eirc_account` (`object_id`),
  CONSTRAINT `fk_service_provider_account__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_service_provider_account__service` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Registry status --
DROP TABLE IF EXISTS `registry_status`;
CREATE TABLE `registry_status` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry status code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Registry type --
DROP TABLE IF EXISTS `registry_type`;
CREATE TABLE `registry_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Import error type --
DROP TABLE IF EXISTS `import_error_type`;
CREATE TABLE `import_error_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Import error type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Registry --
DROP TABLE IF EXISTS `registry`;
CREATE TABLE `registry` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `registry_number` BIGINT(20),
  `type` BIGINT(20) NOT NULL,
  `status` BIGINT(20) NOT NULL,
  `records_count` INTEGER,
  `errors_count` INTEGER default -1 NOT NULL comment 'Cached errors number value, -1 is not init',
  `creation_date` DATETIME,
  `from_date` DATETIME,
  `till_date` DATETIME,
  `load_date` DATETIME,
  `sender_organization_id` BIGINT(20),
  `recipient_organization_id` BIGINT(20),
  `amount` decimal(19,2),
  `import_error_type` BIGINT(20),
  PRIMARY KEY (id),
  CONSTRAINT `fk_registry__registry_type` FOREIGN KEY (`type`) REFERENCES `registry_type` (`code`),
  CONSTRAINT `fk_registry__registry_status` FOREIGN KEY (`status`) REFERENCES `registry_status` (`code`),
  CONSTRAINT `fk_registry__sender_organization` FOREIGN KEY (`sender_organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_registry__recipient_organization` FOREIGN KEY (`recipient_organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_registry__import_error_type` FOREIGN KEY (`import_error_type`) REFERENCES `import_error_type` (`code`)
);

-- Container type --
DROP TABLE IF EXISTS `container_type`;
CREATE TABLE `container_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry container type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Registry containers --
DROP TABLE IF EXISTS `registry_container`;
CREATE TABLE `registry_container` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `data` VARCHAR(2048) NOT NULL comment 'Registry container data',
  `type` BIGINT(20) NOT NULL,
  `registry_id` BIGINT(20) NOT NULL comment 'Registry reference',
  PRIMARY KEY (id),
  CONSTRAINT `fk_registry_container__container_type` FOREIGN KEY (`type`) REFERENCES `container_type` (`code`),
  CONSTRAINT `fk_registry_container__registry` FOREIGN KEY (`registry_id`) REFERENCES `registry` (`id`)
);

-- Type of registry file --
DROP TABLE IF EXISTS `registry_file_type`;
CREATE TABLE `registry_file_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE,
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Registry files --
DROP TABLE IF EXISTS `registry_file`;
CREATE TABLE `registry_file` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `registry_id` BIGINT(20) NOT NULL,
  `name_on_server` VARCHAR(255) NOT NULL comment 'File name on flexpay server',
  `original_name` VARCHAR(255) NOT NULL comment 'Original file name',
  `description` VARCHAR(255) comment 'File description',
  `creation_date` DATETIME NOT NULL comment 'File creation date',
  `user_id` BIGINT(20)NOT NULL comment 'User ID who create this file',
  `size` BIGINT(20) comment 'File size',
  `registry_file_type` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_registry_id__registry_file_type` (`registry_id`, `registry_file_type`),
  CONSTRAINT `fk_registry_file__registry` FOREIGN KEY (`registry_id`) REFERENCES `registry` (`id`),
  CONSTRAINT `fk_registry_file__registry_file_type` FOREIGN KEY (`registry_file_type`) REFERENCES `registry_file_type` (`code`)
);

-- Registry record status --
DROP TABLE IF EXISTS `registry_record_status`;
CREATE TABLE `registry_record_status` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry record status code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
);

-- Registry record --
DROP TABLE IF EXISTS `registry_record`;
CREATE TABLE `registry_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_code` VARCHAR(255) NOT NULL,
  `personal_account_ext` VARCHAR(255) NOT NULL,
  `town_type` VARCHAR(255),
  `town_name` VARCHAR(255),
  `street_type` VARCHAR(255),
  `street_name` VARCHAR(255),
  `building_number` VARCHAR(255),
  `bulk_number` VARCHAR(255),
  `apartment_number` VARCHAR(255),
  `first_name` VARCHAR(255),
  `middle_name` VARCHAR(255),
  `last_name` VARCHAR(255),
  `operation_date` DATETIME NOT NULL,
  `unique_operation_number` BIGINT(20),
  `amount` decimal(19,2),
  `status` BIGINT(20) NOT NULL comment 'Record status reference',
  `import_error_type` BIGINT(20) comment 'Import error type from import error',
  `registry_id` BIGINT(20) NOT NULL comment 'Registry reference',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_registry_record__registry_record_status` FOREIGN KEY (`status`) REFERENCES `registry_record_status` (`code`),
  CONSTRAINT `fk_registry_record__import_error_type` FOREIGN KEY (`import_error_type`) REFERENCES `import_error_type` (`code`),
  CONSTRAINT `fk_registry_record__registry` FOREIGN KEY (`registry_id`) REFERENCES `registry` (`id`)
);

-- Registry record containers --
DROP TABLE IF EXISTS `registry_record_container`;
CREATE TABLE `registry_record_container` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `data` VARCHAR(2048) NOT NULL comment 'Container data',
  `type` BIGINT(20) NOT NULL,
  `record_id` BIGINT(20) NOT NULL comment 'Registry record reference',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_registry_record_container__container_type` FOREIGN KEY (`type`) REFERENCES `container_type` (`code`),
  CONSTRAINT `fk_registry_record_container__registry` FOREIGN KEY (`record_id`) REFERENCES `registry_record` (`id`)
);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;