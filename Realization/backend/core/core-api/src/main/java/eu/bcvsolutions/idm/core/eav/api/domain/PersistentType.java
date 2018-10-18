package eu.bcvsolutions.idm.core.eav.api.domain;

/**
 * Supported attribute values data type
 * 
 * @author Radek Tomiška
 */
public enum PersistentType {

	CHAR,
	SHORTTEXT, // use this persistent type mainly (length 2000, indexed by default) - TEXT is used for long texts and is not indexed by default
	TEXT, // long texts - not indexed by default
	INT,
	LONG, 
	DOUBLE, 
	BOOLEAN, 
	DATE,
	DATETIME,
	BYTEARRAY,
	UUID, // referenced entity uuid (identity) 
	ATTACHMENT; // referenced attachment
}
