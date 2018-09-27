package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Size;

import org.joda.time.LocalDate;
import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.ContractState;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.ExternalIdentifiable;
import eu.bcvsolutions.idm.core.api.entity.ValidableEntity;
import io.swagger.annotations.ApiModelProperty;

/**
 * Identity contract - working position
 *
 * @author Svanda
 */
@Relation(collectionRelation = "identityContracts")
public class IdmIdentityContractDto extends FormableDto implements ValidableEntity, ExternalIdentifiable {

	private static final long serialVersionUID = 1L;

	@Size(max = DefaultFieldLengths.NAME)
	@ApiModelProperty(notes = "Unique external identifier.")
	private String externalId;
	@Embedded(dtoClass = IdmIdentityDto.class)
	private UUID identity;
	private LocalDate validFrom;
	private LocalDate validTill;
	@Size(max = DefaultFieldLengths.NAME)
	private String position;
	private boolean externe;
	private boolean disabled;
	private ContractState state;
	private boolean main;
	@Embedded(dtoClass = IdmTreeNodeDto.class)
	private UUID workPosition;
	private String description;
	private Boolean controlledBySlices; // Is true only if contract has some slice. Contract created by slice, cannot be
										// updated directly! Is sets only if DTO is not trimmed!

	public IdmIdentityContractDto() {
	}

	public IdmIdentityContractDto(UUID id) {
		super(id);
	}

	public UUID getIdentity() {
		return identity;
	}

	public void setIdentity(UUID identity) {
		this.identity = identity;
	}

	@Override
	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	@Override
	public LocalDate getValidTill() {
		return validTill;
	}

	public void setValidTill(LocalDate validTill) {
		this.validTill = validTill;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public boolean isExterne() {
		return externe;
	}

	public void setExterne(boolean externe) {
		this.externe = externe;
	}

	public boolean isDisabled() {
		return state == null ? disabled : state.isDisabled();
	}

	public boolean isExcluded() {
		return state == ContractState.EXCLUDED;
	}

	public void setMain(boolean main) {
		this.main = main;
	}

	public boolean isMain() {
		return main;
	}

	public void setWorkPosition(UUID workPosition) {
		this.workPosition = workPosition;
	}

	public UUID getWorkPosition() {
		return workPosition;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setState(ContractState state) {
		this.state = state;
	}

	public ContractState getState() {
		return state;
	}

	public Boolean getControlledBySlices() {
		return controlledBySlices;
	}

	public void setControlledBySlices(Boolean controlledBySlices) {
		this.controlledBySlices = controlledBySlices;
	}

	@Override
	public boolean isValid(LocalDate targetDate) {
		return ValidableEntity.super.isValid(targetDate) && !isDisabled();
	}

	@Override
	public boolean isValidNowOrInFuture() {
		return ValidableEntity.super.isValidNowOrInFuture() && !isDisabled();
	}

	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@Override
	public String getExternalId() {
		return externalId;
	}
}
