package eu.bcvsolutions.idm.core.security.api.dto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.bcvsolutions.idm.core.api.dto.AbstractComponentDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;

/**
 * Evaluator dto - definition only
 * 
 * @author Radek Tomiška
 */
@JsonInclude(Include.NON_NULL)
@Relation(collectionRelation = "authorizationEvaluators")
public class AuthorizationEvaluatorDto extends AbstractComponentDto {

	private static final long serialVersionUID = 1L;
	@NotEmpty
	private String entityType;
	@NotEmpty
	private String evaluatorType;
	/**
	 * @deprecated @since 8.2.0 use form definition instead
	 */
	@Deprecated
	private List<String> parameters;
	private boolean supportsPermissions;
	private IdmFormDefinitionDto formDefinition;
	
	public String getEntityType() {
		return entityType;
	}
	
	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}
	
	public String getEvaluatorType() {
		return evaluatorType;
	}
	
	public void setEvaluatorType(String evaluatorType) {
		this.evaluatorType = evaluatorType;
	}
	
	/**
	 * @deprecated @since 8.2.0 use form definition instead
	 */
	@Deprecated
	public List<String> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		return parameters;
	}
	
	/**
	 * @deprecated @since 8.2.0 use form definition instead
	 */
	@Deprecated
	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}
	
	public void setSupportsPermissions(boolean supportsPermissions) {
		this.supportsPermissions = supportsPermissions;
	}
	
	public boolean isSupportsPermissions() {
		return supportsPermissions;
	}
	
	public void setFormDefinition(IdmFormDefinitionDto formDefinition) {
		this.formDefinition = formDefinition;
	}
	
	public IdmFormDefinitionDto getFormDefinition() {
		return formDefinition;
	}
}
