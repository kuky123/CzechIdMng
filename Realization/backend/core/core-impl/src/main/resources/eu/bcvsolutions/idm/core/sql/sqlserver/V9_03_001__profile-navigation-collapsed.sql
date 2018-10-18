--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add navigation collapsed into profile

ALTER TABLE idm_profile ADD navigation_collapsed bit NOT NULL DEFAULT 0;
ALTER TABLE idm_profile_a ADD navigation_collapsed bit;
ALTER TABLE idm_profile_a ADD navigation_collapsed_m bit;