update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `eirc_account`)+1 where sequence_name = 'eirc_account';
update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `service_provider_account`)+1 where sequence_name = 'service_provider_account';
update sequence set sequence_value = (select IFNULL(max(`object_id`), 0) from `module_instance`)+1 where sequence_name = 'module_instance';
