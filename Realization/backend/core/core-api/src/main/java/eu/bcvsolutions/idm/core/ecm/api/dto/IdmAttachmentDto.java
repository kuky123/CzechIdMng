package eu.bcvsolutions.idm.core.ecm.api.dto;

import java.io.InputStream;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;

/**
 * Attachment
 * 
 * @author Radek Tomiška
 * @since 7.6.0
 */
@Relation(collectionRelation = "attachments")
public class IdmAttachmentDto extends AbstractDto {

	private static final long serialVersionUID = 1L;
	//
	@Size(max = DefaultFieldLengths.NAME)
	private String ownerType;
	private UUID ownerId;
	private UUID contentId; // identifier on FS
	@Size(max = 512)
	private String contentPath; // path on FS in attachment storage
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	private String name;
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	private String description;
	@Size(max = DefaultFieldLengths.NAME)
	private String mimetype;
	@Size(max = 100)
	private String encoding;
	@Max(999999999999999999l)
	private Long filesize;
	@Size(max = 50)
	private String attachmentType;
	@Size(max = 50)
	private String ownerState;
	private Integer versionNumber;
	private String versionLabel;
	@Embedded(dtoClass = IdmAttachmentDto.class, enabled = false)
	private UUID parent;
	@Embedded(dtoClass = IdmAttachmentDto.class, enabled = false)
	private UUID nextVersion;
	//
	private transient InputStream inputData; // transient binary data for saving

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(UUID contentId) {
		this.contentId = contentId;
	}

	public String getContentPath() {
		return contentPath;
	}

	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public Long getFilesize() {
		return filesize;
	}

	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}

	public String getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(String attachmentType) {
		this.attachmentType = attachmentType;
	}

	public String getOwnerState() {
		return ownerState;
	}

	public void setOwnerState(String ownerState) {
		this.ownerState = ownerState;
	}

	public Integer getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}

	public UUID getParent() {
		return parent;
	}

	public void setParent(UUID parent) {
		this.parent = parent;
	}

	public UUID getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(UUID nextVersion) {
		this.nextVersion = nextVersion;
	}
	
	public void setInputData(InputStream inputData) {
		this.inputData = inputData;
	}

	public InputStream getInputData() {
		return inputData;
	}
}
