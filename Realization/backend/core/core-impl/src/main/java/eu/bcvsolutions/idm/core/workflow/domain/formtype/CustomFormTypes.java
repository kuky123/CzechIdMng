package eu.bcvsolutions.idm.core.workflow.domain.formtype;

import java.util.LinkedHashMap;
import java.util.Map;

import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.FormValue;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.impl.form.BooleanFormType;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.DoubleFormType;
import org.activiti.engine.impl.form.FormTypes;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;

public class CustomFormTypes extends FormTypes {
	
	protected static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd"; // TODO: application scope configuration

	public CustomFormTypes() {
		// register Activiti's default form types
		addFormType(new StringFormType());
		addFormType(new LongFormType());
		addFormType(new DateFormType(DATE_FORMAT_PATTERN));
		addFormType(new BooleanFormType());
		addFormType(new DoubleFormType());
		addFormType(new DecisionFormType());
		addFormType(new ConfigurationFormType());
	}

	@Override
	public AbstractFormType parseFormPropertyType(FormProperty formProperty) {
		Map<String, String> values = new LinkedHashMap<>();
		for (FormValue formValue : formProperty.getFormValues()) {
			values.put(formValue.getId(), formValue.getName());
		}
		switch (formProperty.getType()) {
			case TextAreaFormType.TYPE_NAME: {
				return new TextAreaFormType(values);
			}
			case TextFieldFormType.TYPE_NAME: {
				return new TextFieldFormType(values);
			}
			case CheckboxFormType.TYPE_NAME: {
				return new CheckboxFormType(values);
			}
			case LocalDateFormType.TYPE_NAME: {
				return new LocalDateFormType(values);
			}
			case ConfigurationFormType.TYPE_NAME: {
				return new ConfigurationFormType(values);
			}
			case SelectBoxFormType.TYPE_NAME: {
				return new SelectBoxFormType(values);
			}
			case TaskHistoryFormType.TYPE_NAME: {
				return new TaskHistoryFormType(values);
			}
			default: {
				// delegate construction of all other types
				return super.parseFormPropertyType(formProperty);
			}
		}
	}
}