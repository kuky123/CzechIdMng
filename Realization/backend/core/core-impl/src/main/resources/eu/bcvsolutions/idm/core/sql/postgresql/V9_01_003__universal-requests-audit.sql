--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add audit universal request agenda


-- IdmRequest
ALTER TABLE idm_request_a ADD COLUMN owner_id bytea;
ALTER TABLE idm_request_a ADD COLUMN owner_id_m boolean;
ALTER TABLE idm_request_a ADD COLUMN owner_type character varying(255);
ALTER TABLE idm_request_a ADD COLUMN owner_type_m boolean;

-- IdmRequestItem
ALTER TABLE idm_request_item_a ADD COLUMN data text;
ALTER TABLE idm_request_item_a ADD COLUMN data_m boolean;
ALTER TABLE idm_request_item_a ADD COLUMN owner_id bytea;
ALTER TABLE idm_request_item_a ADD COLUMN owner_id_m boolean;
ALTER TABLE idm_request_item_a ADD COLUMN owner_type character varying(255);
ALTER TABLE idm_request_item_a ADD COLUMN owner_type_m boolean;
ALTER TABLE idm_request_item_a ADD COLUMN super_owner_id bytea;
ALTER TABLE idm_request_item_a ADD COLUMN super_owner_id_m boolean;
ALTER TABLE idm_request_item_a ADD COLUMN super_owner_type character varying(255);
ALTER TABLE idm_request_item_a ADD COLUMN super_owner_type_m boolean;

