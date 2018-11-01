package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.ExternalCodeable;
import eu.bcvsolutions.idm.core.api.domain.ExternalIdentifiable;
import eu.bcvsolutions.idm.core.api.domain.IdentityState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;

/**
 * Filter for identities
 * 
 * @author Radek Tomiška
 *
 */
public class IdmIdentityFilter extends DataFilter implements CorrelationFilter, ExternalIdentifiable, ExternalCodeable {
	
	/**
	 * Identity by username
	 */
	public static final String PARAMETER_USERNAME = "username";
	/**
	 * Subordinates for given identity
	 */
	public static final String PARAMETER_SUBORDINATES_FOR = "subordinatesFor";
	/**
	 * Subordinates by given tree structure
	 */
	public static final String PARAMETER_SUBORDINATES_BY_TREE_TYPE = "subordinatesByTreeType";
	/**
	 * Managers for given identity
	 */
	public static final String PARAMETER_MANAGERS_FOR = "managersFor";
	/**
	 * Managers by given tree structure
	 */
	public static final String PARAMETER_MANAGERS_BY_TREE_TYPE = "managersByTreeType";
	/**
	 * Returns managers by identity's contract working prosition 
	 */
	public static final String PARAMETER_MANAGERS_BY_CONTRACT = "managersByContract";
	/**
	 * Identity is disabled
	 */
	public static final String PARAMETER_DISABLED = "disabled";
	/**
	 * Identity state
	 */
	public static final String PARAMETER_STATE = "state";
	/**
	 * Automatic role (by tree, attribute)
	 */
	public static final String PARAMETER_AUTOMATIC_ROLE = "automaticRoleId";
	/**
	 * Identifiers filter in externalCode, username
	 */
	public static final String PARAMETER_IDENTIFIERS = "identifiers";
	/**
	 * Guarantees for given role
	 */
	public static final String PARAMETER_GUARANTEES_FOR_ROLE = "guaranteesForRole";
	/**
	 * Identities by email
	 */
	public static final String PARAMETER_EMAIL = "email";
	/**
	 * roles - OR
	 */
	private List<UUID> roles;	
	
	/**
	 * Identities for tree structure (by identity contract)
	 */
	private UUID treeNode;
	/**
	 * Identities for tree structure recursively down
	 */
	private boolean recursively = true;
	/**
	 * Identities for tree structure (by identity contract)
	 */
	private UUID treeType;
	/**
	 * managers with contract guarantees included
	 */
	private boolean includeGuarantees = true;
	/**
	 * Identity first name - exact match
	 */
	private String firstName;
	/**
	 * Identity last name - exact match
	 */
	private String lastName;
	
	public IdmIdentityFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public IdmIdentityFilter(MultiValueMap<String, Object> data) {
		super(IdmIdentityDto.class, data);
	}
	
	public String getUsername() {
		return (String) data.getFirst(PARAMETER_USERNAME);
	}

	public void setUsername(String username) {
		data.set(PARAMETER_USERNAME, username);
	}

