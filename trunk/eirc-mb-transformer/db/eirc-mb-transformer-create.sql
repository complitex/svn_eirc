/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

DROP TABLE IF EXISTS `sequence`;
CREATE TABLE `sequence`(
  `sequence_name` VARCHAR(100) NOT NULL COMMENT 'Название таблицы сущности',
  `sequence_value` bigint UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Значение идентификатора',
  PRIMARY KEY (`sequence_name`)
)ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Последовательность генерации идентификаторов объектов';

DROP TABLE IF EXISTS `string_culture`;
CREATE TABLE `string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`, `locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация';

DROP TABLE IF EXISTS `entity`;

CREATE TABLE `entity` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор сущности',
  `entity_table` VARCHAR(100) NOT NULL COMMENT 'Название таблицы сущности',
  `entity_name_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации названия сущности',
  `strategy_factory` VARCHAR(100) NOT NULL COMMENT 'Не используется',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `unique_entity_table` (`entity_table`),
  KEY `key_entity_name_id` (`entity_name_id`),
  CONSTRAINT `fk_entity__string_culture` FOREIGN KEY (`entity_name_id`) REFERENCES `string_culture` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Сущность';

DROP TABLE IF EXISTS `entity_attribute_type`;

CREATE TABLE `entity_attribute_type` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор типа атрибута',
  `entity_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор сущности',
  `mandatory` TINYINT(1) default 0 NOT NULL COMMENT 'Является ли атрибут обязательным',
  `attribute_type_name_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации названия атрибута',
  `start_date` TIMESTAMP NOT NULL default CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия типа атрибута',
  `end_date` TIMESTAMP NULL default NULL COMMENT 'Дата окончания периода действия типа атрибута',
  `system` TINYINT(1) default 0 NOT NULL COMMENT 'Является ли тип атрибута системным',
  PRIMARY KEY (`id`),
  KEY `key_entity_id` (`entity_id`),
  KEY `key_attribute_type_name_id` (`attribute_type_name_id`),
  CONSTRAINT `fk_entity_attribute_type__entity` FOREIGN KEY (`entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_entity_attribute_type__string_culture` FOREIGN KEY (`attribute_type_name_id`) REFERENCES `string_culture` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Тип атрибута сущности';

DROP TABLE IF EXISTS `entity_attribute_value_type`;

CREATE TABLE `entity_attribute_value_type` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор типа значения атрибута',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута',
  `attribute_value_type` VARCHAR(100) NOT NULL COMMENT 'Тип значения атрибута',
  PRIMARY KEY  (`id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  CONSTRAINT `fk_entity_attribute_value_type` FOREIGN KEY (`attribute_type_id`) REFERENCES `entity_attribute_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Тип значения атрибута';

/* Entities */

-- ------------------------------
-- Organization type
-- ------------------------------
DROP TABLE IF EXISTS `organization_type`;

CREATE TABLE `organization_type` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `parent_id` BIGINT(20) COMMENT 'Идентификатор родительского объекта: не используется',
  `parent_entity_id` BIGINT(20) COMMENT 'Идентификатор сущности родительского объекта: не используется',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия объекта',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия объекта',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  `permission_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'Ключ прав доступа к объекту',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор импорта записи',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`start_date`),
  UNIQUE KEY `unique_external_id` (`external_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_parent_id` (`parent_id`),
  KEY `key_parent_entity_id` (`parent_entity_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  KEY `key_permission_id` (`permission_id`),
  CONSTRAINT `fk_organization_type__entity` FOREIGN KEY (`parent_entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_organization_type__permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Тип организации';

DROP TABLE IF EXISTS `organization_type_attribute`;

CREATE TABLE `organization_type_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута: 2300 - НАИМЕНОВАНИЕ ТИПА ОРГАНИЗАЦИИ',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения атрибута: 2300 - STRING_CULTURE',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_organization_type_attribute__organization` FOREIGN KEY (`object_id`) REFERENCES `organization_type`(`object_id`),
  CONSTRAINT `fk_organization_type_attribute__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_organization_type_attribute__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Атрибуты типа организации';

DROP TABLE IF EXISTS `organization_type_string_culture`;

CREATE TABLE `organization_type_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_organization_type_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов типа организации';

-- ------------------------------
-- Organization
-- ------------------------------
DROP TABLE IF EXISTS `organization`;

CREATE TABLE `organization` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `parent_id` BIGINT(20) COMMENT 'Идентификатор родительского объекта: не используется',
  `parent_entity_id` BIGINT(20) COMMENT 'Идентификатор сущности родительского объекта: не используется',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия объекта',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия объекта',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  `permission_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'Ключ прав доступа к объекту',
  `external_id` VARCHAR(20) COMMENT 'Внешний идентификатор импорта записи',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`start_date`),
  UNIQUE KEY `unique_external_id` (`external_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_parent_id` (`parent_id`),
  KEY `key_parent_entity_id` (`parent_entity_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  KEY `key_permission_id` (`permission_id`),
  CONSTRAINT `fk_organization__entity` FOREIGN KEY (`parent_entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_organization__permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Организация';

DROP TABLE IF EXISTS `organization_attribute`;

CREATE TABLE `organization_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута: 900 - НАЗВАНИЕ, 901 - УНИКАЛЬНЫЙ КОД, 902 - РАЙОН, 903 - ПРИНАДЛЕЖИТ, 904  - ТИП ОРГАНИЗАЦИИ',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения атрибута: 900 - STRING_CULTURE, 901 - STRING, 902 - district, 903 - organization, 904 - organization_type',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_organization_attribute__organization` FOREIGN KEY (`object_id`) REFERENCES `organization`(`object_id`),
  CONSTRAINT `fk_organization_attribute__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_organization_attribute__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Атрибуты организации';

DROP TABLE IF EXISTS `organization_string_culture`;

CREATE TABLE `organization_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_organization_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов организации';


-- ------------------------------
-- User info
-- ------------------------------
DROP TABLE IF EXISTS `user_info`;

CREATE TABLE `user_info` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `parent_id` BIGINT(20) COMMENT 'Идентификатор родительского объекта: не используется',
  `parent_entity_id` BIGINT(20) COMMENT 'Идентификатор сущности родительского объекта: не используется',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия объекта',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия объекта',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  `permission_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'Ключ прав доступа к объекту',
  `external_id` BIGINT(20) COMMENT 'Внешний идентификатор импорта записи',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_object_id__start_date` (`object_id`,`start_date`),
  UNIQUE KEY `unique_external_id` (`external_id`),
  KEY `key_object_id` (`object_id`),
  KEY `key_parent_id` (`parent_id`),
  KEY `key_parent_entity_id` (`parent_entity_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  KEY `key_permission_id` (`permission_id`),
  CONSTRAINT `fk_user_info__entity` FOREIGN KEY (`parent_entity_id`) REFERENCES `entity` (`id`),
  CONSTRAINT `fk_user_info__permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Информация о пользователе';

DROP TABLE IF EXISTS `user_info_attribute`;

CREATE TABLE `user_info_attribute` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `attribute_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор атрибута',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  `attribute_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа атрибута: 1000 - ФАМИЛИЯ, 1001 - ИМЯ, 1002 - ОТЧЕСТВО',
  `value_id` BIGINT(20) COMMENT 'Идентификатор значения',
  `value_type_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор типа значения атрибута: 1000 - last_name, 1001 - first_name, 1002 - middle_name',
  `start_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата начала периода действия атрибута',
  `end_date` TIMESTAMP NULL DEFAULT NULL COMMENT 'Дата окончания периода действия атрибута',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Статус: ACTIVE, INACTIVE, ARCHIVE',
  PRIMARY KEY  (`pk_id`),
  UNIQUE KEY `unique_id` (`attribute_id`,`object_id`,`attribute_type_id`, `start_date`),
  KEY `key_object_id` (`object_id`),
  KEY `key_attribute_type_id` (`attribute_type_id`),
  KEY `key_value_id` (`value_id`),
  KEY `key_value_type_id` (`value_type_id`),
  KEY `key_start_date` (`start_date`),
  KEY `key_end_date` (`end_date`),
  KEY `key_status` (`status`),
  CONSTRAINT `fk_user_info__user_info` FOREIGN KEY (`object_id`) REFERENCES `user_info`(`object_id`),
  CONSTRAINT `fk_user_info__entity_attribute_type` FOREIGN KEY (`attribute_type_id`)
  REFERENCES `entity_attribute_type` (`id`),
  CONSTRAINT `fk_user_info__entity_attribute_value_type` FOREIGN KEY (`value_type_id`)
  REFERENCES `entity_attribute_value_type` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Атрибуты информации о пользователе';

DROP TABLE IF EXISTS `user_info_string_culture`;

CREATE TABLE `user_info_string_culture` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локализации',
  `locale_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор локали',
  `value` VARCHAR(1000) COMMENT 'Текстовое значение',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `unique_id__locale` (`id`,`locale_id`),
  KEY `key_locale` (`locale_id`),
  KEY `key_value` (`value`(128)),
  CONSTRAINT `fk_user_info_string_culture__locales` FOREIGN KEY (`locale_id`) REFERENCES `locales` (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Локализация атрибутов информации о пользователе';


-- ------------------------------
-- User Organization
-- ------------------------------
DROP TABLE IF EXISTS `user_organization`;

CREATE TABLE `user_organization` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор организации пользователей',
  `user_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор пользователя',
  `organization_object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор организации',
  `main` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Является ли организация основной',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_unique` (`user_id`, `organization_object_id`),
  CONSTRAINT `fk_user_organization__user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_organization__organization` FOREIGN KEY (`organization_object_id`)
  REFERENCES `organization` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Организация пользователей';

-- ------------------------------
-- Permission
-- ------------------------------

DROP TABLE IF EXISTS `permission`;

CREATE TABLE `permission` (
  `pk_id` BIGINT(20) NOT NULL AUTO_INCREMENT  COMMENT 'Суррогатный ключ',
  `permission_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор права доступа',
  `table` VARCHAR(64) NOT NULL COMMENT 'Таблица',
  `entity` VARCHAR(64) NOT NULL COMMENT 'Сущность',
  `object_id` BIGINT(20) NOT NULL COMMENT 'Идентификатор объекта',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `key_unique` (`permission_id`, `entity`, `object_id`),
  KEY `key_permission_id` (`permission_id`),
  KEY `key_table` (`table`),
  KEY `key_entity` (`entity`),
  KEY `key_object_id` (`object_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Права доступа';

-- ------------------------------
-- First Name
-- ------------------------------

DROP TABLE IF EXISTS `first_name`;

CREATE TABLE `first_name` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор имени',
  `name` VARCHAR(100) NOT NULL COMMENT 'Имя',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_name` (`name`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Имя';

-- ------------------------------
-- Middle Name
-- ------------------------------

DROP TABLE IF EXISTS `middle_name`;

CREATE TABLE `middle_name` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор отчества',
  `name` VARCHAR(100) NOT NULL COMMENT 'Отчество',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_name` (`name`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Отчество';

-- ------------------------------
-- Last Name
-- ------------------------------

DROP TABLE IF EXISTS `last_name`;

CREATE TABLE `last_name` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Идентификатор фамилии',
  `name` VARCHAR(100) NOT NULL COMMENT 'Фамилия',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_name` (`name`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Фамилия';

-- ------------------------------
-- Registry number
-- ------------------------------

DROP TABLE IF EXISTS `registry_number`;

CREATE TABLE `registry_number` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Автоинкремент',
  `value` INTEGER NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT 'Используется для генерации номера реестра';

/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;