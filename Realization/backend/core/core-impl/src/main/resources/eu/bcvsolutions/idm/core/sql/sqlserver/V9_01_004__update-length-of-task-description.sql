--
-- CzechIdM 9 Flyway script 
-- BCV solutions s.r.o.
--
-- update length of task description from 255 to 2000

ALTER TABLE idm_long_running_task ALTER COLUMN task_description NVARCHAR (2000);
