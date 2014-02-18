update sequence set sequence_value = (select IFNULL(max(`id`), 0) from `string_culture`)+1 where sequence_name = 'string_culture';
update sequence set sequence_value = (select IFNULL(max(`id`), 0) from `organization_string_culture`)+1 where sequence_name = 'organization_string_culture';
update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `organization`)+1 where sequence_name = 'organization';
update sequence set sequence_value = (select IFNULL(max(`id`), 0) from `organization_type_string_culture`)+1 where sequence_name = 'organization_type_string_culture';
update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `organization_type`)+1 where sequence_name = 'organization_type';
update sequence set sequence_value = (select IFNULL(max(`id`), 0) from `user_info_string_culture`)+1 where sequence_name = 'user_info_string_culture';
update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `user_info`)+1 where sequence_name = 'user_info';