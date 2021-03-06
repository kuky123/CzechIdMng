package eu.bcvsolutions.idm.core.model.event.processor.contract;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.dto.IdmContractSliceDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmContractSliceFilter;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.processor.ContractSliceProcessor;
import eu.bcvsolutions.idm.core.api.service.ContractSliceManager;
import eu.bcvsolutions.idm.core.api.service.IdmContractSliceService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.model.event.ContractSliceEvent;
import eu.bcvsolutions.idm.core.model.event.ContractSliceEvent.ContractSliceEventType;

/**
 * Update/recalculate contract by slice
 * 
 * @author svandav
 *
 */
@Component
@Description("Update/recalculate contract by slice")
public class ContractSliceSaveRecalculateProcessor extends CoreEventProcessor<IdmContractSliceDto>
		implements ContractSliceProcessor {

	public static final String PROCESSOR_NAME = "contract-slice-save-recalculate-processor";
	//
	@Autowired
	private IdmContractSliceService service;
	@Autowired
	private ContractSliceManager sliceManager;
	@Autowired
	private IdmIdentityContractService contractService;

	public ContractSliceSaveRecalculateProcessor() {
		super(ContractSliceEventType.UPDATE, ContractSliceEventType.CREATE, ContractSliceEventType.EAV_SAVE);
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmContractSliceDto> process(EntityEvent<IdmContractSliceDto> event) {
		IdmContractSliceDto slice = event.getContent();
		IdmContractSliceDto originalSlice = event.getOriginalSource();
		Map<String, Serializable> eventProperties = event.getProperties();

		boolean forceRecalculateCurrentUsingSlice = this
				.getBooleanProperty(IdmContractSliceService.FORCE_RECALCULATE_CURRENT_USING_SLICE, eventProperties);

		if (slice.getIdentity() != null) {
			UUID parentContract = slice.getParentContract();

			// Check if was contractCode changed, if yes, then set parentContract to
			// null (will be recalculated)
			if (originalSlice != null && !Objects.equal(originalSlice.getContractCode(), slice.getContractCode())) {
				slice.setParentContract(null);
				parentContract = null; // When external code changed, link or create new contract is required
			}
			if (originalSlice != null && !Objects.equal(originalSlice.getParentContract(), slice.getParentContract())) {
				slice.setParentContract(null);
				slice.setUsingAsContract(false);
			}

			if (parentContract == null) {
				slice = linkOrCreateContract(slice, eventProperties);
			} else {
				// Update contract by that slice
				if(slice.isUsingAsContract()) {
					IdmIdentityContractDto contract = contractService.get(parentContract);
					sliceManager.updateContractBySlice(contract, slice, eventProperties);
				}
			}
		}

		boolean recalculateUsingAsContract = false;
		UUID parentContract = slice.getParentContract();

		if (originalSlice == null) {
			// Slice is new, we want to recalculate "Is using as contract" field.
			recalculateUsingAsContract = true;
		}

		// Recalculate on change of 'parentContract' field
		boolean parentContractChanged = false;
		if (originalSlice != null && !Objects.equal(originalSlice.getParentContract(), slice.getParentContract())) {
			UUID originalParentContract = originalSlice.getParentContract();
			// Parent contract was changed ... we need recalculate parent contract for
			// original slice
			parentContractChanged = true;
			if (originalParentContract != null) {
				IdmIdentityContractDto originalContract = contractService.get(originalParentContract);
				// Find other slices for original contract
				IdmContractSliceFilter sliceFilter = new IdmContractSliceFilter();
				sliceFilter.setParentContract(originalParentContract);
				List<IdmContractSliceDto> originalSlices = service.find(sliceFilter, null).getContent();
				if (!originalSlices.isEmpty()) {
					IdmContractSliceDto originalNextSlice = sliceManager.findNextSlice(originalSlice, originalSlices);
					IdmContractSliceDto originalSliceToUpdate = originalNextSlice;
					if (originalNextSlice != null) {
						// Next slice exists, update valid-till on previous slice by that slice
						IdmContractSliceDto originalPreviousSlice = sliceManager.findPreviousSlice(originalNextSlice,
								originalSlices);
						if (originalPreviousSlice != null) {
							originalPreviousSlice.setValidTill(originalNextSlice.getValidFrom().minusDays(1));
							originalSliceToUpdate = originalPreviousSlice;
						}
					} else {
						// Next slice does not exists. I means original slice was last. Set valid-till
						// on previous slice to null.
						IdmContractSliceDto originalPreviousSlice = sliceManager.findPreviousSlice(originalSlice,
								originalSlices);
						if (originalPreviousSlice != null
								&& sliceManager.findNextSlice(originalPreviousSlice, originalSlices) == null) {
							originalPreviousSlice.setValidTill(null);
							originalSliceToUpdate = originalPreviousSlice;
						}
					}
					// Save with force recalculation
					service.publish(
							new ContractSliceEvent(ContractSliceEventType.UPDATE, originalSliceToUpdate, ImmutableMap
									.of(IdmContractSliceService.FORCE_RECALCULATE_CURRENT_USING_SLICE, Boolean.TRUE)));
				} else {
					// Parent contract was changed and old contract does not have next slice, we
					// have to delete him.
					contractService.delete(originalContract);
				}
			}
			// Parent contract was changed, want to recalculate "Is using as contract"
			// field.
			recalculateUsingAsContract = true;
		}

		// Recalculate the valid-till on previous slice
		// Evaluates on change of 'validFrom' field or new slice or change the parent
		// contract
		if (originalSlice == null || parentContractChanged
				|| (originalSlice != null && !Objects.equal(originalSlice.getValidFrom(), slice.getValidFrom()))) {
			// Valid from was changed ... we have to change of validity till on previous
			// slice
			if (parentContract != null) {
				// Find other slices for parent contract
				List<IdmContractSliceDto> slices = sliceManager.findAllSlices(parentContract);
				if (!slices.isEmpty()) {
					// Update validity till on this slice and on previous slice
					recalculateValidTill(slice, slices);
					// Update validity till on this original slice and on previous slice (to
					// original slice)
					if (originalSlice != null) {
						IdmContractSliceDto nextSliceForOriginalSlice = sliceManager.findNextSlice(originalSlice,
								slices);
						if (nextSliceForOriginalSlice == null) {
							// Next slice not exists, it means original slice was last
							IdmContractSliceDto previousSliceForOriginalSlice = sliceManager
									.findPreviousSlice(originalSlice, slices);
							if (previousSliceForOriginalSlice != null
									&& sliceManager.findNextSlice(previousSliceForOriginalSlice, slices) == null) {
								previousSliceForOriginalSlice.setValidTill(null);
								// Save with skip this processor
								saveWithoutRecalculate(previousSliceForOriginalSlice);
							}
						}
					}
				}
			}
			// Validity from was changed, want to recalculate "Is using as contract" field.
			recalculateUsingAsContract = true;
		}

		if (recalculateUsingAsContract || forceRecalculateCurrentUsingSlice) {
			IdmContractSliceFilter sliceFilter = new IdmContractSliceFilter();
			sliceFilter.setParentContract(parentContract);
			sliceFilter.setUsingAsContract(Boolean.TRUE);

			IdmContractSliceDto shouldBeSetAsUsing = sliceManager.findValidSlice(parentContract);

			if (shouldBeSetAsUsing != null) {
				shouldBeSetAsUsing = sliceManager.setSliceAsCurrentlyUsing(shouldBeSetAsUsing);
				if (slice.equals(shouldBeSetAsUsing)) {
					// If that slice should be using as contract, then we using returned instance
					// (instead the reload slice from DB)
					slice = shouldBeSetAsUsing;
				}
			}
		}

		// Check if is slice new or contract valid till field was changed.
		if (originalSlice == null
				|| (!Objects.equal(originalSlice.getContractValidTill(), slice.getContractValidTill()))) {
			// If is slice last, then will be to slice valid till copy date of contract
			// valid till
			boolean isSliceLast = sliceManager.findNextSlice(slice, sliceManager.findAllSlices(parentContract)) == null
					? true
					: false;
			if (isSliceLast) {
				slice.setValidTill(null);
				this.saveWithoutRecalculate(slice);
			}
		}

		event.setContent(service.get(slice.getId()));
		return new DefaultEventResult<>(event, this);
	}

	/**
	 * Recalculate valid till on given slice and on previous slice
	 * 
	 * @param slice
	 * @param slices
	 */
	private void recalculateValidTill(IdmContractSliceDto slice, List<IdmContractSliceDto> slices) {
		sliceManager.updateValidTillOnPreviousSlice(slice, slices);
		IdmContractSliceDto nextSlice = sliceManager.findNextSlice(slice, slices);
		if (nextSlice != null) {
			LocalDate validTill = nextSlice.getValidFrom().minusDays(1);
			slice.setValidTill(validTill);
		} else {
			slice.setValidTill(null);
		}
		// Save with skip this processor
		saveWithoutRecalculate(slice);
	}

	/**
	 * Save slice without recalculate ... it means with skip this processor
	 * 
	 * @param slice
	 */
	private void saveWithoutRecalculate(IdmContractSliceDto slice) {
		service.publish(new ContractSliceEvent(ContractSliceEventType.UPDATE, slice,
				ImmutableMap.of(IdmContractSliceService.SKIP_RECALCULATE_CONTRACT_SLICE, Boolean.TRUE)));
	}

	/**
	 * Create or link contract from the slice or create relation on the exists
	 * contract
	 * 
	 * @param slice
	 * @return
	 */
	private IdmContractSliceDto linkOrCreateContract(IdmContractSliceDto slice,
			Map<String, Serializable> eventProperties) {

		String contractCode = slice.getContractCode();
		if (Strings.isNullOrEmpty(contractCode)) {
			// Create new parent contract
			// When new contract is created, then this slice have to be sets as "Is using as
			// contract"
			slice.setUsingAsContract(true);
			IdmIdentityContractDto contract = sliceManager.updateContractBySlice(new IdmIdentityContractDto(), slice, eventProperties);
			slice.setParentContract(contract.getId());

			return service.saveInternal(slice);
		} else {
			// Find other slices
			IdmContractSliceFilter sliceFilter = new IdmContractSliceFilter();
			sliceFilter.setContractCode(contractCode);
			sliceFilter.setIdentity(slice.getIdentity());
			List<IdmContractSliceDto> slices = service.find(sliceFilter, null).getContent();
			// Find contract sets on others slices
			UUID parentContractId = slices.stream()
					.filter(s -> s.getParentContract() != null && !s.getId().equals(slice.getId()))//
					.findFirst()//
					.map(IdmContractSliceDto::getParentContract)//
					.orElse(null);//
			if (parentContractId == null) {
				// Other slices does not have sets contract
				// Create new parent contract
				// When new contract is created, then this slice have to be sets as "Is using as
				// contract"
				slice.setUsingAsContract(true);
				IdmIdentityContractDto contract = sliceManager.updateContractBySlice(new IdmIdentityContractDto(), slice, eventProperties);
				slice.setParentContract(contract.getId());

				return service.saveInternal(slice);
			} else {
				// We found and link on existed contract. We have to do update this contract by
				// slice.
				slice.setParentContract(parentContractId);
				IdmContractSliceDto sliceSaved = service.saveInternal(slice);

				// Update contract by that slice
				if(sliceSaved.isUsingAsContract()) {
					IdmIdentityContractDto contract = contractService.get(sliceSaved.getParentContract());
					sliceManager.updateContractBySlice(contract, sliceSaved, eventProperties);
				}

				return sliceSaved;
			}
		}
	}

	@Override
	public boolean conditional(EntityEvent<IdmContractSliceDto> event) {
		return !this.getBooleanProperty(IdmContractSliceService.SKIP_RECALCULATE_CONTRACT_SLICE, event.getProperties());
	}

	@Override
	public int getOrder() {
		// Execute after save
		return super.getOrder() + 1;
	}

}
