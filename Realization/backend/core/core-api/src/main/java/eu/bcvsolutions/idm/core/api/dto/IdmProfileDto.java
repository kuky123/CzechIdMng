package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Size;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import io.swagger.annotations.ApiModel;

/**
 * Dto for profile
 * 
 * @author Radek Tomiška
 *
 */
@Relation(collectionRelation = "profiles")
@ApiModel(description = "Identity profile - e.g. profile image, preffered language")
public class IdmProfileDto extends AbstractDto  {

	private static final long serialVersionUID = 1L;
	//
	@Embedded(dtoClass = IdmIdentityDto.class)
	private UUID identity;
	@Embedded(dtoClass = IdmAttachmentDto.class)
	private UUID image;
	@Size(max = DefaultFieldLengths.ENUMARATION)
	private String preferredLanguage;
	private boolean navigationCollapsed;

	public IdmProfileDto() {
	}
	
	public IdmProfileDto(UUID id) {
		super(id);
	}

	public UUID getIdentity() {
		return identity;
	}

	public void setIdentity(UUID identity) {
		this.identity = identity;
	}

	public UUID getImage() {
		return image;
	}

	public void setImage(UUID image) {
		this.image = image;
	}

	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}
	
	public boolean isNavigationCollapsed() {
		return navigationCollapsed;
	}
	
	public void setNavigationCollapsed(boolean navigationCollapsed) {
		this.navigationCollapsed = navigationCollapsed;
	}
}
