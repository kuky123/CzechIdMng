--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- Entity and audit entity for generate values


CREATE TABLE idm_generate_value (
	id binary(16) NOT NULL,
	created datetime2 NOT NULL,
	creator nvarchar(255) NOT NULL,
	creator_id binary(16),
	modified datetime2,
	modifier nvarchar(255),
	modifier_id binary(16),
	original_creator nvarchar(255),
	original_creator_id binary(16),
	original_modifier nvarchar(255),
	original_modifier_id binary(16),
	realm_id binary(16),
	transaction_id binary(16),
	dto_type nvarchar(255) NOT NULL,
	description nvarchar(2000),
	disabled bit NOT NULL,
	regenerate_value bit NOT NULL,
	generator_properties image,
	generator_type nvarchar(255) NOT NULL,
	seq smallint,
	unmodifiable bit NOT NULL,
	CONSTRAINT idm_generate_value_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_idm_generate_val_dto_type ON idm_generate_value (dto_type);

CREATE TABLE idm_generate_value_a (
	id binary(16) NOT NULL,
	rev numeric(19,0) NOT NULL,
	revtype smallint,
	created datetime,
	created_m bit,
	creator nvarchar(255),
	creator_m bit,
	creator_id binary(16),
	creator_id_m bit,
	modifier nvarchar(255),
	modifier_m bit,
	modifier_id binary(16),
	modifier_id_m bit,
	original_creator nvarchar(255),
	original_creator_m bit,
	original_creator_id binary(16),
	original_creator_id_m bit,
	original_modifier nvarchar(255),
	original_modifier_m bit,
	original_modifier_id binary(16),
	original_modifier_id_m bit,
	realm_id binary(16),
	realm_id_m bit,
	transaction_id binary(16),
	transaction_id_m bit,
	description nvarchar(2000),
	description_m bit,
	disabled bit,
	disabled_m bit,
	regenerate_value bit,
	regenerate_value_m bit,
	dto_type nvarchar(255),
	dto_type_m bit,
	generator_properties image,
    generator_properties_m bit,
	generator_type nvarchar(255),
	generator_type_m bit,
	seq smallint,
	seq_m bit,
	unmodifiable bit,
	unmodifiable_m bit,
	CONSTRAINT idm_generate_value_a_pkey PRIMARY KEY (id,rev),
	CONSTRAINT fk_42bliu4so7fxlcusukbrtmaj4 FOREIGN KEY (rev) REFERENCES idm_audit(id)
);
