package eu.bcvsolutions.idm.core.model.event.processor.identity;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.model.domain.RoleRequestState;
import eu.bcvsolutions.idm.core.model.dto.IdmIdentityRoleValidRequestDto;
import eu.bcvsolutions.idm.core.model.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.model.dto.filter.ConceptRoleRequestFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent.IdentityRoleEventType;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityRoleRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleValidRequestService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleRequestService;

/**
 * Delete identity role
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Description("Deletes identity role from repository.")
public class IdentityRoleDeleteProcessor extends CoreEventProcessor<IdmIdentityRole> {

	public static final String PROCESSOR_NAME = "identity-role-delete-processor";
	private final IdmIdentityRoleRepository repository;
	private final IdmConceptRoleRequestService conceptRequestService;
	private final IdmRoleRequestService roleRequestService;
	private final IdmIdentityRoleValidRequestService identityRoleValidRequestService;

	@Autowired
	public IdentityRoleDeleteProcessor(IdmIdentityRoleRepository repository,
			IdmConceptRoleRequestService conceptRequestService, IdmRoleRequestService roleRequestService,
			IdmIdentityRoleValidRequestService identityRoleValidRequestService) {
		super(IdentityRoleEventType.DELETE);
		//
		Assert.notNull(repository);
		Assert.notNull(conceptRequestService);
		Assert.notNull(roleRequestService);
		Assert.notNull(identityRoleValidRequestService);
		//
		this.repository = repository;
		this.conceptRequestService = conceptRequestService;
		this.roleRequestService = roleRequestService;
		this.identityRoleValidRequestService = identityRoleValidRequestService;
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmIdentityRole> process(EntityEvent<IdmIdentityRole> event) {
		IdmIdentityRole identityRole = event.getContent();

		// Find all concepts and remove relation on identity role
		ConceptRoleRequestFilter conceptRequestFilter = new ConceptRoleRequestFilter();
		conceptRequestFilter.setIdentityRoleId(identityRole.getId());
		conceptRequestService.findDto(conceptRequestFilter, null).getContent().forEach(concept -> {
			IdmRoleRequestDto request = roleRequestService.getDto(concept.getRoleRequest());
			String message = null;
			if (concept.getState().isTerminatedState()) {
				message = MessageFormat.format(
						"IdentityRole [{0}] (reqested in concept [{1}]) was deleted (not from this role request)!",
						identityRole.getId(), concept.getId());
			} else {
				message = MessageFormat.format(
						"Request change in concept [{0}], was not executed, because requested IdentityRole [{1}] was deleted (not from this role request)!",
						concept.getId(), identityRole.getId());
				concept.setState(RoleRequestState.CANCELED);
			}
			roleRequestService.addToLog(request, message);
			conceptRequestService.addToLog(concept, message);
			concept.setIdentityRole(null);

			roleRequestService.save(request);
			conceptRequestService.save(concept);
		});
		//
		// remove all IdentityRoleValidRequest for this role
		List<IdmIdentityRoleValidRequestDto> validRequests = identityRoleValidRequestService.findAllValidRequestForIdentityRoleId(identityRole.getId());
		identityRoleValidRequestService.deleteAll(validRequests);
		//
		// Delete identity role
		repository.delete(identityRole);
		//
		return new DefaultEventResult<>(event, this);
	}
}