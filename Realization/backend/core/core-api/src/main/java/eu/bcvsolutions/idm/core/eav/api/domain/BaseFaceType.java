package eu.bcvsolutions.idm.core.eav.api.domain;

/**
 * Basic core faces types. Default face type is resolved by attribute's {@link PersistentType}. 
 * 
 * @author Radek Tomiška
 *
 */
public interface BaseFaceType {
  
	String TEXTAREA = "TEXTAREA";
	String RICHTEXTAREA = "RICHTEXTAREA";
	//
	String CURRENCY = "CURRENCY";
	//
	String IDENTITY_SELECT = "IDENTITY-SELECT";
	String ROLE_SELECT = "ROLE-SELECT";
	String FORM_DEFINITION_SELECT = "FORM-DEFINITION-SELECT";
	String TREE_NODE_SELECT = "TREE-NODE-SELECT";
	String ROLE_CATALOGUE_SELECT = "ROLE-CATALOGUE-SELECT";
	//
	String BOOLEAN_SELECT = "BOOLEAN-SELECT";
}
