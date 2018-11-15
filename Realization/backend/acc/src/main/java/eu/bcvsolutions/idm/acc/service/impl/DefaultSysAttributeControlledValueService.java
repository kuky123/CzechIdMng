package eu.bcvsolutions.idm.acc.service.impl;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.dto.SysAttributeControlledValueDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysAttributeControlledValueFilter;
import eu.bcvsolutions.idm.acc.entity.SysAttributeControlledValue;
import eu.bcvsolutions.idm.acc.entity.SysAttributeControlledValue_;
import eu.bcvsolutions.idm.acc.repository.SysAttributeControlledValueRepository;
import eu.bcvsolutions.idm.acc.service.api.SysAttributeControlledValueService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;

/**
 * Service - Controlled value for attribute DTO. Is using in the provisioning
 * merge.
 * 
 * @author Vít Švanda
 *
 */
@Service("sysAttributeControlledValueService")
public class DefaultSysAttributeControlledValueService extends
		AbstractEventableDtoService<SysAttributeControlledValueDto, SysAttributeControlledValue, SysAttributeControlledValueFilter>
		implements SysAttributeControlledValueService {

	@Autowired
	private SysSystemAttributeMappingService systemAttributeMappingService;

	@Autowired
	public DefaultSysAttributeControlledValueService(SysAttributeControlledValueRepository repository,
			EntityEventManager entityEventManager) {
		super(repository, entityEventManager);
	}

	@Transactional
	@Override
	public void setControlledValues(SysSystemAttributeMappingDto attributeMapping,
			List<Serializable> controlledAttributeValues) {
		Assert.notNull(attributeMapping);
		Assert.notNull(controlledAttributeValues);

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(attributeMapping.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.FALSE);

		// Search controlled values for that attribute
		List<SysAttributeControlledValueDto> controlledValues = this.find(attributeControlledValueFilter, null) //
				.getContent();

		// Search values for which does not exists same controlled value (for given
		// attribute)
		List<Serializable> valuesToAdd = controlledAttributeValues.stream().filter(newValue -> { //
			return !controlledValues.stream() //
					.filter(controlledValue -> controlledValue.getValue().equals(newValue)) //
					.findFirst() //
					.isPresent(); //
		}) //
				.collect(Collectors.toList());

		// Search old controlled values which does not exists in new definition
		List<SysAttributeControlledValueDto> controlledValuesToDelete = controlledValues.stream()
				.filter(controlledValue -> { //
					return !controlledAttributeValues.stream() //
							.filter(newValue -> controlledValue.getValue().equals(newValue)) //
							.findFirst() //
							.isPresent(); //
				}) //
				.collect(Collectors.toList());

		// Delete old values
		controlledValuesToDelete.forEach(controlledValue -> this.delete(controlledValue));

		// Persists new controlled values
		valuesToAdd.forEach(valueToAdd -> {
			SysAttributeControlledValueDto controlledValue = new SysAttributeControlledValueDto();
			controlledValue.setAttributeMapping(attributeMapping.getId());
			controlledValue.setHistoricValue(false);
			controlledValue.setValue(valueToAdd);
			controlledValue = this.save(controlledValue);
		});

		// Search historic controlled values for that attribute
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);
		List<SysAttributeControlledValueDto> historicControlledValues = this //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.collect(Collectors.toList());

		List<SysAttributeControlledValueDto> historicValuesToDelete = historicControlledValues.stream() //
				.filter(historicValue -> controlledAttributeValues.contains(historicValue.getValue())) //
				.collect(Collectors.toList());
		
		// If historic value exists in current definition, then will be deleted
		historicValuesToDelete.forEach(historicValue -> {
			this.delete(historicValue);
		});

		// Controlled values are synchronized now, so we can set evict to false
		attributeMapping.setEvictControlledValuesCache(false);
		systemAttributeMappingService.save(attributeMapping);

	}

	@Transactional
	@Override
	public void addHistoricValue(SysSystemAttributeMappingDto attributeMapping, Serializable value) {
		Assert.notNull(attributeMapping);
		Assert.notNull(value);

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(attributeMapping.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		// Search historic values for that attribute
		List<SysAttributeControlledValueDto> historicValues = this.find(attributeControlledValueFilter, null) //
				.getContent();

		boolean historicValueExists = historicValues.stream() //
				.filter(historicValue -> historicValue.getValue().equals(value)) //
				.findFirst() //
				.isPresent();

		if (!historicValueExists) {
			SysAttributeControlledValueDto historicValue = new SysAttributeControlledValueDto();
			historicValue.setAttributeMapping(attributeMapping.getId());
			historicValue.setHistoricValue(true);
			historicValue.setValue(value);
			this.save(historicValue);
		}
	}

	@Override
	protected List<Predicate> toPredicates(Root<SysAttributeControlledValue> root,
			javax.persistence.criteria.CriteriaQuery<?> query, CriteriaBuilder builder,
			SysAttributeControlledValueFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		if (filter.getHistoricValue() != null) {
			predicates.add(
					builder.equal(root.get(SysAttributeControlledValue_.historicValue), filter.getHistoricValue()));
		}
		if (filter.getAttributeMappingId() != null) {
			predicates.add(builder.equal(
					root.get(SysAttributeControlledValue_.attributeMapping).get(SysAttributeControlledValue_.id),
					filter.getAttributeMappingId()));
		}

		return predicates;
	}
}
