--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- Add new column to sync identities configuration "Behavior of the default role for inactive identities"

ALTER TABLE sys_sync_identity_config ADD COLUMN inactive_owner_behavior character varying(255);
UPDATE sys_sync_identity_config SET inactive_owner_behavior = 'LINK' WHERE inactive_owner_behavior IS null AND default_role_id IS NOT null;

ALTER TABLE sys_sync_identity_config_a ADD COLUMN inactive_owner_behavior character varying(255);
ALTER TABLE sys_sync_identity_config_a ADD COLUMN inactive_owner_behavior_m boolean;
