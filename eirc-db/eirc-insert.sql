INSERT INTO `sequence` (`sequence_name`, `sequence_value`) VALUES
('eirc_account',1), ('service_provider_account', 1);

-- --------------------------------
-- Organization type
-- --------------------------------
INSERT INTO `organization_type`(`object_id`) VALUES (2);
INSERT INTO `organization_type_string_culture`(`id`, `locale_id`, `value`) VALUES (2, 1, UPPER('Поставщик услуг')), (2, 2,UPPER('Постачальник послуг'));
INSERT INTO `organization_type_attribute`(`attribute_id`, `object_id`, `attribute_type_id`, `value_id`, `value_type_id`) VALUES (1,2,2300,2,2300);

-- --------------------------------
-- Organization
-- --------------------------------

-- Reference to jdbc data source. It is calculation center only attribute. --
INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (914, 1, UPPER('КПП')), (914, 2, UPPER('КПП'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (913, 900, 1, 914, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (913, 913, UPPER('string'));

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (915, 1, UPPER('ИНН')), (915, 2, UPPER('ІПН'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (914, 900, 1, 915, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (914, 914, UPPER('string'));

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (916, 1, UPPER('Примечание')), (916, 2, UPPER('Примітка'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (915, 900, 1, 916, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (915, 915, UPPER('string'));

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (917, 1, UPPER('Юридический адрес')), (917, 2, UPPER('Юридична адреса'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (916, 900, 1, 917, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (916, 916, UPPER('string'));

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (918, 1, UPPER('Почтовый адрес')), (918, 2, UPPER('Поштова адреса'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (917, 900, 1, 918, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (917, 917, UPPER('string'));

INSERT INTO `string_culture`(`id`, `locale_id`, `value`) VALUES (919, 1, UPPER('E-mail')), (919, 2, UPPER('E-mail'));
INSERT INTO `entity_attribute_type`(`id`, `entity_id`, `mandatory`, `attribute_type_name_id`, `system`) VALUES (918, 900, 1, 919, 1);
INSERT INTO `entity_attribute_value_type`(`id`, `attribute_type_id`, `attribute_value_type`) VALUES (918, 918, UPPER('string'));

INSERT INTO `registry_status` (`code`, `name`) values
    (0, 'Разбирается'),
    (1, 'Разобран'),
    (2, 'Загрузка отменена'),
    (3, 'Загружен с ошибками'),
    (4, 'Обработка запущенна'),
    (5, 'Обрабатывается'),
    (6, 'Выполняется импорт абонентов'),
    (7, 'Обрабатывается с ошибками'),
    (8, 'Выполняется импорт абонентов с ошибками'),
    (9, 'Обработан'),
    (10, 'Абоненты загружены'),
    (11, 'Абоненты загружены с ошибками'),
    (12, 'Обработан с ошибками'),
    (13, 'Обработка отменена'),
    (14, 'Откатывается'),
    (15, 'Откачен'),
    (16, 'Создается'),
    (17, 'Создан'),
    (18, 'Создание отменено')
;

INSERT INTO `registry_type` (`code`, `name`) values
    (0, 'Неизвестный'),
    (1, 'Сальдо'),
    (2, 'Начисление'),
    (3, 'Извещение'),
    (4, 'Счета на закрытие'),
    (5, 'Информационный'),
    (6, 'Корректировки'),
    (7, 'Наличные оплаты'),
    (8, 'Безналичные оплаты'),
    (9, 'Возвраты платежей'),
    (10, 'Ошибки'),
    (11, 'Квитанции'),
    (12, 'Оплаты банка')
;

INSERT INTO `registry_record_status` (`code`, `name`) values
    (1, 'Загружена'),
    (2, 'Обработана с ошибкой'),
    (3, 'Исправлена'),
    (4, 'Обработана')
;

INSERT INTO `container_type` (`code`, `name`) values
    (0,''),
    (1,''),
    (2,''),
    (3,''),
    (4,''),
    (5,''),
    (6,''),
    (7,''),
    (8,''),
    (9,''),
    (10,''),
    (11,''),
    (12,''),
    (14,''),
    (15,''),

    (50,''),
    (52,''),

    (100,''),

    (150,''),


    (500,''),
    (501,''),
    (502,''),
    (503,''),

    (600,''),
    (601,''),
    (602,''),
    (603,''),
    (604,'')
;

-- Current database version
 INSERT INTO `update` (`version`) VALUE ('20130905_41_0.0.1');