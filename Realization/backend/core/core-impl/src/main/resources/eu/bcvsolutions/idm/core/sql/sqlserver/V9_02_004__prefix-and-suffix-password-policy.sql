--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- prefix and suffix for password policy

ALTER TABLE idm_password_policy ADD prefix NVARCHAR(255);
ALTER TABLE idm_password_policy_a ADD prefix NVARCHAR(255);
ALTER TABLE idm_password_policy_a ADD prefix_m bit;

ALTER TABLE idm_password_policy ADD suffix NVARCHAR(255);
ALTER TABLE idm_password_policy_a ADD suffix NVARCHAR(255);
ALTER TABLE idm_password_policy_a ADD suffix_m bit;