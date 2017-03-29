--
-- CzechIdM 7.0 Flyway script 
-- BCV solutions s.r.o.
--
-- This SQL script creates the required tables by Envers (Module Acc)


----- TABLE acc_account_a -----
CREATE TABLE acc_account_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  account_type character varying(255),
  account_type_m boolean,
  uid character varying(1000),
  uid_m boolean,
  system_id bytea,
  system_m boolean,
  system_entity_id bytea,
  system_entity_m boolean,
  CONSTRAINT acc_account_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_ocfc37k7urgtggoneqm048lyy FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE acc_identity_account_a -----
CREATE TABLE acc_identity_account_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  ownership boolean,
  ownership_m boolean,
  account_id bytea,
  account_m boolean,
  identity_id bytea,
  identity_m boolean,
  identity_role_id bytea,
  identity_role_m boolean,
  role_system_id bytea,
  role_system_m boolean,
  CONSTRAINT acc_identity_account_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_tc0lfendp9bg0vgag7pk0ib3d FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_role_system_a -----
CREATE TABLE sys_role_system_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  role_id bytea,
  role_m boolean,
  system_id bytea,
  system_m boolean,
  system_mapping_id bytea,
  system_mapping_m boolean,
  CONSTRAINT sys_role_system_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_n0hfyes79r41p01upa8bkbmu6 FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_role_system_attribute_a -----
CREATE TABLE sys_role_system_attribute_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  confidential_attribute boolean,
  confidential_attribute_m boolean,
  disabled_default_attribute boolean,
  disabled_default_attribute_m boolean,
  entity_attribute boolean,
  entity_attribute_m boolean,
  extended_attribute boolean,
  extended_attribute_m boolean,
  idm_property_name character varying(255),
  idm_property_name_m boolean,
  name character varying(255),
  name_m boolean,
  send_always boolean,
  send_always_m boolean,
  send_only_if_not_null boolean,
  send_only_if_not_null_m boolean,
  strategy_type character varying(255),
  strategy_type_m boolean,
  transform_script text,
  transform_script_m boolean,
  uid boolean,
  uid_m boolean,
  role_system_id bytea,
  role_system_m boolean,
  system_attr_mapping_id bytea,
  system_attribute_mapping_m boolean,
  CONSTRAINT sys_role_system_attribute_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_mb7uvabbp62wpmroqbn80nh34 FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_schema_attribute_a -----
CREATE TABLE sys_schema_attribute_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  class_type character varying(255),
  class_type_m boolean,
  createable boolean,
  createable_m boolean,
  multivalued boolean,
  multivalued_m boolean,
  name character varying(255),
  name_m boolean,
  native_name character varying(255),
  native_name_m boolean,
  readable boolean,
  readable_m boolean,
  required boolean,
  required_m boolean,
  returned_by_default boolean,
  returned_by_default_m boolean,
  updateable boolean,
  updateable_m boolean,
  object_class_id bytea,
  object_class_m boolean,
  CONSTRAINT sys_schema_attribute_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_h227a5dpgs004bu0mf3bxdjdn FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_schema_obj_class_a -----
CREATE TABLE sys_schema_obj_class_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  auxiliary boolean,
  auxiliary_m boolean,
  container boolean,
  container_m boolean,
  object_class_name character varying(255),
  object_class_name_m boolean,
  system_id bytea,
  system_m boolean,
  CONSTRAINT sys_schema_obj_class_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_jhghwftbl8995o2e1m18dswbq FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_sync_config_a -----
CREATE TABLE sys_sync_config_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  custom_filter boolean,
  custom_filter_m boolean,
  custom_filter_script text,
  custom_filter_script_m boolean,
  description character varying(2000),
  description_m boolean,
  enabled boolean,
  enabled_m boolean,
  filter_operation character varying(255),
  filter_operation_m boolean,
  linked_action character varying(255),
  linked_action_m boolean,
  linked_action_wf character varying(255),
  linked_action_wf_key_m boolean,
  missing_account_action character varying(255),
  missing_account_action_m boolean,
  missing_account_action_wf character varying(255),
  missing_account_action_wf_key_m boolean,
  missing_entity_action character varying(255),
  missing_entity_action_m boolean,
  missing_entity_action_wf character varying(255),
  missing_entity_action_wf_key_m boolean,
  name character varying(255),
  name_m boolean,
  reconciliation boolean,
  reconciliation_m boolean,
  token text,
  token_m boolean,
  unlinked_action character varying(255),
  unlinked_action_m boolean,
  unlinked_action_wf character varying(255),
  unlinked_action_wf_key_m boolean,
  correlation_attribute_id bytea,
  correlation_attribute_m boolean,
  filter_attribute_id bytea,
  filter_attribute_m boolean,
  system_mapping_id bytea,
  system_mapping_m boolean,
  token_attribute_id bytea,
  token_attribute_m boolean,
  CONSTRAINT sys_sync_config_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_qu1u38lrk8vnhkqd5b4ehf2mp FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_system_a -----
