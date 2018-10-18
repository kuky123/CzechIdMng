--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- Entity and audit entity for generate values


CREATE TABLE idm_generate_value (
	id bytea NOT NULL,
	created timestamp without time zone NOT NULL,
	creator varchar(255) NOT NULL,
	creator_id bytea,
	modified timestamp without time zone,
	modifier varchar(255),
	modifier_id bytea,
	original_creator varchar(255),
	original_creator_id bytea,
	original_modifier varchar(255),
	original_modifier_id bytea,
	realm_id bytea,
	transaction_id bytea,
	dto_type varchar(255) NOT NULL,
	description varchar(2000),
	disabled boolean NOT NULL,
	regenerate_value boolean NOT NULL,
	generator_properties bytea,
	generator_type varchar(255) NOT NULL,
	seq smallint,
	unmodifiable boolean NOT NULL,
	CONSTRAINT idm_generate_value_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_idm_generate_val_entity_t
  ON idm_generate_value
  USING btree
  (dto_type);

CREATE TABLE idm_generate_value_a (
	id bytea NOT NULL,
	rev bigint NOT NULL,
	revtype smallint,
	created timestamp without time zone,
	created_m boolean,
	creator varchar(255),
	creator_m boolean,
	creator_id bytea,
	creator_id_m boolean,
	modifier varchar(255),
	modifier_m boolean,
	modifier_id bytea,
	modifier_id_m boolean,
	original_creator varchar(255),
	original_creator_m boolean,
	original_creator_id bytea,
	original_creator_id_m boolean,
	original_modifier varchar(255),
	original_modifier_m boolean,
	original_modifier_id bytea,
	original_modifier_id_m boolean,
	realm_id bytea,
	realm_id_m boolean,
	transaction_id bytea,
	transaction_id_m boolean,
	description varchar(2000),
	description_m boolean,
	disabled boolean,
	disabled_m boolean,
	regenerate_value boolean,
	regenerate_value_m boolean,
	dto_type varchar(255),
	dto_type_m boolean,
	generator_properties bytea,
    generator_properties_m boolean,
	generator_type varchar(255),
	generator_type_m boolean,
	seq smallint,
	seq_m boolean,
	unmodifiable boolean,
	unmodifiable_m boolean,
	CONSTRAINT idm_generate_value_a_pkey PRIMARY KEY (id,rev),
	CONSTRAINT fk_42bliu4so7fxlcusukbrtmaj4 FOREIGN KEY (rev)
	REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);
