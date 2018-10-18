package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.primitives.Shorts;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmGenerateValueFilter;
import eu.bcvsolutions.idm.core.api.generator.ValueGenerator;
import eu.bcvsolutions.idm.core.api.service.IdmGenerateValueService;
import eu.bcvsolutions.idm.core.api.service.ValueGeneratorManager;
import eu.bcvsolutions.idm.core.security.api.service.EnabledEvaluator;

/**
 * Default implementation manager for generating value
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 * @since 9.2.0
 */
@Service("valueGeneratorManager")
public class DefaultValueGeneratorManager implements ValueGeneratorManager {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultValueGeneratorManager.class);

	@Autowired
	private IdmGenerateValueService service;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private EnabledEvaluator enabledEvaluator;
	//
	// generators are cached
	private Map<String, ValueGenerator<? extends AbstractDto>> generators = null; // <generatorType, generator>
	
	@Override
	public <DTO extends AbstractDto> DTO generate(DTO dto) {
		Assert.notNull(dto);
		//
		List<IdmGenerateValueDto> enabledGenerateValues = getEnabledGenerateValues(dto.getClass());
		//
		// iterate over saved and enabled generators in DB for given entity type
		for (IdmGenerateValueDto generateValue : enabledGenerateValues) {
			ValueGenerator<DTO> generator = getGenerator(generateValue);
			if (generator == null) {
				LOG.warn("Generator for type [{}] doesn't exist. Or more generators found for type.", generateValue.getGeneratorType());
				continue;
			}
			if (!generator.supports(dto.getClass())) {
				LOG.warn("Generator for type [{}] doesn't support given dto type [{}]. Fix your configuration [{}].", 
						generateValue.getGeneratorType(), dto.getClass(), generateValue.getId());
				continue;
			}
			//
			DTO generatedDto = generator.generate(dto, generateValue);
			if (generatedDto == null) {
				LOG.debug("Generator [{}] returned null, original dto will be returned", generator.getName());
			} else {
				dto = generatedDto;
			}
		}
		//
		return dto;
	}
	
	@Override
	public List<ValueGeneratorDto> getAvailableGenerators(Class<? extends AbstractDto> dtoType) {
		return getAllGenerators()
				.values()
				.stream()
				.filter(generator -> {
					return !generator.isDisabled() 
							&& enabledEvaluator.isEnabled(generator)
							&& (dtoType == null || generator.supports(dtoType));
				})
				.map(this::toDto)
				.collect(Collectors.toList());
	}
	
	@Override
	public Set<Class<? extends AbstractDto>> getSupportedTypes() {
		return getAllGenerators()
				.values()
				.stream()
				.map(ValueGenerator::getDtoClass)
				.collect(Collectors.toSet());
	}

	@Override
	public boolean supportsGenerating(AbstractDto dto) {
		Assert.notNull(dto);
		//
		return getSupportedTypes().contains(dto.getClass());
	}

	/**
	 * Return generator for given DTO
	 *
	 * @param generatorDto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <E extends AbstractDto> ValueGenerator<E> getGenerator(IdmGenerateValueDto generatorDto) {
		String generatorType = generatorDto.getGeneratorType();
		if (!generators.containsKey(generatorType)) {
			return null;
		}
		//
		return (ValueGenerator<E>) generators.get(generatorType);
	}
	
	/**
	 * Transform {@link ValueGenerator} to {@link ValueGeneratorDto}.
	 * {@link ValueGeneratorDto} was prepared for send to fronted agenda.
	 *
	 * @param valueGenerator
	 * @return
	 */
	private ValueGeneratorDto toDto(ValueGenerator<?> valueGenerator) {
		ValueGeneratorDto valueGeneratorDto = new ValueGeneratorDto();
		valueGeneratorDto.setId(valueGenerator.getId());
		valueGeneratorDto.setDescription(valueGenerator.getDescription());
		valueGeneratorDto.setModule(valueGenerator.getModule());
		valueGeneratorDto.setDtoType(valueGenerator.getDtoClass().getCanonicalName());
		valueGeneratorDto.setDisabled(valueGenerator.isDisabled());
		valueGeneratorDto.setName(valueGenerator.getName());
		valueGeneratorDto.setGeneratorType(AopProxyUtils.ultimateTargetClass(valueGenerator).getCanonicalName());
		valueGeneratorDto.setFormDefinition(valueGenerator.getFormDefinition());
		//
		return valueGeneratorDto;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, ValueGenerator<? extends AbstractDto>> getAllGenerators() {
		if (generators == null) {
			Map<String, ValueGenerator<? extends AbstractDto>> results = new HashMap<>();
			context.getBeansOfType(ValueGenerator.class)
					.values()
					.forEach(generator -> {
						results.put(AopProxyUtils.ultimateTargetClass(generator).getCanonicalName(), generator);
					});
			//
			generators = results;
		}
		return generators;
	}
	
	private List<IdmGenerateValueDto> getEnabledGenerateValues(Class<? extends AbstractDto> dtoType) {
		Assert.notNull(dtoType);
		//
		IdmGenerateValueFilter filter = new IdmGenerateValueFilter();
		filter.setDisabled(Boolean.FALSE);
		filter.setDtoType(dtoType.getCanonicalName());

		// we must create new instance of array list, given list is unmodifiable
		List<IdmGenerateValueDto> generateValues = new ArrayList<>(service.find(filter, null).getContent());

		// sort by order
		Collections.sort(generateValues, new Comparator<IdmGenerateValueDto>() {
			@Override
			public int compare(IdmGenerateValueDto o1, IdmGenerateValueDto o2) {
				return Shorts.compare(o1.getSeq(), o2.getSeq());
			}
		});
		return generateValues;
	}
}
