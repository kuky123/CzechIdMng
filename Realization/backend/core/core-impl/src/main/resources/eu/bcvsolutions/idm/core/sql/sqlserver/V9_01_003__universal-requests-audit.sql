--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add audit universal request agenda


-- IdmRequest
ALTER TABLE idm_request_a ADD owner_id binary(16);
ALTER TABLE idm_request_a ADD owner_id_m bit;
ALTER TABLE idm_request_a ADD owner_type nvarchar(255);
ALTER TABLE idm_request_a ADD owner_type_m bit;

-- IdmRequestItem
ALTER TABLE idm_request_item_a ADD data nvarchar(MAX);
ALTER TABLE idm_request_item_a ADD data_m bit;
ALTER TABLE idm_request_item_a ADD owner_id binary(16);
ALTER TABLE idm_request_item_a ADD owner_id_m bit;
ALTER TABLE idm_request_item_a ADD owner_type nvarchar(255);
ALTER TABLE idm_request_item_a ADD owner_type_m bit;
ALTER TABLE idm_request_item_a ADD super_owner_id binary(16);
ALTER TABLE idm_request_item_a ADD super_owner_id_m bit;
ALTER TABLE idm_request_item_a ADD super_owner_type nvarchar(255);
ALTER TABLE idm_request_item_a ADD super_owner_type_m bit;

