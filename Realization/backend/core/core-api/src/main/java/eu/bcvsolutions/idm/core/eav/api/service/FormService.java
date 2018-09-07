package eu.bcvsolutions.idm.core.eav.api.service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationClass;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.script.ScriptEnabled;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormValueFilter;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Work with form definitions, attributes and their values => eav, extended attributes and their values to given owner ({@link FormableEntity}).
 * 
 * Confidential values are stored in confidential storage.
 * 
 * {@link AbstractEntity} type or {@link AbstractDto} can be used as owner type. 
 * Underlying {@link AbstractEntity} has to extend {@link FormableEntity}. 
 * If {@link AbstractDto} is given as owner type, then {@link FormableEntity} owner will be found by 
 * {@link LookupService} => transformation to {@link FormableEntity}. 
 * 
 * @see ConfidentialStorage
 * @see IdmFormDefinition}
 * @see IdmFormAttribute}
 * @see FormableEntity
 * @see LookupService
 * 
 * @author Radek Tomiška
 *
 */
public interface FormService extends ScriptEnabled {
	
	/**
	 * Default definition name for type (if no name is given)
	 */
	String DEFAULT_DEFINITION_CODE = IdmFormDefinitionService.DEFAULT_DEFINITION_CODE;
	
	/**
	 * Returns true, when given owner type support eav forms. If {@link AbstractDto} owner type is given, 
	 * then underlying {@link AbstractEntity} is resolved automatically => {@link AbstractEntity} has 
	 * to implement  {@link FormableEntity}.
	 * 
	 * @see {@link IdmFormDefinitionService#isFormable(Class)}
	 * @param ownerType
	 * @return
	 */
	boolean isFormable(Class<? extends Identifiable> ownerType);
	
	/**
	 * Finds main definition by given type
	 * 
	 * @param type
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto getDefinition(String type, BasePermission... permission);
	
	/**
	 * Returns all definitions for given type
	 * 
	 * @param type
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	List<IdmFormDefinitionDto> getDefinitions(String type, BasePermission... permission);
	
	/**
	 * Finds definition by given type and code (optional) 
	 * 
	 * @param type
	 * @param code [optional] - if no code is given, then returns main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto getDefinition(String type, String code, BasePermission... permission);
	
	/**
	 * Finds main definition by given owner
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType owner type
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, BasePermission... permission);
	
	/**
	 * Returns all definitions for given type by given owner
	 * 
	 * @param owner
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	List<IdmFormDefinitionDto> getDefinitions(Identifiable owner, BasePermission... permission);
	
	/**
	 * Returns all definitions for given type
	 * 
	 * @param ownerType
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	List<IdmFormDefinitionDto> getDefinitions(Class<? extends Identifiable> ownerType, BasePermission... permission);
	
	/**
	 * Finds definition by given type and code (optional) 
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType
	 * @param code [optional] - if no code is given, then returns main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, String code, BasePermission... permission);
	
	/**
	 * Return default definition type for given owner type.
	 * 
	 * @param ownerType owner type
	 * @return
	 */
	String getDefaultDefinitionType(Class<? extends Identifiable> ownerType);
	
