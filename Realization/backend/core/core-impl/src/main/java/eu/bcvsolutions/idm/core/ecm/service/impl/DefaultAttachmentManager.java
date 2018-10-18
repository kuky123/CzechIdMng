package eu.bcvsolutions.idm.core.ecm.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.ecm.api.config.AttachmentConfiguration;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.filter.IdmAttachmentFilter;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.ecm.entity.IdmAttachment;
import eu.bcvsolutions.idm.core.ecm.entity.IdmAttachment_;
import eu.bcvsolutions.idm.core.ecm.repository.IdmAttachmentRepository;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Attachment - saved to FS
 * TODO: delete attachment from FS after transaction is commited
 * 
 * @author Radek Tomiška
 * @since 7.6.0
 */
public class DefaultAttachmentManager 
		extends AbstractReadWriteDtoService<IdmAttachmentDto, IdmAttachment, IdmAttachmentFilter>
		implements AttachmentManager {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultAttachmentManager.class);
	// remove old temp files by this extension? 
	// or create "temp" attachment without owner in the same storage?
	private static final String DEFAULT_TEMP_FILE_EXTENSION = "tmp";
	//
	private final AttachmentConfiguration attachmentConfiguration;
	private final LookupService lookupService;
	
	@Autowired
	public DefaultAttachmentManager(
			IdmAttachmentRepository repository,
			AttachmentConfiguration attachmentConfiguration,
			LookupService lookupService) {
		super(repository);
		//
		Assert.notNull(attachmentConfiguration);
		Assert.notNull(lookupService);
		//
		this.lookupService = lookupService;
		this.attachmentConfiguration = attachmentConfiguration;
	}
	
	@Override
	public AuthorizableType getAuthorizableType() {
		return null; // internal agenda only for now
	}
	
	@Override
	@Transactional
	public void deleteInternal(IdmAttachmentDto attachment) {
		super.deleteInternal(attachment);
		//
		// remove data from FS in second cycle after end of delete from DB.
		deleteFile(attachment.getContentPath());
		//
		LOG.debug("Attachment [{}] was deleted from FS [{}].", attachment.getId(), attachment.getContentPath());
	}

	@Override
	@Transactional
	public IdmAttachmentDto saveAttachment(Identifiable owner, IdmAttachmentDto attachment, BasePermission... permission) {
		Assert.notNull(attachment, "Insert attachment");
		Assert.notNull(attachment.getInputData(), "Insert binary data");
		//
		// set owner - can be preset externally
		if (StringUtils.isEmpty(attachment.getOwnerType())) {
			attachment.setOwnerType(getOwnerType(owner));
		}
		if (attachment.getOwnerId() == null) {
			attachment.setOwnerId(getOwnerId(owner));
		}
		//
		if (attachment.getId() != null) {
			return updateAttachment(attachment, permission);
		}
		if (attachment.getEncoding() == null) {
			attachment.setEncoding(AttachableEntity.DEFAULT_ENCODING);
		}
		if (attachment.getVersionNumber() == null) {
			attachment.setVersionNumber(1);
		}
		if (StringUtils.isEmpty(attachment.getVersionLabel())) {
			attachment.setVersionLabel(attachment.getVersionNumber() + ".0");
		}
		File targetFile = null;
		try {
            // save to FS
			// generate guid
			attachment.setContentId(UUID.randomUUID());
			// save file
			targetFile = saveFile(attachment, attachment.getInputData());
			return save(attachment, permission);
		} catch (Exception ex) {
			FileUtils.deleteQuietly(targetFile);
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_CREATE_FAILED, ImmutableMap.of(
					"attachmentName", attachment.getName(),
					"ownerType", attachment.getOwnerType(),
					"ownerId", attachment.getOwnerId() == null ? "" : attachment.getOwnerId().toString())
					, ex);
		} finally {
			IOUtils.closeQuietly(attachment.getInputData());
		}
	}
	
	@Override
	@Transactional
	public IdmAttachmentDto saveAttachmentVersion(Identifiable owner, IdmAttachmentDto attachment, UUID previousVersionId, BasePermission... permission) {
		IdmAttachmentDto previousVersion = null;
		if (previousVersionId != null) {
			 previousVersion = get(previousVersionId);
		}
		//
		return saveAttachmentVersion(owner, attachment, previousVersion, permission);
	}
	
	/**
	 * Save new version of attachment
	 *
	 * @param attachment
	 * @param previousVersion if not use, then is searching attachment for same
	 * object by name
	 * @return
	 */
	@Override
	@Transactional
	public IdmAttachmentDto saveAttachmentVersion(Identifiable owner, IdmAttachmentDto attachment, IdmAttachmentDto previousVersion, BasePermission... permission) {
		Assert.notNull(owner);
		Assert.notNull(attachment, "Insert attachment");
		//
		if (previousVersion == null) {
			List<IdmAttachmentDto> previousVersions = findLastVersionByOwnerAndName(getOwnerType(owner), getOwnerId(owner), attachment.getName(), permission);
			if (previousVersions.size() == 1) {
				previousVersion = previousVersions.get(0);
			}
		}
		if (previousVersion == null) {
			return saveAttachment(owner, attachment, permission);
		}
		attachment.setVersionNumber(previousVersion.getVersionNumber() + 1);
		attachment.setVersionLabel(attachment.getVersionNumber() + ".0"); // TODO: work with minor versions
		if (previousVersion.getParent() != null) {
			attachment.setParent(previousVersion.getParent());
		} else {
			attachment.setParent(previousVersion.getId());
		}
		attachment = saveAttachment(owner, attachment, permission);
		previousVersion.setNextVersion(attachment.getId());
		save(previousVersion, permission);
		//
		return attachment;
	}
	
	@Override
	@Transactional
	public IdmAttachmentDto updateAttachment(IdmAttachmentDto attachment, BasePermission... permission) {
		Assert.notNull(attachment, "Insert attachment");
		Assert.notNull(attachment.getId(), "Insert saved attachment");
		//
		if (attachment.getEncoding() == null) {
			attachment.setEncoding(AttachableEntity.DEFAULT_ENCODING);
		}
		File targetFile = null;
		try {

			String previousPath = null;
			if (attachment.getInputData() != null) {
				previousPath = attachment.getContentPath();
				targetFile = saveFile(attachment, attachment.getInputData());
			}
			attachment = save(attachment, permission);
			if (previousPath != null) {
				deleteFile(previousPath);
			}
		} catch (Exception ex) {
			FileUtils.deleteQuietly(targetFile);
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_UPDATE_FAILED, ImmutableMap.of(
					"attachment", attachment.getId().toString(),
					"ownerType", attachment.getOwnerType(),
					"ownerId", attachment.getOwnerId() == null ? "" : attachment.getOwnerId().toString())
					, ex);
		} finally {
			IOUtils.closeQuietly(attachment.getInputData());
		}
		return attachment;
	}
	
	@Override
	public InputStream getAttachmentData(UUID attachmentId, BasePermission... permission) {
		if(attachmentId == null) {
			return null;
		}
		return getAttachmentData(get(attachmentId, permission));
	}
	
	@Override
	@Transactional
	public void deleteAttachment(IdmAttachmentDto attachment, BasePermission... permission) {
		deleteAttachment(attachment.getId(), permission);
	}
	
	@Override
	@Transactional
	public void deleteAttachment(UUID attachmentId, BasePermission... permission) {
		getAttachmentVersions(attachmentId).forEach(version -> {
			delete(version, permission);
		});
	}
	
	@Override
	@Transactional
	public void deleteAttachments(Identifiable owner, BasePermission... permission) {
		Assert.notNull(owner);
		//
		deleteAttachments(getOwnerId(owner), getOwnerType(owner), permission);
	}
	
	@Override
	@Transactional
	public void deleteAttachments(UUID ownerId, String ownerType, BasePermission... permission) {
		Assert.notNull(ownerId);
		Assert.notNull(ownerType);
		//
		getAttachments(ownerId, ownerType, null).forEach(attachment -> {
			deleteAttachment(attachment, permission);
		});
	}
	
	@Override
	@Transactional
	public Page<IdmAttachmentDto> getAttachments(Identifiable owner, Pageable pageable, BasePermission... permission) {
		Assert.notNull(owner);
		//
		return getAttachments(getOwnerId(owner), getOwnerType(owner), pageable, permission);
	}
	
	@Override
	@Transactional
	public Page<IdmAttachmentDto> getAttachments(UUID ownerId, String ownerType, Pageable pageable, BasePermission... permission) {
		Assert.notNull(ownerId);
		Assert.notNull(ownerType);
		//
		IdmAttachmentFilter filter = new IdmAttachmentFilter();
		filter.setOwnerType(ownerType);
		filter.setOwnerId(ownerId);
		filter.setLastVersionOnly(Boolean.TRUE);
		//
		return find(filter, pageable, permission);
	}
	
	@Override
	@Transactional
	public List<IdmAttachmentDto> getAttachmentVersions(UUID attachmentId, BasePermission... permission) {
		Assert.notNull(attachmentId);
		//
		IdmAttachmentFilter filter = new IdmAttachmentFilter();
		filter.setVersionsFor(attachmentId);
		//
		return find(
				filter, 
				new PageRequest(0, Integer.MAX_VALUE, new Sort(Direction.DESC, IdmAttachment_.versionNumber.getName())),
				permission)
				.getContent();
	}
	
	@Override
	public File createTempFile() {
		try {			
			File tempFile = File.createTempFile(UUID.randomUUID().toString(), "." + DEFAULT_TEMP_FILE_EXTENSION, new File(getTempPath()));
			tempFile.deleteOnExit();
			return tempFile;
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_CREATE_TEMP_FILE_FAILED, ImmutableMap.of(
					"extension", DEFAULT_TEMP_FILE_EXTENSION,
					"temp", attachmentConfiguration.getTempPath())
					, ex);
		}
	}
	
	/**
	 * Purge old temporary files and attachments once per day.
	 * Temporary files older than configured ttl will be purged.
	 * Temporary uploaded attachments older than configured ttl will be purged.
	 * 
	 * @return purged files count
	 */
	@Scheduled(fixedDelay = 86400000) // once per day
	public void purgeTemp() {	
		int purgedFiles = 0;
		long ttl = attachmentConfiguration.getTempTtl();
		if (ttl == 0) {
			LOG.warn("Removing old temporary files is disabled. Configure property [{}] - time to live in milliseconds (greater than zero).", AttachmentConfiguration.PROPERTY_TEMP_TTL);
			return;
		}
		//
		// purge older temporary files than purge time
		long purgeTime = System.currentTimeMillis() - ttl;
		File temp = new File(getTempPath());
		if (temp.isDirectory()) {
		    File[] files = temp.listFiles();
		    if (files != null) {
		    	for (File f : files) {
		    		try {
			    		if (f.getName().endsWith("." + DEFAULT_TEMP_FILE_EXTENSION) && f.lastModified() < purgeTime) {
			    			f.delete();
			    			purgedFiles++;
			    		}
		    		} catch (Exception ex) {
		    			LOG.error("Removing old temporary [.{}] file [{}] failed", DEFAULT_TEMP_FILE_EXTENSION, f.getName(), ex);
		    		}
		    	}
		    }
		}
		//
		// purge temporary attachments
		IdmAttachmentFilter filter = new IdmAttachmentFilter();
		filter.setOwnerType(TEMPORARY_ATTACHMENT_OWNER_TYPE);
		filter.setCreatedBefore(DateTime.now().minusMillis(Math.toIntExact(ttl)));
		for (IdmAttachmentDto attachment : find(filter, null)) {
			delete(attachment);
			purgedFiles++;
		}
		//
		LOG.debug("Temporary files were purged [{}]", purgedFiles);
	}
	
	@Override
	public String getOwnerType(Identifiable owner) {
		Assert.notNull(owner);
		//
		return getOwnerType(owner.getClass());
	}
	
	/**
	 * Owner type has to be entity class - dto class can be given.
	 * 
	 * @param ownerType
	 * @return
	 */
	@Override
	public String getOwnerType(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType);
		//
		// dto class was given
		Class<? extends AttachableEntity> ownerEntityType = getAttachableOwnerType(ownerType);
		if (ownerEntityType == null) {
			throw new IllegalArgumentException(String.format("Owner type [%s] has to generatize [AttachableEntity]", ownerType));
		}
		return ownerEntityType.getCanonicalName();
	}
	
	@Override
	protected List<Predicate> toPredicates(Root<IdmAttachment> root, CriteriaQuery<?> query, CriteriaBuilder builder, IdmAttachmentFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		//
		// text - owner type, owner code
		String text = filter.getText();
		if (StringUtils.isNotEmpty(text)) {
			text = text.toLowerCase();
			predicates.add(builder.or(
					builder.like(builder.lower(root.get(IdmAttachment_.name)), "%" + text + "%"),
					builder.like(builder.lower(root.get(IdmAttachment_.description)), "%" + text + "%")));
		}
		// owner id
		UUID ownerId = filter.getOwnerId();
		if (ownerId != null) {
			predicates.add(builder.equal(root.get(IdmAttachment_.ownerId), ownerId));
		}
		// owner type
		String ownerType = filter.getOwnerType();
		if (StringUtils.isNotEmpty(ownerType)) {
			predicates.add(builder.equal(root.get(IdmAttachment_.ownerType), ownerType));
		}
		// name
		String name = filter.getName();
		if (StringUtils.isNotEmpty(name)) {
			predicates.add(builder.equal(root.get(IdmAttachment_.name), name));
		}
		// last version only
		Boolean lastVersionOnly = filter.getLastVersionOnly();
		if (lastVersionOnly != null && lastVersionOnly) {
			predicates.add(builder.isNull(root.get(IdmAttachment_.nextVersion)));
		}
		// versions
		UUID versionsFor = filter.getVersionsFor();
		if (versionsFor != null) {
			CriteriaQuery<IdmAttachment> subCriteriaQuery = builder.createQuery(getEntityClass());
			Subquery<IdmAttachment> subquery = subCriteriaQuery.subquery(getEntityClass());
			Root<IdmAttachment> subRoot = subquery.from(getEntityClass());
			subquery.select(subRoot.get(IdmAttachment_.parent));
			
			subquery.where(builder.equal(subRoot.get(IdmAttachment_.id), versionsFor));
			
			predicates.add(builder.or(
					builder.equal(root.get(IdmAttachment_.parent).get(IdmAttachment_.id), versionsFor),
					builder.equal(root.get(IdmAttachment_.id), versionsFor),
					builder.equal(root.get(IdmAttachment_.parent).get(IdmAttachment_.id), subquery),
					builder.equal(root, subquery)
			));
		}
		// created before
		DateTime createdBefore = filter.getCreatedBefore();
		if (createdBefore != null) {
			predicates.add(builder.lessThan(root.get(IdmAttachment_.created), createdBefore));
		}
		// created after
		DateTime createdAfter = filter.getCreatedAfter();
		if (createdAfter != null) {
			predicates.add(builder.greaterThan(root.get(IdmAttachment_.created), createdAfter));
		}
		//
		return predicates;
	}
	
	private InputStream getAttachmentData(IdmAttachmentDto attachment) {
		Assert.notNull(attachment, "Attachment is required");
		//
		try {
			return new FileInputStream(getStoragePath() + attachment.getContentPath());
		} catch (FileNotFoundException ex) {
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_DATA_NOT_FOUND, ImmutableMap.of(
					"attachmentId", attachment.getId(),
					"attachmentName", attachment.getName(),
					"contentPath", attachment.getContentPath())
					, ex);
		}
	}
	
	private List<IdmAttachmentDto> findLastVersionByOwnerAndName(String ownerType, UUID ownerId, String name, BasePermission... permission) {
		Assert.notNull(ownerType, "Insert type of owner");
		Assert.notNull(ownerId, "Insert ID of owner");
		Assert.notNull(name, "Insert name of attachment");
		//
		IdmAttachmentFilter filter = new IdmAttachmentFilter();
		filter.setOwnerType(ownerType);
		filter.setOwnerId(ownerId);
		filter.setName(name);
		filter.setLastVersionOnly(Boolean.TRUE);
		//
		return find(filter, new PageRequest(0, Integer.MAX_VALUE, new Sort(Direction.ASC, IdmAttachment_.name.getName())), permission).getContent();
	}
	
	private File saveFile(IdmAttachmentDto attachment, InputStream in) throws Exception {
		
		File targetFile = null;
		FileOutputStream os = null;
		try {
			// create path
			Calendar calendar = Calendar.getInstance();
			String path = "/" + calendar.get(Calendar.YEAR)
							+ "/" + (calendar.get(Calendar.MONTH) + 1)
							+ "/" + calendar.get(Calendar.DATE);
			File directory = new File(getStoragePath() + path);
			if (!directory.exists()) {
				directory.mkdirs();
			}
			// file not has same guid as on FS - guid attachment is not change, will be create new version
			attachment.setContentPath(path + "/" + UUID.randomUUID() + ".bin");
			// save binary data
			targetFile = new File(getStoragePath() + attachment.getContentPath());
			os = new FileOutputStream(targetFile);
			IOUtils.copy(in, os);
			attachment.setFilesize(targetFile.length());
			return targetFile;
		} catch (Exception ex) {
			FileUtils.deleteQuietly(targetFile);
			throw ex;
		} finally {
			attachment.setInputData(null);
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(os);
		}
	}
	
	private void deleteFile(String path) {
		if (StringUtils.isEmpty(path)) {
			return; // nothing to remove
		}
		// remove data from FS
		File targetFile = new File(getStoragePath() + path);
		FileUtils.deleteQuietly(targetFile);
	}

	/**
	 * Configured storage path
	 * 
	 * @return
	 */
	private String getStoragePath() {
		return attachmentConfiguration.getStoragePath();
	}
	
	/**
	 * Configured temp path
	 * 
	 * @return
	 */
	private String getTempPath() {
		return attachmentConfiguration.getTempPath();
	}
	
	/**
	 * UUID identifier from given owner.
	 * 
	 * @param owner
	 * @return
	 */
	private UUID getOwnerId(Identifiable owner) {
		if (owner == null || owner.getId() == null) {
			return null;
		}		
		Assert.isInstanceOf(UUID.class, owner.getId(), "Entity with UUID identifier is supported as owner for attachments.");
		//
		return (UUID) owner.getId();
	}
	
	/**
	 * Returns {@link FormableEntity}. Owner type has to be entity class - dto class can be given.
	 * 
	 * @param ownerType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends AttachableEntity> getAttachableOwnerType(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType, "Owner type is required!");
		// formable entity class was given
		if (AttachableEntity.class.isAssignableFrom(ownerType)) {
			return (Class<? extends AttachableEntity>) ownerType;
		}
		// dto class was given
		Class<?> ownerEntityType = lookupService.getEntityClass(ownerType);
		if (AttachableEntity.class.isAssignableFrom(ownerEntityType)) {
			return (Class<? extends AttachableEntity>) ownerEntityType;
		}
		return null;
	}
}
