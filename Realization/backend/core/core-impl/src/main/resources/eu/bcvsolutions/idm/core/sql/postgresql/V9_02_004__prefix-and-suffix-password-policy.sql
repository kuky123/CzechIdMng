--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- prefix and suffix for password policy

ALTER TABLE idm_password_policy ADD COLUMN prefix varchar(255);
ALTER TABLE idm_password_policy_a ADD COLUMN prefix varchar(255);
ALTER TABLE idm_password_policy_a ADD COLUMN prefix_m boolean;

ALTER TABLE idm_password_policy ADD COLUMN suffix varchar(255);
ALTER TABLE idm_password_policy_a ADD COLUMN suffix varchar(255);
ALTER TABLE idm_password_policy_a ADD COLUMN suffix_m boolean;