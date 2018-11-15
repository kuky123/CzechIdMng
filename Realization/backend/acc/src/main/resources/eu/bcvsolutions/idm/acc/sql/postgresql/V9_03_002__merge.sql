--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- Add entity for controlled and historic values (for provisioning merge)

CREATE TABLE sys_attribute_contr_value (
	id bytea NOT NULL,
	created timestamp NOT NULL,
	creator varchar(255) NOT NULL,
	creator_id bytea NULL,
	modified timestamp NULL,
	modifier varchar(255) NULL,
	modifier_id bytea NULL,
	original_creator varchar(255) NULL,
	original_creator_id bytea NULL,
	original_modifier varchar(255) NULL,
	original_modifier_id bytea NULL,
	realm_id bytea NULL,
	transaction_id bytea NULL,
	historic_value bool NOT NULL,
	value bytea NULL,
	attribute_id bytea NOT NULL,
	CONSTRAINT sys_attribute_contr_value_pkey PRIMARY KEY (id)
);

CREATE TABLE sys_attribute_contr_value_a (
	id bytea NOT NULL,
	rev int8 NOT NULL,
	revtype int2 NULL,
	created timestamp NULL,
	created_m bool NULL,
	creator varchar(255) NULL,
	creator_m bool NULL,
	creator_id bytea NULL,
	creator_id_m bool NULL,
	modifier varchar(255) NULL,
	modifier_m bool NULL,
	modifier_id bytea NULL,
	modifier_id_m bool NULL,
	original_creator varchar(255) NULL,
	original_creator_m bool NULL,
	original_creator_id bytea NULL,
	original_creator_id_m bool NULL,
	original_modifier varchar(255) NULL,
	original_modifier_m bool NULL,
	original_modifier_id bytea NULL,
	original_modifier_id_m bool NULL,
	realm_id bytea NULL,
	realm_id_m bool NULL,
	transaction_id bytea NULL,
	transaction_id_m bool NULL,
	historic_value bool NULL,
	historic_value_m bool NULL,
	value bytea NULL,
	value_m bool NULL,
	attribute_id bytea NULL,
	attribute_mapping_m bool NULL,
	CONSTRAINT sys_attribute_contr_value_a_pkey PRIMARY KEY (id, rev),
	CONSTRAINT fk_fu5i4vhs025v4gpygupurphw2 FOREIGN KEY (rev) REFERENCES idm_audit(id)
);

-- Add evict_contr_values_cache column
ALTER TABLE sys_system_attribute_mapping ADD COLUMN evict_contr_values_cache boolean;
ALTER TABLE sys_system_attribute_mapping_a ADD COLUMN evict_contr_values_cache boolean;
ALTER TABLE sys_system_attribute_mapping_a ADD COLUMN evict_controlled_values_cache_m boolean;
-- Set evict_contr_values_cache to true for all attributes (I cannot update only MERGE attributes, because some attributes could have set different strategy )
update	sys_system_attribute_mapping set evict_contr_values_cache = true;
ALTER TABLE sys_system_attribute_mapping ALTER COLUMN evict_contr_values_cache SET NOT NULL;
