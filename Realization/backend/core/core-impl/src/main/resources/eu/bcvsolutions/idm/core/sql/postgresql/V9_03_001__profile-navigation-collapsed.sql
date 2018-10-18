--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add navigation collapsed into profile

ALTER TABLE idm_profile ADD COLUMN navigation_collapsed boolean NOT NULL DEFAULT false;
ALTER TABLE idm_profile_a ADD COLUMN navigation_collapsed boolean;
ALTER TABLE idm_profile_a ADD COLUMN navigation_collapsed_m boolean;