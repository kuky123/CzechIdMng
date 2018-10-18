package eu.bcvsolutions.idm.core.ecm.rest.impl;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.filter.IdmAttachmentFilter;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.ecm.service.DefaultAttachmentManagerIntegrationTest;
import eu.bcvsolutions.idm.test.api.TestHelper;

/**
 * Controller tests
 * - CRUD
 * - Upload
 * - filters (TODO: add all)
 * 
 * @author Radek Tomiška
 *
 */
public class IdmAttachmentControllerRestTest extends AbstractReadWriteDtoControllerRestTest<IdmAttachmentDto> {

	@Autowired private IdmAttachmentController controller;
	@Autowired private AttachmentManager attachmentManager;
	
	@Override
	protected AbstractReadWriteDtoController<IdmAttachmentDto, ?> getController() {
		return controller;
	}

	@Override
	protected IdmAttachmentDto prepareDto() {
		IdmAttachmentDto dto = DefaultAttachmentManagerIntegrationTest.prepareDto();
		//
		return dto;
	}
	
	@Test
	public void testUpload() throws Exception {
		String fileName = "file.txt";
		String content = "some text content";
		String response = getMockMvc().perform(MockMvcRequestBuilders.fileUpload(getBaseUrl() + "/upload")
				.file("data", IOUtils.toByteArray(IOUtils.toInputStream(content)))
        		.param("fileName", fileName)
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isCreated())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andReturn()
                .getResponse()
                .getContentAsString();
		IdmAttachmentDto createdDto = (IdmAttachmentDto) getMapper().readValue(response, IdmAttachmentDto.class);
		//
		Assert.assertNotNull(createdDto);
		Assert.assertNotNull(createdDto.getId());
		Assert.assertEquals(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE, createdDto.getOwnerType());
		Assert.assertEquals(content.length(), createdDto.getFilesize().intValue());
		Assert.assertNull(createdDto.getOwnerId());
		Assert.assertEquals(fileName, createdDto.getName());
		Assert.assertEquals(content, IOUtils.toString(attachmentManager.getAttachmentData(createdDto.getId())));
		//
		attachmentManager.delete(createdDto);
	}
	
	@Test
	public void testFindByCreated() {
		UUID ownerId = UUID.randomUUID();
		IdmAttachmentDto attachment = prepareDto();
		attachment.setOwnerId(ownerId);
		IdmAttachmentDto attachmentOne = createDto(attachment);
		getHelper().waitForResult(null, 20, 1);
		DateTime afterOne = DateTime.now();
		getHelper().waitForResult(null, 20, 1);
		attachment = prepareDto();
		attachment.setOwnerId(ownerId);
		IdmAttachmentDto attachmentTwo = createDto(attachment);
		getHelper().waitForResult(null, 20, 1);
		DateTime afterTwo = DateTime.now();
		//
		IdmAttachmentFilter filter = new IdmAttachmentFilter();
		filter.setOwnerId(ownerId);
		List<IdmAttachmentDto> results = find(filter);
		//
		Assert.assertEquals(2, results.size());
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentOne.getId())));
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentTwo.getId())));
		//
		filter.setCreatedBefore(afterOne);
		results = find(filter);
		Assert.assertEquals(1, results.size());
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentOne.getId())));
		//
		filter.setCreatedBefore(afterTwo);
		results = find(filter);
		Assert.assertEquals(2, results.size());
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentOne.getId())));
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentTwo.getId())));
		//
		filter.setCreatedBefore(null);
		filter.setCreatedAfter(afterOne);
		results = find(filter);
		Assert.assertEquals(1, results.size());
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentTwo.getId())));
		//
		filter.setCreatedAfter(afterTwo);
		results = find(filter);
		Assert.assertTrue(results.isEmpty());
		//
		filter.setCreatedBefore(afterTwo);
		filter.setCreatedAfter(afterOne);
		results = find(filter);
		Assert.assertEquals(1, results.size());
		Assert.assertTrue(results.stream().anyMatch(a -> a.getId().equals(attachmentTwo.getId())));
	}
}
