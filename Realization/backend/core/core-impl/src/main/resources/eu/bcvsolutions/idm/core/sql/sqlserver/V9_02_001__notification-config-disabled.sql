--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add disabled column to idm_notification_configuration

ALTER TABLE idm_notification_configuration ADD disabled bit NOT NULL DEFAULT 0;
ALTER TABLE idm_notification_configuration_a ADD disabled bit;
ALTER TABLE idm_notification_configuration_a ADD disabled_m bit;