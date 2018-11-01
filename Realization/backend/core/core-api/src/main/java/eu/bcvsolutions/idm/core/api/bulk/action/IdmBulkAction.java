package eu.bcvsolutions.idm.core.api.bulk.action;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.plugin.core.Plugin;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;

/**
 * Interface for bulk operation
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

public interface IdmBulkAction<DTO extends AbstractDto, F extends BaseFilter>
		extends Plugin<Class<? extends BaseEntity>>, Ordered {

	int DEFAULT_ORDER = 0;
	
	/**
	 * Get list of form attributes
	 *
	 * @return
	 */
	List<IdmFormAttributeDto> getFormAttributes();

	/**
	 * Get name of bulk action
	 *
	 * @return
	 */
	String getName();
	
	/**
	 * Get bulk action
	 *
	 * @return
	 */
	IdmBulkActionDto getAction();

	/**
	 * Set bulk action
	 *
	 * @param action
	 */
	void setAction(IdmBulkActionDto action);
	
	/**
	 * Get module
	 *
	 * @return
	 */
	String getModule();
	
	/**
	 * Return service. With the service will be executed bulk action.
	 *
	 * @return
	 */
	ReadWriteDtoService<DTO, F> getService();
	
	/**
	 * Validate given bulk action. Is necessary specify the action by setter {@link IdmBulkAction#setAction(IdmBulkActionDto)}
	 */
	void validate();
	
	/**
	 * Return authorities required for process one item.
	 *
	 * @return
	 */
	List<String> getAuthorities();

	/**
	 * Prevalidate method is called before start the action. It used for show additional informations to the user.
	 * 
	 * @return
	 */
	ResultModels prevalidate();
	
	/**
	 * Returns {@code true}, when action can be executed without items are selected.
	 * Returns {@code false} by default.
	 * 
	 * @return
	 * @since 9.2.2
	 */
	default boolean showWithoutSelection() {
		return false;
	}
	
	/**
	 * Returns {@code true}, when action can be executed with items are selected (items identifiers or filter is given).
	 * Returns {@code true} by default.
	 * 
	 * @return
	 * @since 9.2.2
	 */
	default boolean showWithSelection() {
		return true;
	}
}
