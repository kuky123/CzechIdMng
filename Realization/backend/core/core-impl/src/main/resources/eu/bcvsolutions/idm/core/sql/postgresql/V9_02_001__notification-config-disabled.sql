--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add disabled column to idm_notification_configuration

ALTER TABLE idm_notification_configuration ADD COLUMN disabled boolean NOT NULL DEFAULT false;
ALTER TABLE idm_notification_configuration_a ADD COLUMN disabled boolean;
ALTER TABLE idm_notification_configuration_a ADD COLUMN disabled_m boolean;