	public UUID getSubordinatesFor() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_SUBORDINATES_FOR));
	}

	public void setSubordinatesFor(UUID subordinatesFor) {
		data.set(PARAMETER_SUBORDINATES_FOR, subordinatesFor);
	}

	public UUID getSubordinatesByTreeType() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_SUBORDINATES_BY_TREE_TYPE));
	}

	public void setSubordinatesByTreeType(UUID subordinatesByTreeType) {
		data.set(PARAMETER_SUBORDINATES_BY_TREE_TYPE, subordinatesByTreeType);
	}
	
	public void setManagersFor(UUID managersFor) {
		data.set(PARAMETER_MANAGERS_FOR, managersFor);
	}
	
	public UUID getManagersFor() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_MANAGERS_FOR));
	}
	
	public void setManagersByTreeType(UUID managersByTreeType) {
		data.set(PARAMETER_MANAGERS_BY_TREE_TYPE, managersByTreeType);
	}
	
	public UUID getManagersByTreeType() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_MANAGERS_BY_TREE_TYPE));
	}
	
	public UUID getManagersByContract() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_MANAGERS_BY_CONTRACT));
	}
	
	public void setManagersByContract(UUID managersByContract) {
		data.set(PARAMETER_MANAGERS_BY_CONTRACT, managersByContract);
	}
	
	public void setRoles(List<UUID> roles) {
		this.roles = roles;
	}
	
	public List<UUID> getRoles() {
		if (roles == null) {
			roles = new ArrayList<>();
		}
		return roles;
	}

	@Override
	public String getProperty() {
		return (String) data.getFirst(PARAMETER_CORRELATION_PROPERTY);
	}

	@Override
	public void setProperty(String property) {
		data.set(PARAMETER_CORRELATION_PROPERTY, property);
	}

	@Override
	public String getValue() {
		return (String) data.getFirst(PARAMETER_CORRELATION_VALUE);
	}

	@Override
	public void setValue(String value) {
		data.set(PARAMETER_CORRELATION_VALUE, value);
	}
	
	public UUID getTreeNode() {
		return treeNode;
	}
	
	public void setTreeNode(UUID treeNode) {
		this.treeNode = treeNode;
	}
	
	public UUID getTreeType() {
		return treeType;
	}
	
	public void setTreeType(UUID treeType) {
		this.treeType = treeType;
	}
	
	public boolean isRecursively() {
		return recursively;
	}
	
	public void setRecursively(boolean recursively) {
		this.recursively = recursively;
	}
	
	public boolean isIncludeGuarantees() {
		return includeGuarantees;
	}
	
	public void setIncludeGuarantees(boolean includeGuarantees) {
		this.includeGuarantees = includeGuarantees;
	}

	public Boolean getDisabled() {
		// TODO: parameter converter
		Object disabled = data.getFirst(PARAMETER_DISABLED);
		if (disabled == null) {
			return null;
		}
		if (disabled instanceof Boolean) {
			return (Boolean) disabled;
		}
		return Boolean.valueOf(disabled.toString()) ;
	}

	public void setDisabled(Boolean disabled) {
		data.set(PARAMETER_DISABLED, disabled);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public void setState(IdentityState state) {
		data.set(PARAMETER_STATE, state);
	}
	
	public IdentityState getState() {
		return (IdentityState) data.getFirst(PARAMETER_STATE);
	}

	public UUID getAutomaticRoleId() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_AUTOMATIC_ROLE));
	}

	public void setAutomaticRoleId(UUID automaticRoleId) {
		data.set(PARAMETER_AUTOMATIC_ROLE, automaticRoleId);
	}
	
	public void setIdentifiers(List<String> identifiers) {
		data.put(PARAMETER_IDENTIFIERS, new ArrayList<Object>(identifiers));
	}

	public List<String> getIdentifiers() {
		List<Object> identifiers = data.get(PARAMETER_IDENTIFIERS);
		if (identifiers == null) {
			return Collections.emptyList();
		}
		return identifiers.stream()
				.map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
	}

	@Override
	public String getExternalCode() {
		return (String) data.getFirst(PROPERTY_EXTERNAL_CODE);
	}

	@Override
	public void setExternalCode(String externalCode) {
		data.set(PROPERTY_EXTERNAL_CODE, externalCode);
	}
	
	@Override
	public String getExternalId() {
		return (String) data.getFirst(PROPERTY_EXTERNAL_ID);
	}
	
	@Override
	public void setExternalId(String externalId) {
		data.set(PROPERTY_EXTERNAL_ID, externalId);
	}
	
	public UUID getGuaranteesForRole() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_GUARANTEES_FOR_ROLE));
	}
	
	public void setGuaranteesForRole(UUID guaranteesForRole) {
		data.set(PARAMETER_GUARANTEES_FOR_ROLE, guaranteesForRole);
	}
	
	/**
	 * @since 9.3.0
	 * @return
	 */
	public String getEmail() {
		return (String) data.getFirst(PARAMETER_EMAIL);
	}
	
	/**
	 * @since 9.3.0
	 * @param email
	 */
	public void setEmail(String email) {
		data.set(PARAMETER_EMAIL, email);
	}
}
