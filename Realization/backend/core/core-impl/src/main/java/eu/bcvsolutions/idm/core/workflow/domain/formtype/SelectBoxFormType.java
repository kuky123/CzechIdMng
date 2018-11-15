package eu.bcvsolutions.idm.core.workflow.domain.formtype;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bcvsolutions.idm.core.api.exception.CoreException;

/**
 * Form type for select box component
 *
 * @author Roman Kučera
 */
public class SelectBoxFormType extends AbstractComponentFormType {

	private static final long serialVersionUID = 1L;
	public static final String TYPE_NAME = "selectBox";

	public SelectBoxFormType(Map<String, String> values) {
		super(values);
	}

	@Override
	public Object convertFormValueToModelValue(String propertyValue) {
		return propertyValue;
	}

	@Override
	public String convertModelValueToFormValue(Object modelValue) {
		if (modelValue == null) {
			return null;
		}
		if (!(modelValue instanceof String)) {
			try {
				return new ObjectMapper().writeValueAsString(modelValue);
			} catch (JsonProcessingException e) {
				throw new CoreException(e);
			}
		}
		return (String) modelValue;
	}

	@Override
	public String getName() {
		return TYPE_NAME;
	}
}
