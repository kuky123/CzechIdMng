--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add recipients (alias) to notification configuration

ALTER TABLE idm_notification_configuration ADD redirect bit NOT NULL DEFAULT 0;
ALTER TABLE idm_notification_configuration_a ADD redirect bit;
ALTER TABLE idm_notification_configuration_a ADD redirect_m bit;

ALTER TABLE idm_notification_configuration ADD recipients NVARCHAR(2000);
ALTER TABLE idm_notification_configuration_a ADD recipients NVARCHAR(2000);
ALTER TABLE idm_notification_configuration_a ADD recipients_m bit;