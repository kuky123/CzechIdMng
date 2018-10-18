package eu.bcvsolutions.idm.core.notification;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import eu.bcvsolutions.idm.InitTestData;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.security.api.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.api.utils.IdmAuthorityUtils;
import eu.bcvsolutions.idm.test.api.AbstractRestTest;

/**
 * Test for sending notifications via rest
 *
 * @author Peter Sourek
 */
public class NotificationRestTest extends AbstractRestTest {

	private static final String TEST_SENDER_1 = "testSender1";
	private static final String TEST_RECIPIENT_1 = "testRecipient1";
	private static final String TEST_SUBJECT = "Test subject";
	private static final String TEST_MESSAGE = "Test message";
	private static final String TEST_TOPIC = "testTopic";

	@Autowired
	IdmIdentityService identityService;

	@Test
	public void testSendNotification() throws Exception {
		final IdmIdentityDto sender = createTestIdentity(TEST_SENDER_1);
		final IdmIdentityDto recipient = createTestIdentity(TEST_RECIPIENT_1);
		//
		final IdmNotificationDto notif = createTestNotification(NotificationLevel.INFO, TEST_SUBJECT, TEST_MESSAGE, TEST_TOPIC, sender, recipient);
		final String jsonContent = jsonify(notif);
		//
		MockHttpServletResponse response = getMockMvc().perform(MockMvcRequestBuilders.post(BaseDtoController.BASE_PATH + "/notifications")
			.with(authentication(getAuthentication()))
			.contentType(MediaTypes.HAL_JSON)
			.content(jsonContent))
			.andReturn()
			.getResponse();
		//
		assertEquals(201, response.getStatus());
	}

	IdmIdentityDto createTestIdentity(String name) {
		final IdmIdentityDto dto = new IdmIdentityDto();
		dto.setUsername(name);
		dto.setFirstName(name);
		dto.setLastName(name);
		dto.setEmail(name + "@email.com");
		//
		final IdmIdentityDto result = identityService.saveInternal(dto);
		return result;
	}

	IdmNotificationDto createTestNotification(NotificationLevel level, String subject, String message, String topic, IdmIdentityDto sender, IdmIdentityDto... recipients) {
		final IdmMessageDto msg = new IdmMessageDto();
		msg.setHtmlMessage(message);
		msg.setTextMessage(message);
		msg.setLevel(level);
		msg.setSubject(subject);
		//
		final List<IdmNotificationRecipientDto> rec = Arrays.stream(recipients).map(r -> {
			final IdmNotificationRecipientDto res = new IdmNotificationRecipientDto();
			res.setIdentityRecipient(r.getId());
			res.setRealRecipient(r.getUsername());
			return res;
		}).collect(Collectors.toList());
		//
		final IdmNotificationDto result = new IdmNotificationDto();
		result.setIdentitySender(sender.getId());
		result.setRecipients(rec);
		result.setTopic(topic);
		result.setMessage(msg);
		return result;
	}

	String jsonify(IdmNotificationDto dto) throws IOException {
		ObjectMapper m = new ObjectMapper();
		StringWriter sw = new StringWriter();
		ObjectWriter writer = m.writerFor(IdmNotificationDto.class);
		writer.writeValue(sw, dto);
		return sw.toString();
	}

	private Authentication getAuthentication() {

		return new IdmJwtAuthentication(
			identityService.getByUsername(InitTestData.TEST_ADMIN_USERNAME),
			null,
			Lists.newArrayList(IdmAuthorityUtils.getAdminAuthority()),
			"test");
	}

}
