package eu.bcvsolutions.idm.core.api.generator;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;

/**
 * Abstract class for value generators
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 *
 * @param <DTO> generator is designed for only one dto type
 */
public abstract class AbstractValueGenerator<DTO extends AbstractDto> implements ValueGenerator<DTO>, BeanNameAware {

	private String beanName; // spring bean name - used as processor id
	private final Class<DTO> dtoClass;
	
	@Autowired
	private ConfigurationService configurationService;
	
	@SuppressWarnings({ "unchecked" })
	public AbstractValueGenerator() {
		this.dtoClass = (Class<DTO>) GenericTypeResolver.resolveTypeArgument(getClass(), ValueGenerator.class);
	}
	
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
	
	@Override
	public String getId() {
		return beanName;
	}

	@Override
	public Class<DTO> getDtoClass() {
		return dtoClass;
	}

	@Override
	public boolean supports(Class<?> dtoType) {
		Assert.notNull(dtoType);
		//
		return this.getDtoClass().isAssignableFrom(dtoType);
	}

	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}
}