	/**
	 * Returns attribute by given code from main form definition
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType owner type
	 * @param attributeCode attribute code
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormAttributeDto getAttribute(Class<? extends Identifiable> ownerType, String attributeCode, BasePermission... permission);
	
	/**
	 * Returns attributes by given form definition
	 * 
	 * @param formDefinition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	List<IdmFormAttributeDto> getAttributes(IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Returns attribute by given code form definition by type and code
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType
	 * @param definitionCode [optional] - if no code is given, then returns main definition
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormAttributeDto getAttribute(Class<? extends Identifiable> ownerType, String definitionCode, String attributeCode, BasePermission... permission);
	
	/**
	 * Returns attribute by given code form definition by type and code
	 * 
	 * @param formDefinition
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormAttributeDto getAttribute(IdmFormDefinitionDto formDefinition, String attributeCode, BasePermission... permission);
	
	/**
	 * Saves given form definition
	 * 
	 * @param formDefinition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto saveDefinition(IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Creates form definition
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param type definition type
	 * @param code [optional] definition code (main with default code will be created, if no definition code is given)
	 * @param formAttributes
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto createDefinition(String type, String code, List<IdmFormAttributeDto> formAttributes, BasePermission... permission);
	
	/**
	 * Creates main form definition for given owner type
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType owner type
	 * @param formAttributes
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto createDefinition(Class<? extends Identifiable> ownerType, List<IdmFormAttributeDto> formAttributes, BasePermission... permission);
	
	/**
	 * Creates form definition
	 * 
	 * @param ownerType owner type
	 * @param code [optional] definition code (main with default code will be created, if no definition code is given)
	 * @param formAttributes
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormDefinitionDto createDefinition(Class<? extends Identifiable> ownerType, String code, List<IdmFormAttributeDto> formAttributes, BasePermission... permission);
	
	/**
	 * Persists given form attribute.
	 * 
	 * @param attribute
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormAttributeDto saveAttribute(IdmFormAttributeDto attribute, BasePermission... permission);
	
	/**
	 * Persists given form attribute. If attribute does not have form definition specified, then main will be used.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerType owner type
	 * @param attribute
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormAttributeDto saveAttribute(Class<? extends Identifiable> ownerType, IdmFormAttributeDto attribute, BasePermission... permission);
		
	/**
	 * Saves form values to given owner and form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param formDefinition [optional] if not specified, then main will be used.
	 * @param values
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmFormValueDto> saveValues(Identifiable owner, IdmFormDefinitionDto formDefinition, List<IdmFormValueDto> values, BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param formDefinition [optional] if not specified, then main will be used.
	 * @param values
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmFormValueDto> saveValues(Identifiable owner, UUID formDefinition, List<IdmFormValueDto> values, BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param formDefinition [optional] if not specified, then main will be used.
	 * @param values
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form definition. Only given form attributes by the given values will be saved.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param formDefinition [optional] if not specified, then main will be used.
	 * @param values
	 * @param permission base permissions to evaluate (AND)
	 * @return 
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmFormInstanceDto saveFormInstance(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only.
	 * 
	 * @param owner
	 * @param attribute
	 * @param persistentValue raw values
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param attribute
	 * @param persistentValues raw values
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param formDefinition [optional] - main will be used, if no definition is given
	 * @param attributeCode
	 * @param persistentValues
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param formDefinition  [optional] - main will be used, if no definition is given
	 * @param attributeCode
	 * @param persistentValues
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only. Main form definition will be used.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param attributeCode
	 * @param persistentValues
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> saveValues(
			Identifiable owner,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Saves form values to given owner and form attribute - saves attribute values only. Main form definition will be used.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param attributeCode
	 * @param persistentValues
	 * @param permission base permissions to evaluate (AND)
	 * @return persisted values
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission);
	
	/**
	 * Reads form values by given owner. Return values from main form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> getValues(Identifiable owner, BasePermission... permission);
	
	/**
	 * Reads form values by given owner. Return values from main form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> getValues(UUID ownerId, Class<? extends Identifiable> ownerType, BasePermission... permission);
	
	/**
	 * Reads form values by given owner. Return values from main form definition.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	IdmFormInstanceDto getFormInstance(Identifiable owner, BasePermission... permission);

	/**
	 * Reads form values by given owner and form definition
	 * 
	 * @param owner
	 * @param formDefinitionId [optional] if form definition is not given, then return attribute values from main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	List<IdmFormValueDto> getValues(Identifiable owner, UUID formDefinitionId, BasePermission... permission);
	/**
	 * Reads form values by given owner and form definition
	 * 
	 * @param owner
	 * @param formDefinition [optional] if form definition is not given, then return attribute values from main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	List<IdmFormValueDto> getValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Reads form values by given owner and form definition
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param formDefinition [optional] if form definition is not given, then return attribute values from main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Reads form values by given owner and form definition
	 * 
	 * @param owner
	 * @param formDefinition [optional] if form definition is not given, then return attribute values from main definition
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	IdmFormInstanceDto getFormInstance(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Returns attribute values by attributeCode from given definition, or empty collection
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param formDefinition [optional] if form definition is not given, then return attribute values from main definition
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	List<IdmFormValueDto> getValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission);
	
	/**
	 * Returns attribute values by attributeCode from given definition, or empty collection
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param formDefinition [optional] if form definition is not given, then return attribute values from main definition
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if form definition is not given and main definition does not exist
	 */
	List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission);
	
	/**
	 * Returns attribute values by given attribute definition, or empty collection.
	 * 
	 * @param owner
	 * @param attribute (required)
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if attribute is not given
	 */
	List<IdmFormValueDto> getValues(Identifiable owner, IdmFormAttributeDto attribute, BasePermission... permission);
	
	/**
	 * Returns attribute values by given attribute definition, or empty collection.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param attribute  (required)
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if attribute is not given
	 */
	List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			BasePermission... permission);
	
	/**
	 * Returns attribute values by attributeCode from main definition, or empty collection
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param owner
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> getValues(Identifiable owner, String attributeCode, BasePermission... permission);
	
	/**
	 * Returns attribute values by attributeCode from main definition, or empty collection
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)}
	 * @param ownerId
	 * @param ownerType
	 * @param attributeCode
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 * @throws IllegalArgumentException if main definition does not exist
	 */
	List<IdmFormValueDto> getValues(UUID ownerId, Class<? extends Identifiable> ownerType, String attributeCode, BasePermission... permission);
	
	/**
	 * Deletes given attribute definition
	 * 
	 * @param attribute
	 * @param permission base permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteAttribute(IdmFormAttributeDto attribute, BasePermission... permission);

	/**
	 *  Deletes form attribute value by value ID
	 * @param formDefinition
	 * @param permissions base permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteValue(IdmFormDefinitionDto formDefinition, String valueId, BasePermission... permissions);

	/**
	 * Deletes form values by given owner
	 * 
	 * @param owner values owner
	 * @param permission base permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteValues(Identifiable owner, BasePermission... permission);
	
	/**
	 * Deletes form values by given owner and form definition
	 * 
	 * @param owner values owner
	 * @param formDefinition
	 * @param permission base permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission);
	
	/**
	 * Deletes form values by given owner and form attribute
	 * 
	 * @param owner
	 * @param formAttribute
	 * @param permission base permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteValues(Identifiable owner, IdmFormAttributeDto formAttribute, BasePermission... permission);
	
	/**
	 * Returns key in confidential storage for given extended attribute and owner
	 * 
	 * @see {@link ConfidentialStorage}
	 * @see {@link AbstractFormValueService#getConfidentialStorageKey(IdmFormAttribute)}
	 * 
	 * @param owner attribute owner
	 * @param attribute extended attribute
	 * @return key to confidential storage to given owner and attribute
	 */
	String getConfidentialStorageKey(Identifiable owner, IdmFormAttributeDto attribute);
	
	/**
	 * Returns value in FormValue's persistent type from confidential storage
	 * 
	 * @see {@link #getConfidentialStorageKey(FormableEntity, IdmFormAttribute)}
	 * 
	 * @param guardedValue
	 * @param <O> values owner
	 * @param <E> value entity
	 * @return
	 */
	Serializable getConfidentialPersistentValue(IdmFormValueDto guardedValue);
	
	/**
	 * Finds owners by attribute value
	 * Returns owner as {@link BaseDto}.
	 * 
	 * @param ownerType owner type
	 * @param attribute attribute
	 * @param persistentValue attribute value
	 * @param pageable
	 * @param <O> values owner
	 * @return owners dto
	 */
	<O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType, IdmFormAttributeDto attribute, Serializable persistentValue, Pageable pageable);
	
	/**
	 * Finds owners by attribute value from default form definition.
	 * Returns owner as {@link BaseDto}.
	 * 
	 * @see {@link #getDefaultDefinitionType(Class)
	 * @param ownerType owner type
	 * @param attributeCode attribute
	 * @param persistentValue attribute value
	 * @param pageable
	 * @param <O> values owner
	 * @return owners dto
	 */
	<O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType, String attributeCode, Serializable persistentValue, Pageable pageable);
	
	/**
	 * Find values by given filter
	 * 
	 * @param filter Owner is required - values are returnded by given owner type.
	 * @param pageable
	 * @param permission
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Page<IdmFormValueDto> findValues(IdmFormValueFilter filter, Pageable pageable, BasePermission... permission);
	
	
	/**
	 * Method returns full class names of all entities implementing {@link FormableEntity}
	 * 
	 * @return
	 */
	List<String> getOwnerTypes();

	/**
	 * Create instance of form definition from the given configuration class
	 * 
	 * @param configurationClass
	 * @return
	 */
	IdmFormDefinitionDto convertConfigurationToFormDefinition(Class<? extends ConfigurationClass> configurationClass);
}
