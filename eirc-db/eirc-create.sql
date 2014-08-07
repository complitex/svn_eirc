/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- EIRC account --
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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Service --
DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(128),
  `parent_id` BIGINT(20),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_parent_service` FOREIGN KEY (`parent_id`) REFERENCES `service` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;


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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

DROP TABLE IF EXISTS `service_provider_account_attribute`;

CREATE TABLE `service_provider_account_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения: 100 - STRING_CULTURE',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_sp_account_attribute__sp_account` FOREIGN KEY (`object_id`) REFERENCES `service_provider_account`(`object_id`),
  CONSTRAINT `fk_sp_account_attribute__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_sp_account_attribute__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Дополнительные атрибуты л/с ПУ (кол-во проживающих и т.п.)';

DROP TABLE IF EXISTS `service_provider_account_string_culture`;

CREATE TABLE `service_provider_account_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_sp_account_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов л/с ПУ';

DROP TABLE IF EXISTS `owner_exemption`;
CREATE TABLE `owner_exemption` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `object_id` BIGINT(20) NOT NULL,
  `service_provider_account_id` BIGINT(20) NOT NULL,
  `first_name` VARCHAR(255),
  `last_name` VARCHAR(255),
  `middle_name` VARCHAR(255),
  `inn` VARCHAR(255),
  `begin_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_date` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`begin_date`),
  KEY `key_object_id` (object_id),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  CONSTRAINT `fk_owner_exemption__sp_account` FOREIGN KEY (`service_provider_account_id`) REFERENCES `service_provider_account` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Носители льготы';

DROP TABLE IF EXISTS `exemption`;
CREATE TABLE `exemption` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `object_id` BIGINT(20) NOT NULL,
  `owner_exemption_id` BIGINT(20) NOT NULL,
  `category` VARCHAR(255),
  `number_using` INTEGER,
  `begin_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_date` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`begin_date`),
  KEY `key_object_id` (object_id),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  CONSTRAINT `fk_exemption__owner_exemption` FOREIGN KEY (`owner_exemption_id`) REFERENCES `owner_exemption` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Льготы';

DROP TABLE IF EXISTS `saldo_out`;
CREATE TABLE `saldo_out` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_provider_account_id` BIGINT(20) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `date_formation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `saldo_out_unique_sp_account__date_formation` (`service_provider_account_id`,`date_formation`),
  CONSTRAINT `fk_saldo_out__sp_account` FOREIGN KEY (`service_provider_account_id`) REFERENCES `service_provider_account` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Исходящее сальдо';

DROP TABLE IF EXISTS `charge`;
CREATE TABLE `charge` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_provider_account_id` BIGINT(20) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `date_formation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `charge_unique_sp_account__date_formation` (`service_provider_account_id`,`date_formation`),
  CONSTRAINT `fk_charge__sp_account` FOREIGN KEY (`service_provider_account_id`) REFERENCES `service_provider_account` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Начисление';

