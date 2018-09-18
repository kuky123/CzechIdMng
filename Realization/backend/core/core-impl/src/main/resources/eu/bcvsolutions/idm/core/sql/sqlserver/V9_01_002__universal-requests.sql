--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- add universal request agenda

CREATE TABLE idm_request (
	id binary(16) NOT NULL,
	created datetime2(6) NOT NULL,
	creator nvarchar(255) NOT NULL,
	creator_id binary(16) NULL,
	modified datetime2(6) NULL,
	modifier nvarchar(255) NULL,
	modifier_id binary(16) NULL,
	original_creator nvarchar(255) NULL,
	original_creator_id binary(16) NULL,
	original_modifier nvarchar(255) NULL,
	original_modifier_id binary(16) NULL,
	realm_id binary(16) NULL,
	transaction_id binary(16) NULL,
	description nvarchar(2000) NULL,
	execute_immediately bit NOT NULL,
	"name" nvarchar(255) NULL,
	operation nvarchar(255) NULL,
	owner_id binary(16) NULL,
	owner_type nvarchar(255) NOT NULL,
	request_type nvarchar(255) NOT NULL,
	result_cause nvarchar(MAX) NULL,
	result_code nvarchar(255) NULL,
	result_model image NULL,
	result_state nvarchar(45) NOT NULL,
	state nvarchar(255) NOT NULL,
	wf_process_id nvarchar(255) NULL,
	CONSTRAINT idm_request_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_idm_req_name ON idm_request(name) ;
CREATE INDEX idx_idm_req_o_id ON idm_request(owner_id) ;
CREATE INDEX idx_idm_req_o_type ON idm_request(owner_type) ;
CREATE INDEX idx_idm_req_state ON idm_request(state) ;
CREATE INDEX idx_idm_req_wf ON idm_request(wf_process_id) ;

CREATE TABLE idm_request_a (
	id binary(16) NOT NULL,
	rev numeric(19,0) NOT NULL,
	revtype smallint NULL,
	created datetime2(6) NULL,
	created_m bit NULL,
	creator nvarchar(255) NULL,
	creator_m bit NULL,
	creator_id binary(16) NULL,
	creator_id_m bit NULL,
	modifier nvarchar(255) NULL,
	modifier_m bit NULL,
	modifier_id binary(16) NULL,
	modifier_id_m bit NULL,
	original_creator nvarchar(255) NULL,
	original_creator_m bit NULL,
	original_creator_id binary(16) NULL,
	original_creator_id_m bit NULL,
	original_modifier nvarchar(255) NULL,
	original_modifier_m bit NULL,
	original_modifier_id binary(16) NULL,
	original_modifier_id_m bit NULL,
	realm_id binary(16) NULL,
	realm_id_m bit NULL,
	transaction_id binary(16) NULL,
	transaction_id_m bit NULL,
	description nvarchar(2000) NULL,
	description_m bit NULL,
	execute_immediately bit NULL,
	execute_immediately_m bit NULL,
	"name" nvarchar(255) NULL,
	name_m bit NULL,
	operation nvarchar(255) NULL,
	operation_m bit NULL,
	request_type nvarchar(255) NULL,
	request_type_m bit NULL,
	state nvarchar(255) NULL,
	state_m bit NULL,
	wf_process_id nvarchar(255) NULL,
	wf_process_id_m bit NULL,
	CONSTRAINT idm_request_a_pkey PRIMARY KEY (id, rev),
	CONSTRAINT fk_7pgo3odj3kecfk7ae7mlregjc FOREIGN KEY (rev) REFERENCES idm_audit(id)
);

CREATE TABLE idm_request_item (
	id binary(16) NOT NULL,
	created datetime2(6) NOT NULL,
	creator nvarchar(255) NOT NULL,
	creator_id binary(16) NULL,
	modified datetime2(6) NULL,
	modifier nvarchar(255) NULL,
	modifier_id binary(16) NULL,
	original_creator nvarchar(255) NULL,
	original_creator_id binary(16) NULL,
	original_modifier nvarchar(255) NULL,
	original_modifier_id binary(16) NULL,
	realm_id binary(16) NULL,
	transaction_id binary(16) NULL,
	"data" nvarchar(MAX) NULL,
	operation nvarchar(255) NULL,
	owner_id binary(16) NULL,
	owner_type nvarchar(255) NOT NULL,
	result_cause nvarchar(MAX) NULL,
	result_code nvarchar(255) NULL,
	result_model image NULL,
	result_state nvarchar(45) NOT NULL,
	state nvarchar(255) NOT NULL,
	super_owner_id binary(16) NULL,
	super_owner_type nvarchar(255) NULL,
	wf_process_id nvarchar(255) NULL,
	request_id binary(16) NOT NULL,
	CONSTRAINT idm_request_item_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_idm_req_item_o_id ON idm_request_item(owner_id) ;
CREATE INDEX idx_idm_req_item_o_type ON idm_request_item(owner_type) ;
CREATE INDEX idx_idm_req_item_operation ON idm_request_item(operation) ;
CREATE INDEX idx_idm_req_item_req_id ON idm_request_item(request_id) ;
CREATE INDEX idx_idm_req_item_so_id ON idm_request_item(super_owner_id) ;
CREATE INDEX idx_idm_req_item_so_type ON idm_request_item(super_owner_type) ;

CREATE TABLE idm_request_item_a (
	id binary(16) NOT NULL,
	rev numeric(19,0) NOT NULL,
	revtype smallint NULL,
	created datetime2(6) NULL,
	created_m bit NULL,
	creator nvarchar(255) NULL,
	creator_m bit NULL,
	creator_id binary(16) NULL,
	creator_id_m bit NULL,
	modifier nvarchar(255) NULL,
	modifier_m bit NULL,
	modifier_id binary(16) NULL,
	modifier_id_m bit NULL,
	original_creator nvarchar(255) NULL,
	original_creator_m bit NULL,
	original_creator_id binary(16) NULL,
	original_creator_id_m bit NULL,
	original_modifier nvarchar(255) NULL,
	original_modifier_m bit NULL,
	original_modifier_id binary(16) NULL,
	original_modifier_id_m bit NULL,
	realm_id binary(16) NULL,
	realm_id_m bit NULL,
	transaction_id binary(16) NULL,
	transaction_id_m bit NULL,
	operation nvarchar(255) NULL,
	operation_m bit NULL,
	state nvarchar(255) NULL,
	state_m bit NULL,
	wf_process_id nvarchar(255) NULL,
	wf_process_id_m bit NULL,
	request_id binary(16) NULL,
	request_m bit NULL,
	CONSTRAINT idm_request_item_a_pkey PRIMARY KEY (id, rev),
	CONSTRAINT fk_rxmtso437vofg86kgq9cu48r8 FOREIGN KEY (rev) REFERENCES idm_audit(id)
);