CREATE TABLE sys_system_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  connector_bundle_name character varying(255),
  connector_bundle_version character varying(30),
  connector_name character varying(255),
  connector_framework character varying(255),
  connector_key_m boolean,
  host character varying(255),
  port integer,
  timeout integer,
  use_ssl boolean,
  connector_server_m boolean,
  description character varying(2000),
  description_m boolean,
  disabled boolean,
  disabled_m boolean,
  name character varying(255),
  name_m boolean,
  queue boolean,
  queue_m boolean,
  readonly boolean,
  readonly_m boolean,
  remote boolean,
  remote_m boolean,
  virtual boolean,
  virtual_m boolean,
  password_pol_gen_id bytea,
  password_policy_generate_m boolean,
  password_pol_val_id bytea,
  password_policy_validate_m boolean,
  CONSTRAINT sys_system_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_khsnjdk8wea24bo32saq8cc14 FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_system_attribute_mapping_a -----
CREATE TABLE sys_system_attribute_mapping_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  authentication_attribute boolean,
  authentication_attribute_m boolean,
  confidential_attribute boolean,
  confidential_attribute_m boolean,
  disabled_attribute boolean,
  disabled_attribute_m boolean,
  entity_attribute boolean,
  entity_attribute_m boolean,
  extended_attribute boolean,
  extended_attribute_m boolean,
  idm_property_name character varying(255),
  idm_property_name_m boolean,
  name character varying(255),
  name_m boolean,
  send_always boolean,
  send_always_m boolean,
  send_only_if_not_null boolean,
  send_only_if_not_null_m boolean,
  strategy_type character varying(255),
  strategy_type_m boolean,
  transform_from_res_script text,
  transform_from_resource_script_m boolean,
  transform_to_res_script text,
  transform_to_resource_script_m boolean,
  uid boolean,
  uid_m boolean,
  schema_attribute_id bytea,
  schema_attribute_m boolean,
  system_mapping_id bytea,
  system_mapping_m boolean,
  CONSTRAINT sys_system_attribute_mapping_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_ncp3og5psyxsw3kr3kyqc49mm FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_system_form_value_a -----
CREATE TABLE sys_system_form_value_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  boolean_value boolean,
  boolean_value_m boolean,
  byte_value bytea,
  byte_value_m boolean,
  confidential boolean,
  confidential_m boolean,
  date_value timestamp without time zone,
  date_value_m boolean,
  double_value numeric(38,4),
  double_value_m boolean,
  long_value bigint,
  long_value_m boolean,
  persistent_type character varying(45),
  persistent_type_m boolean,
  seq smallint,
  seq_m boolean,
  string_value text,
  string_value_m boolean,
  attribute_id bytea,
  form_attribute_m boolean,
  owner_id bytea,
  owner_m boolean,
  CONSTRAINT sys_system_form_value_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_awctj8y0nn7xyocg95xqpsq62 FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


----- TABLE sys_system_mapping_a -----
CREATE TABLE sys_system_mapping_a
(
  id bytea NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  created timestamp without time zone,
  created_m boolean,
  creator character varying(255),
  creator_m boolean,
  creator_id bytea,
  creator_id_m boolean,
  modified timestamp without time zone,
  modified_m boolean,
  modifier character varying(255),
  modifier_m boolean,
  modifier_id bytea,
  modifier_id_m boolean,
  original_creator character varying(255),
  original_creator_m boolean,
  original_creator_id bytea,
  original_creator_id_m boolean,
  original_modifier character varying(255),
  original_modifier_m boolean,
  original_modifier_id bytea,
  original_modifier_id_m boolean,
  realm_id bytea,
  realm_id_m boolean,
  transaction_id bytea,
  transaction_id_m boolean,
  entity_type character varying(255),
  entity_type_m boolean,
  name character varying(255),
  name_m boolean,
  operation_type character varying(255),
  operation_type_m boolean,
  object_class_id bytea,
  object_class_m boolean,
  CONSTRAINT sys_system_mapping_a_pkey PRIMARY KEY (id, rev),
  CONSTRAINT fk_8ruymojmoa1l1xv4x1n5tbatv FOREIGN KEY (rev)
      REFERENCES idm_audit (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);