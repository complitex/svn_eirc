START TRANSACTION;

-- --------------------------------
-- Current database version
-- --------------------------------
INSERT INTO `update` (`version`) VALUE ('20140529_184_0.0.1');

-- --------------------------------
-- Add sequence for registry_number
-- --------------------------------

INSERT INTO `sequence` (`sequence_name`, `sequence_value`) VALUES
  ('registry_number', 1);

COMMIT;