DROP TABLE IF EXISTS `cash_payment`;
CREATE TABLE `cash_payment` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_provider_account_id` BIGINT(20) NOT NULL,
  `payment_collector_id` BIGINT(20) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `number_quittance` VARCHAR(20),
  `date_formation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  KEY `cash_payment_sp_account__date_formation` (`service_provider_account_id`, `date_formation`),
  CONSTRAINT `fk_cash_payment__organization` FOREIGN KEY (`payment_collector_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_cash_payment__sp_account` FOREIGN KEY (`service_provider_account_id`) REFERENCES `service_provider_account` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Наличные оплаты';

DROP TABLE IF EXISTS `cashless_payment`;
CREATE TABLE `cashless_payment` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_provider_account_id` BIGINT(20) NOT NULL,
  `payment_collector_id` BIGINT(20) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `number_quittance` VARCHAR(20),
  `date_formation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  KEY `cashless_payment_sp_account__date_formation_formation` (`service_provider_account_id`, `date_formation`),
  CONSTRAINT `fk_cashless_payment__organization` FOREIGN KEY (`payment_collector_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_cashless_payment__sp_account` FOREIGN KEY (`service_provider_account_id`) REFERENCES `service_provider_account` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Безналичные оплаты';

-- Registry status --
DROP TABLE IF EXISTS `registry_status`;
CREATE TABLE `registry_status` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry status code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Registry type --
DROP TABLE IF EXISTS `registry_type`;
CREATE TABLE `registry_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Import error type --
DROP TABLE IF EXISTS `import_error_type`;
CREATE TABLE `import_error_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Import error type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Registry --
DROP TABLE IF EXISTS `registry`;
CREATE TABLE `registry` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `registry_number` BIGINT(20),
  `type` BIGINT(20) NOT NULL,
  `status` BIGINT(20),
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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Container type --
DROP TABLE IF EXISTS `container_type`;
CREATE TABLE `container_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry container type code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Type of registry file --
DROP TABLE IF EXISTS `registry_file_type`;
CREATE TABLE `registry_file_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE,
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Registry record status --
DROP TABLE IF EXISTS `registry_record_status`;
CREATE TABLE `registry_record_status` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `code` BIGINT(20) NOT NULL UNIQUE comment 'Registry record status code',
  `name` VARCHAR(255),
  PRIMARY KEY (`pk_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Registry record --
DROP TABLE IF EXISTS `registry_record`;
CREATE TABLE `registry_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `service_code` VARCHAR(255) NOT NULL,
  `personal_account_ext` VARCHAR(255) NOT NULL,
  `city_type` VARCHAR(255),
  `city` VARCHAR(255),
  `street_type` VARCHAR(255),
  `street` VARCHAR(255),
  `building_number` VARCHAR(255),
  `bulk_number` VARCHAR(255),
  `apartment_number` VARCHAR(255),
  `room_number` VARCHAR(255),
  `first_name` VARCHAR(255),
  `middle_name` VARCHAR(255),
  `last_name` VARCHAR(255),
  `operation_date` DATETIME NOT NULL,
  `unique_operation_number` BIGINT(20),
  `amount` decimal(19,2),
  `city_type_id` BIGINT(20),
  `city_id` BIGINT(20),
  `street_type_id` BIGINT(20),
  `street_id` BIGINT(20),
  `building_id` BIGINT(20),
  `apartment_id` BIGINT(20),
  `room_id` BIGINT(20),
  `status` BIGINT(20) comment 'Record status reference',
  `import_error_type` BIGINT(20) comment 'Import error type from import error',
  `registry_id` BIGINT(20) NOT NULL comment 'Registry reference',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_registry_record__registry_record_status` FOREIGN KEY (`status`) REFERENCES `registry_record_status` (`code`),
  CONSTRAINT `fk_registry_record__import_error_type` FOREIGN KEY (`import_error_type`) REFERENCES `import_error_type` (`code`),
  INDEX `idx_registry_record__status_error`  (`status`, `import_error_type`, `registry_id`),
  INDEX `idx_registry_record__status`  (`status`, `registry_id`),
  CONSTRAINT `fk_registry_record__registry` FOREIGN KEY (`registry_id`) REFERENCES `registry` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ------------------------------
-- Corrections
-- ------------------------------
DROP TABLE IF EXISTS `city_correction`;

CREATE TABLE `city_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта населенного пункта',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Название населенного пункта',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_city_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_city_correction__city` FOREIGN KEY (`object_id`) REFERENCES `city` (`object_id`),
  CONSTRAINT `fk_city_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT 'Коррекция населенного пункта';

DROP TABLE IF EXISTS `city_type_correction`;

CREATE TABLE `city_type_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта типа населенного пункта',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Название типа населенного пункта',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_city_type__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_city_type_correction__city_type` FOREIGN KEY (`object_id`) REFERENCES `city_type` (`object_id`),
  CONSTRAINT `fk_city_type_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT  CHARSET=utf8 COMMENT 'Коррекция типа населенного пункта';

DROP TABLE IF EXISTS `district_correction`;

CREATE TABLE `district_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `city_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта населенного пункта',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта района',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Название типа населенного пункта',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_city_object_id` (`city_object_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_district_correction__city` FOREIGN KEY (`city_object_id`) REFERENCES `city` (`object_id`),
  CONSTRAINT `fk_district_correction__district` FOREIGN KEY (`object_id`) REFERENCES `district` (`object_id`),
  CONSTRAINT `fk_district_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_district_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT 'Коррекция района';

DROP TABLE IF EXISTS `street_correction`;

CREATE TABLE `street_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `city_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта населенного пункта',
  `street_type_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта типа улицы',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта улицы',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Название типа населенного пункта',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_city_object_id` (`city_object_id`),
  KEY `key_street_type_object_id` (`street_type_object_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_street_correction__city` FOREIGN KEY (`city_object_id`) REFERENCES `city` (`object_id`),
  CONSTRAINT `fk_street_correction__street_type` FOREIGN KEY (`street_type_object_id`) REFERENCES `street_type` (`object_id`),
  CONSTRAINT `fk_street_correction__street` FOREIGN KEY (`object_id`) REFERENCES `street` (`object_id`),
  CONSTRAINT `fk_street_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_street_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT 'Коррекция улицы';

DROP TABLE IF EXISTS `street_type_correction`;

CREATE TABLE `street_type_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта типа населенного пункта',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Название типа населенного пункта',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_street_type__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_street_type_correction__street_type` FOREIGN KEY (`object_id`) REFERENCES `street_type` (`object_id`),
  CONSTRAINT `fk_street_type_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT 'Коррекция типа улицы';

DROP TABLE IF EXISTS `building_correction`;

CREATE TABLE `building_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `street_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта улица',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта дом',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Номер дома',
  `correction_corp` VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'Корпус дома',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_street_object_id` (`street_object_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_building_correction__street` FOREIGN KEY (`street_object_id`) REFERENCES `street` (`object_id`),
  CONSTRAINT `fk_building_correction__building` FOREIGN KEY (`object_id`) REFERENCES `building` (`object_id`),
  CONSTRAINT `fk_building_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_building_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT  CHARSET=utf8 COMMENT 'Коррекция дома';

DROP TABLE IF EXISTS `apartment_correction`;

CREATE TABLE `apartment_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `building_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта дом',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта квартира',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Номер квартиры',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_building_object_id` (`building_object_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_apartment_correction__building` FOREIGN KEY (`building_object_id`) REFERENCES `building` (`object_id`),
  CONSTRAINT `fk_apartment_correction__apartment` FOREIGN KEY (`object_id`) REFERENCES `apartment` (`object_id`),
  CONSTRAINT `fk_apartment_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_apartment_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT  CHARSET=utf8 COMMENT 'Коррекция квартиры';

DROP TABLE IF EXISTS `room_correction`;

CREATE TABLE `room_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `building_object_id` BIGINT(20) COMMENT 'Идентификатор объекта дом',
  `apartment_object_id` BIGINT(20) COMMENT 'Идентификатор объекта квартира',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта комната',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор объекта',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Номер комнаты',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_building_object_id` (`building_object_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_room_correction__building` FOREIGN KEY (`building_object_id`) REFERENCES `building` (`object_id`),
  CONSTRAINT `fk_room_correction__apartment` FOREIGN KEY (`apartment_object_id`) REFERENCES `apartment` (`object_id`),
  CONSTRAINT `fk_room_correction__room` FOREIGN KEY (`object_id`) REFERENCES `room` (`object_id`),
  CONSTRAINT `fk_room_correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_room_correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB DEFAULT  CHARSET=utf8 COMMENT 'Коррекция комнаты';

-- ------------------------------
-- Organization Correction
-- ------------------------------

DROP TABLE IF EXISTS `organization_correction`;

CREATE TABLE `organization_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта организация',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор организации',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Код организации',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_organization__correction__organization_object` FOREIGN KEY (`object_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_organization__correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_organization__correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Коррекция организации';

-- ------------------------------
-- Service Correction
-- ------------------------------

DROP TABLE IF EXISTS `service_correction`;

CREATE TABLE `service_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта услуга',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор услуги',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Код организации',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_service__correction__service_object` FOREIGN KEY (`object_id`) REFERENCES `service` (`id`),
  CONSTRAINT `fk_service__correction__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_service__correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Коррекция услуги';

-- ------------------------------
-- Service provider account Correction
-- ------------------------------

DROP TABLE IF EXISTS `service_provider_account_correction`;

CREATE TABLE `service_provider_account_correction` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор коррекции',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта л/с ПУ',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор л/с ПУ',
  `correction` VARCHAR(100) NOT NULL COMMENT 'Код организации',
  `begin_date` DATE NOT NULL DEFAULT '1970-01-01' COMMENT 'Дата начала актуальности соответствия',
  `end_date` DATE NOT NULL DEFAULT '2054-12-31' COMMENT 'Дата окончания актуальности соответствия',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `user_organization_id` BIGINT(20),
  `module_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор модуля',
  `status` INTEGER COMMENT 'Статус',
  PRIMARY KEY (`id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_correction` (`correction`),
  KEY `key_begin_date` (`begin_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_organization_id` (`organization_id`),
  KEY `key_user_organization_id` (`user_organization_id`),
  KEY `key_module_id` (`module_id`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_sp_account__correction__sp_account_object` FOREIGN KEY (`object_id`) REFERENCES `service_provider_account` (`object_id`),
  CONSTRAINT `fk_sp_account__organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`object_id`),
  CONSTRAINT `fk_sp_account__correction__user_organization` FOREIGN KEY (`user_organization_id`) REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Коррекция л/с ПУ';

-- ------------------------------
-- Module instance
-- ------------------------------
DROP TABLE IF EXISTS `module_instance`;

CREATE TABLE `module_instance` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `parent_id` BIGINT(20) COMMENT 'Идентификатор родительского объекта',
  `parent_entity_id` BIGINT(20) COMMENT 'Идентификатор сущности родительского объекта',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия объекта',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия объекта',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус объекта. См. класс StatusType',
  `permission_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'Ключ прав доступа к объекту',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор импорта записи',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`start_date`),
  UNIQUE KEY `unique_external_id` (`external_id`),
  KEY `key_object_id` (object_id),
  KEY `key_parent_id` (`parent_id`),
  KEY `key_parent_entity_id` (`parent_entity_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  KEY `key_permission_id` (`permission_id`),
  CONSTRAINT `fk_module_instance__entity` FOREIGN KEY (`parent_entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_module_instance__permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Инстанс модуля';

DROP TABLE IF EXISTS `module_instance_attribute`;

CREATE TABLE `module_instance_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута: 100 - НАИМЕНОВАНИЕ МОДУЛЯ',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения: 100 - STRING_CULTURE',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_module_instance_attribute__module_instance` FOREIGN KEY (`object_id`) REFERENCES `module_instance`(`object_id`),
  CONSTRAINT `fk_module_instance_attribute__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_module_instance_attribute__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Атрибуты модуля';

DROP TABLE IF EXISTS `module_instance_string_culture`;

CREATE TABLE `module_instance_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_module_instance_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов модуля';

-- ------------------------------
-- Module instance
-- ------------------------------
DROP TABLE IF EXISTS `module_instance_type`;

CREATE TABLE `module_instance_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `parent_id` BIGINT(20) COMMENT 'Идентификатор родительского объекта',
  `parent_entity_id` BIGINT(20) COMMENT 'Идентификатор сущности родительского объекта',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия объекта',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия объекта',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус объекта. См. класс StatusType',
  `permission_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'Ключ прав доступа к объекту',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор импорта записи',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`start_date`),
  UNIQUE KEY `unique_external_id` (`external_id`),
  KEY `key_object_id` (object_id),
  KEY `key_parent_id` (`parent_id`),
  KEY `key_parent_entity_id` (`parent_entity_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  KEY `key_permission_id` (`permission_id`),
  CONSTRAINT `fk_module_instance_type__entity` FOREIGN KEY (`parent_entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_module_instance_type__permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Тип инстанса модуля';

DROP TABLE IF EXISTS `module_instance_type_attribute`;

CREATE TABLE `module_instance_type_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута: 100 - НАИМЕНОВАНИЕ МОДУЛЯ',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения: 100 - STRING_CULTURE',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_module_instance_type_attribute__module_instance_type` FOREIGN KEY (`object_id`) REFERENCES `module_instance_type`(`object_id`),
  CONSTRAINT `fk_module_instance_type_attribute__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_module_instance_type_attribute__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Атрибуты типа модуля';

DROP TABLE IF EXISTS `module_instance_type_string_culture`;

CREATE TABLE `module_instance_type_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_module_instance_type_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов типа модуля';

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;