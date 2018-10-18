--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add recipients (alias) to notification configuration

ALTER TABLE idm_notification_configuration ADD COLUMN redirect boolean NOT NULL DEFAULT false;
ALTER TABLE idm_notification_configuration_a ADD COLUMN redirect boolean;
ALTER TABLE idm_notification_configuration_a ADD COLUMN redirect_m boolean;

ALTER TABLE idm_notification_configuration ADD COLUMN recipients varchar(2000);
ALTER TABLE idm_notification_configuration_a ADD COLUMN recipients varchar(2000);
ALTER TABLE idm_notification_configuration_a ADD COLUMN recipients_m boolean;