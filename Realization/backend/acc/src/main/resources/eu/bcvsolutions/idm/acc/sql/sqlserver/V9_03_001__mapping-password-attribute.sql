--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- Add new property password attribute for attribute mapping

ALTER TABLE sys_system_attribute_mapping ADD password_attribute bit NOT NULL DEFAULT 0
GO
ALTER TABLE sys_system_attribute_mapping_a ADD password_attribute bit
GO
ALTER TABLE sys_system_attribute_mapping_a ADD password_attribute_m bit
GO

-- update all current __PASSWORD__ attributes for provisioning to password attribute
UPDATE
	sys_system_attribute_mapping
SET
	password_attribute = 1
WHERE
	EXISTS(
		SELECT
			sa.id
		FROM
			sys_schema_attribute sa
		WHERE
			sa.id = schema_attribute_id
			AND sa.name = '__PASSWORD__'
	)
	AND EXISTS(
		SELECT
			sm.id
		FROM
			sys_system_mapping sm
		WHERE
			sm.id = system_mapping_id
			AND sm.operation_type = 'PROVISIONING'
	);