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
DROP TABLE IF EXISTS `service_string_culture`;
CREATE TABLE `service_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `id` BIGINT(20) NOT NULL,
  `locale_id` BIGINT(20) NOT NULL,
  `value` VARCHAR(1000),
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`),
  CONSTRAINT `fk_service_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(128),
  `parent_id` BIGINT(20),
  `name_id` BIGINT(20),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_parent_service` FOREIGN KEY (`parent_id`) REFERENCES `service` (`id`),
  CONSTRAINT `fk_service__service_string__culture` FOREIGN KEY (`name_id`) REFERENCES `service_string_culture` (`id`)
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


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;