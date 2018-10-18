package eu.bcvsolutions.idm.core.ecm.service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.ecm.api.config.AttachmentConfiguration;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.filter.IdmAttachmentFilter;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.ecm.service.impl.DefaultAttachmentManager;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Attachments
 * - CRUD
 * - versions
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class DefaultAttachmentManagerIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ApplicationContext context;
	@Autowired private AttachmentConfiguration attachmentConfiguration;
	//
	private DefaultAttachmentManager attachmentManager;
	
	@Before
	public void init() {
		attachmentManager = context.getAutowireCapableBeanFactory().createBean(DefaultAttachmentManager.class);
	}	
	
	@Test
	public void testCreateAttachment() throws IOException {
		Identifiable owner = new TestOwnerEntity(UUID.randomUUID());
		String content = "test data";
		IdmAttachmentDto attachment = prepareAttachment(content);
		//
		attachmentManager.saveAttachment(owner, attachment);
		List<IdmAttachmentDto> attachments = attachmentManager.getAttachments(owner, null).getContent();
		//
		Assert.assertEquals(1, attachments.size());
		IdmAttachmentDto createdAttachment = attachments.get(0);
		Assert.assertEquals(attachment.getName(), createdAttachment.getName());
		Assert.assertEquals(attachment.getMimetype(), createdAttachment.getMimetype());
		Assert.assertEquals(owner.getClass().getCanonicalName(), createdAttachment.getOwnerType());
		Assert.assertEquals(owner.getId(), createdAttachment.getOwnerId());
		Assert.assertNotNull(createdAttachment.getContentId());
		Assert.assertNotNull(createdAttachment.getContentPath());
		Assert.assertNull(createdAttachment.getParent());
		Assert.assertNull(createdAttachment.getNextVersion());
		Assert.assertEquals(Integer.valueOf(1), createdAttachment.getVersionNumber());
		Assert.assertEquals("1.0", createdAttachment.getVersionLabel());
		Assert.assertEquals(AttachableEntity.DEFAULT_ENCODING, createdAttachment.getEncoding());
		Assert.assertEquals(Long.valueOf(content.getBytes(AttachableEntity.DEFAULT_CHARSET).length), createdAttachment.getFilesize());
		Assert.assertEquals(1, attachmentManager.getAttachmentVersions(createdAttachment.getId()).size());
		//
		// get attachment data
		Assert.assertEquals(content, IOUtils.toString(attachmentManager.getAttachmentData(createdAttachment.getId())));
		//
		// remove owner
		attachmentManager.deleteAttachments(owner);
		//
		Assert.assertEquals(0, attachmentManager.getAttachments(owner, null).getTotalElements());
	}
	
	@Test
	public void testAttachmentVersions() throws IOException {
		Identifiable owner = new TestOwnerEntity(UUID.randomUUID());
		String contentOne = "test data 1";
		IdmAttachmentDto attachmentOne = prepareAttachment(contentOne);
		attachmentOne = attachmentManager.saveAttachment(owner, attachmentOne);
		String contentTwo = "test data 2";
		IdmAttachmentDto attachmentTwo = prepareAttachment(contentTwo);
		attachmentTwo = attachmentManager.saveAttachmentVersion(owner, attachmentTwo, attachmentOne);
		String contentThree = "test data 3";
		IdmAttachmentDto attachmentThree = prepareAttachment(contentThree);
		attachmentThree = attachmentManager.saveAttachmentVersion(owner, attachmentThree, attachmentTwo);
		//
		Assert.assertNotNull(attachmentManager.get(attachmentOne));
		Assert.assertNotNull(attachmentManager.get(attachmentTwo));
		Assert.assertNotNull(attachmentManager.get(attachmentThree));
		//
		// last version only
		List<IdmAttachmentDto> attachments = attachmentManager.getAttachments(owner, null).getContent();
		Assert.assertEquals(1, attachments.size());
		Assert.assertEquals(contentThree, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(0).getId())));
		//
		attachments = attachmentManager.getAttachmentVersions(attachmentOne.getId());
		Assert.assertEquals(3, attachments.size());
		// three
		Assert.assertEquals(Integer.valueOf(3), attachments.get(0).getVersionNumber());
		Assert.assertEquals("3.0", attachments.get(0).getVersionLabel());
		Assert.assertNull(attachments.get(0).getNextVersion());
		Assert.assertEquals(attachmentOne.getId(), attachments.get(0).getParent());
		Assert.assertEquals(contentThree, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(0).getId())));
		// two
		Assert.assertEquals(Integer.valueOf(2), attachments.get(1).getVersionNumber());
		Assert.assertEquals("2.0", attachments.get(1).getVersionLabel());
		Assert.assertEquals(attachmentThree.getId(), attachments.get(1).getNextVersion());
		Assert.assertEquals(attachmentOne.getId(), attachments.get(1).getParent());
		Assert.assertEquals(contentTwo, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(1).getId())));
		// one
		Assert.assertEquals(Integer.valueOf(1), attachments.get(2).getVersionNumber());
		Assert.assertEquals("1.0", attachments.get(2).getVersionLabel());
		Assert.assertEquals(attachmentTwo.getId(), attachments.get(2).getNextVersion());
		Assert.assertNull(attachments.get(2).getParent());
		Assert.assertEquals(contentOne, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(2).getId())));
		//
		attachmentManager.deleteAttachment(attachmentThree);
		//
		Assert.assertNull(attachmentManager.get(attachmentOne));
		Assert.assertNull(attachmentManager.get(attachmentTwo));
		Assert.assertNull(attachmentManager.get(attachmentThree));
	}
	
	@Test
	public void testAttachmentVersionsWithSameName() throws IOException {
		Identifiable owner = new TestOwnerEntity(UUID.randomUUID());
		String contentOne = "test data 1";
		IdmAttachmentDto attachmentOne = prepareAttachment(contentOne);
		attachmentOne = attachmentManager.saveAttachment(owner, attachmentOne);
		String contentTwo = "test data 2";
		IdmAttachmentDto attachmentTwo = prepareAttachment(contentTwo);
		attachmentTwo = attachmentManager.saveAttachmentVersion(owner, attachmentTwo, (IdmAttachmentDto) null);
		String contentThree = "test data 3";
		IdmAttachmentDto attachmentThree = prepareAttachment(contentThree);
		attachmentThree = attachmentManager.saveAttachmentVersion(owner, attachmentThree, (IdmAttachmentDto) null);
		//
		Assert.assertNotNull(attachmentManager.get(attachmentOne));
		Assert.assertNotNull(attachmentManager.get(attachmentTwo));
		Assert.assertNotNull(attachmentManager.get(attachmentThree));
		//
		// last version only
		List<IdmAttachmentDto> attachments = attachmentManager.getAttachments(owner, null).getContent();
		Assert.assertEquals(1, attachments.size());
		Assert.assertEquals(contentThree, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(0).getId())));
		//
		attachments = attachmentManager.getAttachmentVersions(attachmentOne.getId());
		Assert.assertEquals(3, attachments.size());
		// three
		Assert.assertEquals(Integer.valueOf(3), attachments.get(0).getVersionNumber());
		Assert.assertEquals("3.0", attachments.get(0).getVersionLabel());
		Assert.assertNull(attachments.get(0).getNextVersion());
		Assert.assertEquals(attachmentOne.getId(), attachments.get(0).getParent());
		Assert.assertEquals(contentThree, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(0).getId())));
		// two
		Assert.assertEquals(Integer.valueOf(2), attachments.get(1).getVersionNumber());
		Assert.assertEquals("2.0", attachments.get(1).getVersionLabel());
		Assert.assertEquals(attachmentThree.getId(), attachments.get(1).getNextVersion());
		Assert.assertEquals(attachmentOne.getId(), attachments.get(1).getParent());
		Assert.assertEquals(contentTwo, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(1).getId())));
		// one
		Assert.assertEquals(Integer.valueOf(1), attachments.get(2).getVersionNumber());
		Assert.assertEquals("1.0", attachments.get(2).getVersionLabel());
		Assert.assertEquals(attachmentTwo.getId(), attachments.get(2).getNextVersion());
		Assert.assertNull(attachments.get(2).getParent());
		Assert.assertEquals(contentOne, IOUtils.toString(attachmentManager.getAttachmentData(attachments.get(2).getId())));
		//
		attachmentManager.deleteAttachment(attachmentThree);
		//
		Assert.assertNull(attachmentManager.get(attachmentOne));
		Assert.assertNull(attachmentManager.get(attachmentTwo));
		Assert.assertNull(attachmentManager.get(attachmentThree));
	}
	
	@Test
	public void testUpdateAttachment() {
		Identifiable owner = new TestOwnerEntity(UUID.randomUUID());
		String contentOne = "test data 1";
		IdmAttachmentDto attachment = prepareAttachment(contentOne);
		attachment = attachmentManager.saveAttachment(owner, attachment);
		//
		String updatedContent = "update";
		attachment.setInputData(IOUtils.toInputStream(updatedContent));
		attachment.setDescription(updatedContent);		
		attachment = attachmentManager.updateAttachment(attachment);
		//
		List<IdmAttachmentDto> attachments = attachmentManager.getAttachmentVersions(attachment.getId());
		//
		Assert.assertEquals(1, attachments.size());
		IdmAttachmentDto createdAttachment = attachments.get(0);
		Assert.assertEquals(attachment.getName(), createdAttachment.getName());
		Assert.assertEquals(attachment.getMimetype(), createdAttachment.getMimetype());
		Assert.assertEquals(owner.getClass().getCanonicalName(), createdAttachment.getOwnerType());
		Assert.assertEquals(owner.getId(), createdAttachment.getOwnerId());
		Assert.assertNotNull(createdAttachment.getContentId());
		Assert.assertNotNull(createdAttachment.getContentPath());
		Assert.assertNull(createdAttachment.getParent());
		Assert.assertNull(createdAttachment.getNextVersion());
		Assert.assertEquals(Integer.valueOf(1), createdAttachment.getVersionNumber());
		Assert.assertEquals("1.0", createdAttachment.getVersionLabel());
		Assert.assertEquals(AttachableEntity.DEFAULT_ENCODING, createdAttachment.getEncoding());
		Assert.assertEquals(Long.valueOf(updatedContent.getBytes(AttachableEntity.DEFAULT_CHARSET).length), createdAttachment.getFilesize());
		Assert.assertEquals(updatedContent, createdAttachment.getDescription());
		//
		attachmentManager.deleteAttachment(attachment);
		//
		Assert.assertEquals(0, attachmentManager.getAttachmentVersions(attachment.getId()).size());
	}
	
	@Test
	public void testCreateAndPurgeTempFiles() {
		long ttl = attachmentConfiguration.getTempTtl();
		try {
			// cleanup and disable
			attachmentConfiguration.setTempTtl(1);
			attachmentManager.purgeTemp();
			Assert.assertEquals(0, countTempFiles());
			attachmentConfiguration.setTempTtl(0);
			//
			// create temp files
			attachmentManager.createTempFile();
			attachmentManager.createTempFile();
			attachmentManager.createTempFile();
			//
			attachmentManager.purgeTemp();
			Assert.assertEquals(3, countTempFiles());
			//
			attachmentConfiguration.setTempTtl(100000000);
			attachmentManager.purgeTemp();
			Assert.assertEquals(3, countTempFiles());
			//
			attachmentConfiguration.setTempTtl(1);
			attachmentManager.purgeTemp();
			Assert.assertEquals(0, countTempFiles());
		} finally {
			attachmentConfiguration.setTempTtl(ttl);
		}
	}
	
	@Test
	public void testCreateAndPurgeTempAttachments() {
		long ttl = attachmentConfiguration.getTempTtl();
		try {
			UUID ownerId = UUID.randomUUID();
			IdmAttachmentFilter filter = new IdmAttachmentFilter();
			filter.setOwnerId(ownerId);
			filter.setOwnerType(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE);
			//
			createDto(ownerId);
			getHelper().waitForResult(null, 20, 1);
			Assert.assertEquals(1, attachmentManager.find(filter, null).getContent().size());
			//
			attachmentConfiguration.setTempTtl(10000);
			attachmentManager.purgeTemp();
			Assert.assertEquals(1, attachmentManager.find(filter, null).getContent().size());
			
			attachmentConfiguration.setTempTtl(19);
			attachmentManager.purgeTemp();
			Assert.assertTrue(attachmentManager.find(filter, null).getContent().isEmpty());
		} finally {
			attachmentConfiguration.setTempTtl(ttl);
		}
	}
	
	private IdmAttachmentDto prepareAttachment(String content) {
		IdmAttachmentDto attachment = new IdmAttachmentDto();
		attachment.setName("test.txt");
		attachment.setMimetype("text/plain");
		attachment.setInputData(IOUtils.toInputStream(content));
		//
		return attachment;
	}
	
	private int countTempFiles() {
		File temp = new File(attachmentConfiguration.getTempPath());
		//
		return temp.listFiles().length;
	}
	
	private IdmAttachmentDto createDto(UUID ownerId) {
		IdmAttachmentDto dto = prepareDto();
		dto.setOwnerId(ownerId);
		//
		return attachmentManager.save(dto);
	}
	
	public static IdmAttachmentDto prepareDto() {
		IdmAttachmentDto dto = new IdmAttachmentDto();
		dto.setOwnerType(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE);
		dto.setName("name-" + UUID.randomUUID());
		dto.setMimetype(AttachableEntity.DEFAULT_MIMETYPE);
		dto.setVersionNumber(1);
		dto.setVersionLabel("1.0");
		dto.setContentId(UUID.randomUUID());
		dto.setContentPath("mock");
		dto.setEncoding(AttachableEntity.DEFAULT_ENCODING);
		dto.setFilesize(1L);
		//
		return dto;
	}
	
	private class TestOwnerEntity implements AttachableEntity {
		
		private static final long serialVersionUID = 1L;
		private UUID id;
		
		public TestOwnerEntity(UUID id) {
			this.id = id;
		}
		
		@Override
		public void setId(Serializable id) {
			this.id = (UUID) id; 			
		}

		@Override
		public UUID getId() {
			return id;
		}
		
	}
}
