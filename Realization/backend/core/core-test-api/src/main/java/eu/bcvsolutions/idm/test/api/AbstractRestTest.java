package eu.bcvsolutions.idm.test.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.security.api.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.api.service.GrantedAuthoritiesFactory;

/**
 * Abstract rest test - using mock mvc
 * 
 * @author Radek Tomiška
 *
 */
@Ignore
public abstract class AbstractRestTest extends AbstractIntegrationTest {

	private MockMvc mockMvc;
	private static String CONTENT_TYPE = "application/hal+json";
	//
	@Autowired private WebApplicationContext webApplicationContext;
	@Autowired private GrantedAuthoritiesFactory grantedAuthoritiesFactory;

	@Before
	public void setup() throws Exception {
		this.mockMvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	/**
	 * Initialized mock mvc 
	 * 
	 * @return
	 */
	public MockMvc getMockMvc() {
		return mockMvc;
	}

	public String getJsonAsString(String endpoint, Authentication auth) throws UnsupportedEncodingException, Exception {
		return this.getJsonAsString(endpoint, null, null, null, null, null, auth);
	}

	public String getJsonAsString(String endpoint, String anotherParameters, Long size, Long page, String sort,
			String order, Authentication auth) throws UnsupportedEncodingException, Exception {

		return getMockMvc()
				.perform(get(BaseController.BASE_PATH + endpoint
						+ getPageableAndParameters(anotherParameters, size, page, sort, order))
								.with(authentication(auth)).contentType(CONTENT_TYPE))
				.andReturn().getResponse().getContentAsString();
	}

	public List<LinkedHashMap<String, Object>> getEmbeddedList(String nameEmbeddedList, String json) throws IOException {
		JSONObject tObject = new JSONObject(json);
		String embeddedString = tObject.get("_embedded").toString();
		tObject = new JSONObject(embeddedString);
		// get embedded list
		String listString = tObject.get(nameEmbeddedList).toString();

		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(listString, new TypeReference<List<LinkedHashMap<String, Object>>>() {});
	}
	
	/**
	 * Login as admin
	 * 
	 * @return
	 */
	protected Authentication getAdminAuthentication() {
		return getAuthentication(TestHelper.ADMIN_USERNAME);
	}
	
	/**
	 * Login identity
	 * 
	 * TODO: move to test helper
	 * 
	 * @return
	 */
	protected Authentication getAuthentication(String username) {
		return new IdmJwtAuthentication(
				getHelper().getService(IdmIdentityService.class).getByUsername(username), 
				null, 
				grantedAuthoritiesFactory.getGrantedAuthorities(username), 
				"test");
	}

	private String getPageableAndParameters(String anotherParameters, Long size, Long page, String sort, String order) {
		StringBuilder pageable = new StringBuilder();
		if (anotherParameters != null && !anotherParameters.isEmpty()) {
			pageable.append('?');
			pageable.append(anotherParameters);
		}
		if (size != null) {
			if (pageable.length() > 0) {
				pageable.append('&');
			} else {
				pageable.append('?');
			}
			pageable.append("size=");
			pageable.append(size);
		}
		if (page != null) {
			if (pageable.length() > 0) {
				pageable.append('&');
			} else {
				pageable.append('?');
			}
			pageable.append("page=");
			pageable.append(page);
		}
		if (sort != null) {
			if (pageable.length() > 0) {
				pageable.append('&');
			} else {
				pageable.append('?');
			}
			pageable.append("sort=");
			pageable.append(sort);
			if (order != null) {
				pageable.append(',');
				pageable.append(order);
			}
		}
		return pageable.toString();
	}
	
	/**
	 * Returns basic auth in base64 usable in rest header
	 * 
	 * @param user
	 * @param password
	 * @return
	 */
	protected String getBasicAuth(String user, String password) {
		return Base64.encodeBase64String((user + ":" + password).getBytes(StandardCharsets.UTF_8));
	}
}
