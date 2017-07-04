package eu.bcvsolutions.idm.acc.event.processor;

import java.util.List;

import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.filter.SystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.exception.TreeTypeException;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeType;
import eu.bcvsolutions.idm.core.model.event.TreeTypeEvent.TreeTypeEventType;

/**
 * Before tree type delete - check connection on systems mapping
 * 
 * @author Svanda
 *
 */
@Component("accTreeTypeDeleteProcessor")
@Description("Ensures referential integrity. Cannot be disabled.")
public class TreeTypeDeleteProcessor extends AbstractEntityEventProcessor<IdmTreeType> {
	
	public static final String PROCESSOR_NAME = "tree-type-delete-processor";
	private final SysSystemMappingService systemMappingService;
	
	@Autowired
	public TreeTypeDeleteProcessor(SysSystemMappingService systemMappingService) {
		super(TreeTypeEventType.DELETE);
		//
		Assert.notNull(systemMappingService);
		//
		this.systemMappingService = systemMappingService;
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmTreeType> process(EntityEvent<IdmTreeType> event) {
		IdmTreeType treeType = event.getContent();
		Asserts.notNull(treeType, "TreeType must be set!");
		SystemMappingFilter filter = new SystemMappingFilter();
		filter.setTreeTypeId(treeType.getId());
		
		List<SysSystemMapping> mappings = systemMappingService.find(filter, null).getContent();
		long count = mappings.size();
		if (count > 0) {
			throw new TreeTypeException(AccResultCode.SYSTEM_MAPPING_TREE_TYPE_DELETE_FAILED, ImmutableMap.of("treeType", treeType.getName(), "system", mappings.get(0).getSystem().getCode()));
		}
		
		return new DefaultEventResult<>(event, this);
	}
	
	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER - 1;
	}
	
	@Override
	public boolean isDisableable() {
		return false;
	}
}