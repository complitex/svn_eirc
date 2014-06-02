-- --------------------------------
-- Locale
-- --------------------------------

INSERT INTO `locales`(`id`, `locale`, `system`) VALUES (1, 'ru', 1);
INSERT INTO `locales`(`id`, `locale`, `system`) VALUES (2, 'uk', 0);

-- --------------------------------
-- Sequence
-- --------------------------------

INSERT INTO `sequence` (`sequence_name`, `sequence_value`) VALUES
('string_culture',1),
('organization',1), ('organization_string_culture',1),
('organization_type',1), ('organization_type_string_culture',1),
('user_info', 3), ('user_info_string_culture', 1);

-- Permission
INSERT INTO `permission` (`permission_id`, `table`, `entity`, `object_id`) VALUES (0, 'ALL', 'ALL', 0);
INSERT INTO `sequence` (`sequence_name`, `sequence_value`) VALUES ('permission', 1);

-- --------------------------------
-- User
-- --------------------------------

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (1000, 1, 'Пользователь'), (1000, 2, 'Користувач');
INSERT INTO `entity`(`id`, `entity_table`, `entity_name_id`, `strategy_factory`) VALUES (1000, 'user_info', 1000, '');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (1001, 1, UPPER('Фамилия')), (1001, 2, UPPER('Прізвище'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (1000, 1000, 1, 1001, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (1000, 1000, 'last_name');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (1002, 1, UPPER('Имя')), (1002, 2, UPPER('Ім\'я'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (1001, 1000, 1, 1002, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (1001, 1001, 'first_name');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (1003, 1, UPPER('Отчество')), (1003, 2, UPPER('По батькові'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (1002, 1000, 1, 1003, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (1002, 1002, 'middle_name');

-- admin user --
INSERT INTO `user_info` (`object_id`) VALUES (1);
INSERT INTO `first_name`(`id`, `name`) VALUES (1,'admin');
INSERT INTO `middle_name`(`id`, `name`) VALUES (1,'admin');
INSERT INTO `last_name`(`id`, `name`) VALUES (1,'admin');
INSERT INTO `user_info_attribute` (`attribute_id`, `object_id`, `attribute_type_id`, `value_id`, `value_type_id`)
    VALUES (1,1,1000,1,1000), (1,1,1001,1,1001), (1,1,1002,1,1002);
INSERT INTO `user` (`id`, `login`, `password`, `user_info_object_id`) VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1);
INSERT INTO `usergroup` (`id`, `login`, `group_name`) VALUES (1, 'admin', 'ADMINISTRATORS');
INSERT INTO `usergroup` (`id`, `login`, `group_name`) VALUES (2, 'admin', 'EMPLOYEES');
INSERT INTO `usergroup` (`id`, `login`, `group_name`) VALUES (3, 'admin', 'EMPLOYEES_CHILD_VIEW');

-- anonymous user --
INSERT INTO `user_info` (`object_id`) VALUES (2);
INSERT INTO `first_name`(`id`, `name`) VALUES (2,'ANONYMOUS');
INSERT INTO `middle_name`(`id`, `name`) VALUES (2,'ANONYMOUS');
INSERT INTO `last_name`(`id`, `name`) VALUES (2,'ANONYMOUS');
INSERT INTO `user_info_attribute` (`attribute_id`, `object_id`, `attribute_type_id`, `value_id`, `value_type_id`)
    VALUES (1,2,1000,2,1000), (1,2,1001,2,1001), (1,2,1002,2,1002);
INSERT INTO `user` (`id`, `login`, `password`, `user_info_object_id`)  VALUES (2, 'ANONYMOUS', 'ANONYMOUS', 2);

-- --------------------------------
-- Organization type
-- --------------------------------

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (2300, 1, 'Тип организации'), (2300, 2, 'Тип организации');
INSERT INTO `entity`(`id`, `entity_table`, `entity_name_id`, `strategy_factory`) VALUES (2300, 'organization_type', 2300, '');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (2301, 1, UPPER('Тип организации')), (2301, 2, UPPER('Тип организации'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (2300, 2300, 1, 2301, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (2300, 2300, UPPER('string_culture'));

INSERT INTO `organization_type`(`object_id`) VALUES (1);
INSERT INTO `organization_type_string_culture`(`id`, `locale_id`, `value`) VALUES (1, 1, UPPER('Организации пользователей')), (1, 2,UPPER('Организации пользователей'));
INSERT INTO `organization_type_attribute`(`attribute_id`, `object_id`, `attribute_type_id`, `value_id`, `value_type_id`) VALUES (1,1,2300,1,2300);

-- --------------------------------
-- Organization
-- --------------------------------

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (900, 1, 'Организация'), (900, 2, 'Організація');
INSERT INTO `entity`(`id`, `entity_table`, `entity_name_id`, `strategy_factory`) VALUES (900, 'organization', 900, '');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (901, 1, UPPER('Наименование организации')), (901, 2, UPPER('Найменування організації'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (900, 900, 1, 901, 1);
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (902, 1, UPPER('Уникальный код организации')), (902, 2, UPPER('Унікальний код організації'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (901, 900, 1, 902, 1);
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (904, 1, UPPER('Принадлежит')), (904, 2, UPPER('Принадлежит'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (903, 900, 0, 904, 1);
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (905, 1, UPPER('Тип организации')), (905, 2, UPPER('Тип организации'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (904, 900, 0, 905, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (900, 900, UPPER('string_culture'));
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (901, 901, UPPER('string'));
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (903, 903, 'organization');
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (904, 904, 'organization_type');
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (906, 1, UPPER('Короткое наименование')), (906, 2, UPPER('Короткое наименование'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (906, 900, 0, 906, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (906, 906, UPPER('string_culture'));

-- Current database version
INSERT INTO `update` (`version`) VALUE ('20140602_187_0.0.2');