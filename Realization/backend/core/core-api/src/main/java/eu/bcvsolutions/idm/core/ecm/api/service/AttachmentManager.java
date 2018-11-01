package eu.bcvsolutions.idm.core.ecm.api.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.filter.IdmAttachmentFilter;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Work with attachments
 * - attachment can be assigned to {@link AttachableEntity}, which has to have {@link UUID} identifier.
 * - {@link AbstractEntity} type or {@link AbstractDto} can be used as owner type.
 * - when owner is deleted, then all attachments have to be deleted to - override owner's service delete method properly.
 * 
 * @author Radek Tomiška
 * @since 7.6.0
 */
public interface AttachmentManager extends 
		ReadWriteDtoService<IdmAttachmentDto, IdmAttachmentFilter>,
		AuthorizableService<IdmAttachmentDto> {
	
	/**
	 * Temporary attachments - they are uploaded without owner.
	 */
	String TEMPORARY_ATTACHMENT_OWNER_TYPE = "TEMP";

	/**
	 * Save attachment. Closes given data input stream automatically.
	 * 
	 * @param owner
	 * @param attachment
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmAttachmentDto saveAttachment(Identifiable owner, IdmAttachmentDto attachment, BasePermission... permission);

	/**
	 * Persist new version of attachment. Closes given data input stream automatically.
	 * 
	 * @param owner
	 * @param attachment
	 * @param previousVersion [optional] if its not specified, then is searching attachment for same object by name
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmAttachmentDto saveAttachmentVersion(Identifiable owner, IdmAttachmentDto attachment, IdmAttachmentDto previousVersion, BasePermission... permission);
	
	/**
	 * Persist new version of attachment. Closes given data input stream automatically.
	 * 
	 * @param owner
	 * @param attachment
	 * @param previousVersionId  [optional] if its not specified, then is searching attachment for same object by name
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmAttachmentDto saveAttachmentVersion(Identifiable owner, IdmAttachmentDto attachment, UUID previousVersionId, BasePermission... permission);
	
	/**
	 * Update attachment - replaces attachment binary data and updates metadata. 
	 * Doesn't create new attachment version.
	 * Usable mainly for update attachment metadata (e.g. description)
	 * Closes given data input stream automatically.
	 * 
	 * @param attachment
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	IdmAttachmentDto updateAttachment(IdmAttachmentDto attachment, BasePermission... permission);
	
	/**
	 * Returns owner's attachments in last version.
	 * 
	 * @param owner
	 * @param pageable
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	Page<IdmAttachmentDto> getAttachments(Identifiable owner, Pageable pageable, BasePermission... permission);
	
	/**
	 * Returns owner's attachments in last version.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param pageable
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @since 9.2.2
	 */
	Page<IdmAttachmentDto> getAttachments(UUID ownerId, String ownerType, Pageable pageable, BasePermission... permission);
	
	/**
	 * Returns all attachment versions orderer from newer to older.
	 * 
	 * @param attachmentId
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	List<IdmAttachmentDto> getAttachmentVersions(UUID attachmentId, BasePermission... permission);

	/**
	 * Binary data of attachment
	 * 
	 * @param attachmentId
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	InputStream getAttachmentData(UUID attachmentId, BasePermission... permission);
	
	/**
	 * Deletes given attachment. All versions will be removed
	 * 
	 * @param attachment
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteAttachment(IdmAttachmentDto attachment, BasePermission... permission);
	
	/**
	 * Deletes given attachment. All versions will be removed
	 * 
	 * @param attachmentId
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @since 9.0.0
	 */
	void deleteAttachment(UUID attachmentId, BasePermission... permission);
	
	/**
	 * Deletes all attachments by given owner
	 * 
	 * @param owner
	 * @param permission permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	void deleteAttachments(Identifiable owner, BasePermission... permission);
	
	/**
	 * Deletes all attachments by given owner type and id 
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param permission permissions to evaluate (AND)
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @since 9.2.2
	 */
	void deleteAttachments(UUID ownerId, String ownerType, BasePermission... permission);
	
	/**
	 *  Create temporary file with default ("bin") extension
	 * 
	 * @return
	 */
	File createTempFile();
	
	/**
	 * Owner type has to be entity class - dto class can be given.
	 * 
	 * @param owner
	 * @return
	 * @since 9.2.2
	 */
	String getOwnerType(Identifiable owner);
	
	/**
	 * Owner type has to be entity class - dto class can be given.
	 * 
	 * @param ownerType
	 * @return
	 * @since 9.2.2
	 */
	String getOwnerType(Class<? extends Identifiable> ownerType);
}