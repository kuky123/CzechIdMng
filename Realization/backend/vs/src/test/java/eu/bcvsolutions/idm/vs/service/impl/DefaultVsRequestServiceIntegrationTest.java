package eu.bcvsolutions.idm.vs.service.impl;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.InitDemoData;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemEntityFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.domain.IdentityState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.dto.LoginDto;
import eu.bcvsolutions.idm.core.security.api.service.LoginService;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.filter.api.IcResultsHandler;
import eu.bcvsolutions.idm.ic.impl.IcObjectClassImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;
import eu.bcvsolutions.idm.vs.TestHelper;
import eu.bcvsolutions.idm.vs.connector.basic.BasicVirtualConfiguration;
import eu.bcvsolutions.idm.vs.domain.VirtualSystemGroupPermission;
import eu.bcvsolutions.idm.vs.domain.VsOperationType;
import eu.bcvsolutions.idm.vs.domain.VsRequestState;
import eu.bcvsolutions.idm.vs.domain.VsValueChangeType;
import eu.bcvsolutions.idm.vs.dto.VsAccountDto;
import eu.bcvsolutions.idm.vs.dto.VsAttributeDto;
import eu.bcvsolutions.idm.vs.dto.VsConnectorObjectDto;
import eu.bcvsolutions.idm.vs.dto.VsRequestDto;
import eu.bcvsolutions.idm.vs.dto.VsSystemDto;
import eu.bcvsolutions.idm.vs.dto.filter.VsRequestFilter;
import eu.bcvsolutions.idm.vs.entity.VsAccount;
import eu.bcvsolutions.idm.vs.entity.VsRequest;
import eu.bcvsolutions.idm.vs.evaluator.VsRequestByImplementerEvaluator;
import eu.bcvsolutions.idm.vs.service.api.VsAccountService;
import eu.bcvsolutions.idm.vs.service.api.VsRequestService;

/**
 * Virtual system request test
 * + request filters
 * 
 * @author Svanda
 * @author Patrik Stloukal
 */
@Component
public class DefaultVsRequestServiceIntegrationTest extends AbstractIntegrationTest {

	private static final String USER_ONE_NAME = "vsUserOne";
	private static final String USER_IMPLEMENTER_NAME = "vsUserImplementer";
	private static final String ROLE_ONE_NAME = "vsRoleOne";
	private static final String USER_ONE_CHANGED_NAME = "vsUserOneChanged";

	@Autowired
	private TestHelper helper;
	@Autowired
	private VsRequestService requestService;
	@Autowired
	private VsAccountService accountService;
	@Autowired
	private FormService formService;
	@Autowired
	private LoginService loginService;
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private SysSystemService systemService;
	@Autowired
	private SysSchemaAttributeService schemaAttributeService;
	@Autowired
	private SysSystemAttributeMappingService systemAttributeMappingService;
	@Autowired
	private SysSystemMappingService systemMappingService;
	@Autowired
	private IdmFormAttributeService formAttributeService;
	@Autowired
	private SysSystemEntityService systemEntityService;
	@Autowired
	private IcConnectorFacade connectorFacade;

	@Before
	public void init() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		this.deleteAll(USER_ONE_NAME, USER_ONE_CHANGED_NAME, USER_IMPLEMENTER_NAME, ROLE_ONE_NAME);
		super.logout();
	}

	@Test
	public void createAndRealizeRequestTest() {

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);
		// We try realize the request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		request = requestService.realize(request);
		Assert.assertEquals(VsRequestState.REALIZED, request.getState());
		account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNotNull("Account cannot be null, because request was realized!", account);
	}

	@Test
	public void disableRequestTest() {

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		IdmIdentityDto identity = helper.createIdentity(USER_ONE_NAME);
		this.assignRoleSystem(system, identity, ROLE_ONE_NAME);
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);
		// We try realize the request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		request = requestService.realize(request);
		Assert.assertEquals(VsRequestState.REALIZED, request.getState());
		account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNotNull("Account cannot be null, because request was realized!", account);
		Assert.assertEquals(Boolean.TRUE, account.isEnable());

		super.logout();
		loginAsAdmin();
		// Disable the identity
		identity.setState(IdentityState.DISABLED_MANUALLY);
		identityService.save(identity);

		// Find created requests
		requests = requestService.find(requestFilter, null).getContent().stream()
				.filter(r -> VsRequestState.IN_PROGRESS == r.getState()).collect(Collectors.toList());
		Assert.assertEquals(1, requests.size());
		request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.UPDATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		// We try realize the request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		request = requestService.realize(request);
		Assert.assertEquals(VsRequestState.REALIZED, request.getState());
		account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNotNull("Account cannot be null, because request was realized!", account);
		Assert.assertEquals(Boolean.FALSE, account.isEnable());
	}

	@Test
	public void systemAccountFilterTest() {

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);
		// We try realize the request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		request = requestService.realize(request);
		Assert.assertEquals(VsRequestState.REALIZED, request.getState());
		account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNotNull("Account cannot be null, because request was realized!", account);

		IcConnectorConfiguration configuration = systemService.getConnectorConfiguration(system);
		IcObjectClass objectClass = new IcObjectClassImpl("__ACCOUNT__");
		List<String> uids = new ArrayList<>();
		connectorFacade.search(system.getConnectorInstance(), configuration, objectClass, null, new IcResultsHandler() {

			@Override
			public boolean handle(IcConnectorObject connectorObject) {
				uids.add(connectorObject.getUidValue());
				return true;
			}
		});
		Assert.assertEquals(1, uids.size());
		Assert.assertEquals(USER_ONE_NAME, uids.get(0));
	}

	@Test
	public void createAndCancelRequestTest() {
		String reason = "cancel \"request\" reason!";

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);

		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);
		// We try cancel the request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		request = requestService.cancel(request, reason);
		Assert.assertEquals(VsRequestState.CANCELED, request.getState());
		Assert.assertEquals(reason, request.getReason());
		account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was canceled!", account);
	}

	@Test(expected = ForbiddenEntityException.class)
	public void realizeRequestWithouRightTest() {
		String reason = "cancel \"request\" reason!";

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);

		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);
		// We try cancel the request
		super.logout();
		loginService.login(new LoginDto(USER_ONE_NAME, new GuardedString("password")));
		request = requestService.cancel(request, reason);
	}

	@Test
	public void createMoreRequestsTest() {
		String changed = "changed";

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);

		IdmIdentityDto userOne = identityService.getByUsername(USER_ONE_NAME);
		userOne.setFirstName(changed);
		userOne.setLastName(changed);
		identityService.save(userOne);
		// Duplicated save ... not invoke provisioning
		identityService.save(userOne);

		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(2, requests.size());
		VsRequestDto changeRequest = requests.stream().filter(
				req -> VsRequestState.IN_PROGRESS == req.getState() && VsOperationType.UPDATE == req.getOperationType())
				.findFirst().orElse(null);
		Assert.assertNotNull("Request with change not found!", changeRequest);
	}

	@Test
	public void realizeUpdateAndDeleteRequestsTest() {
		String changed = "changed";

		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);
		this.assignRoleSystem(system, helper.createIdentity(USER_ONE_NAME), ROLE_ONE_NAME);
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto request = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, request.getUid());
		Assert.assertEquals(VsOperationType.CREATE, request.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, request.getState());

		VsAccountDto account = accountService.findByUidSystem(USER_ONE_NAME, system.getId());
		Assert.assertNull("Account must be null, because request was not realized yet!", account);

		IdmIdentityDto userOne = identityService.getByUsername(USER_ONE_NAME);
		userOne.setFirstName(changed);
		userOne.setLastName(changed);
		identityService.save(userOne);
		// Delete identity
		identityService.delete(userOne);

		// Test read rights (none requests can be returned for UserOne)
		IdmIdentityDto userTwo = helper.createIdentity("vsUserTwo");
		super.logout();
		loginService.login(new LoginDto(userTwo.getUsername(), new GuardedString("password")));
		requests = requestService.find(requestFilter, null, IdmBasePermission.READ).getContent();
		Assert.assertEquals("We found request without correct rights!", 0, requests.size());

		// Test read rights (3 requests must be returned for UserImplementer)
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		requests = requestService.find(requestFilter, null, IdmBasePermission.READ).getContent();
		Assert.assertEquals(3, requests.size());
		VsRequestDto changeRequest = requests.stream().filter(
				req -> VsRequestState.IN_PROGRESS == req.getState() && VsOperationType.UPDATE == req.getOperationType())
				.findFirst().orElse(null);
		Assert.assertNotNull("Request with change not found!", changeRequest);
		VsRequestDto deleteRequest = requests.stream().filter(
				req -> VsRequestState.IN_PROGRESS == req.getState() && VsOperationType.DELETE == req.getOperationType())
				.findFirst().orElse(null);
		Assert.assertNotNull("Request with delete not found!", deleteRequest);
		VsRequestDto createRequest = requests.stream().filter(
				req -> VsRequestState.IN_PROGRESS == req.getState() && VsOperationType.CREATE == req.getOperationType())
				.findFirst().orElse(null);
		Assert.assertNotNull("Request with create not found!", createRequest);

		// Realize create request
		request = requestService.realize(createRequest);
		// Realize update request
		request = requestService.realize(changeRequest);
		// Realize delete request
		request = requestService.realize(deleteRequest);

		// Find only archived
		requestFilter.setOnlyArchived(Boolean.TRUE);
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(3, requests.size());
		boolean foundNotRealized = requests.stream().filter(req -> VsRequestState.REALIZED != req.getState())
				.findFirst().isPresent();
		Assert.assertTrue("Found not realized requests!", !foundNotRealized);

	}

	@Test
	public void checkMultivalueInWishObjectTest() {
		String ldapGroupsName = "ldapGroups";
		String changed = "changed";
		List<String> attributes = new ArrayList<>(Lists.newArrayList(BasicVirtualConfiguration.DEFAULT_ATTRIBUTES));
		attributes.add(ldapGroupsName);

		// Create virtual system with extra attribute (ldapGroups)
		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, attributes);

		// Search attribute definition for ldapGroups and set him to multivalue
		String virtualSystemKey = MessageFormat.format("{0}:systemId={1}", system.getConnectorKey().getFullName(),
				system.getId().toString());
		String type = VsAccount.class.getName();
		IdmFormDefinitionDto definition = this.formService.getDefinition(type, virtualSystemKey);
		IdmFormAttributeDto ldapGroupsFormAttr = formAttributeService.findAttribute(VsAccount.class.getName(),
				definition.getCode(), ldapGroupsName);
		Assert.assertNotNull("Ldap attribute muste exist!", ldapGroupsFormAttr);
		ldapGroupsFormAttr.setMultiple(true);
		formService.saveAttribute(ldapGroupsFormAttr);

		// Generate schema for system (we need propagate multivalue setting)
		SysSchemaObjectClassDto schema = systemService.generateSchema(system).get(0);

		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());
		List<SysSchemaAttributeDto> schemaAttributes = schemaAttributeService.find(schemaAttributeFilter, null)
				.getContent();
		SysSystemMappingFilter systemMappingFilter = new SysSystemMappingFilter();
		systemMappingFilter.setSystemId(system.getId());
		systemMappingFilter.setObjectClassId(schema.getId());
		SysSystemMappingDto mapping = systemMappingService.find(systemMappingFilter, null).getContent().get(0);
		for (SysSchemaAttributeDto schemaAttr : schemaAttributes) {
			if (ldapGroupsName.equals(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setUid(false);
				attributeMapping.setEntityAttribute(false);
				attributeMapping.setExtendedAttribute(true);
				attributeMapping.setIdmPropertyName(ldapGroupsName);
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(mapping.getId());
				systemAttributeMappingService.save(attributeMapping);
			}
		}
		IdmIdentityDto userOne = helper.createIdentity(USER_ONE_NAME);
		List<Serializable> initList = ImmutableList.of("TEST1", "TEST2", "TEST3");
		formService.saveValues(userOne, ldapGroupsName, initList);

		this.assignRoleSystem(system, userOne, ROLE_ONE_NAME);

		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto createRequest = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, createRequest.getUid());
		Assert.assertEquals(VsOperationType.CREATE, createRequest.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, createRequest.getState());

		VsConnectorObjectDto wish = requestService.getWishConnectorObject(createRequest);
		boolean findAttributeWithouChange = wish.getAttributes().stream().filter(attribute -> !attribute.isChanged())
				.findFirst().isPresent();
		Assert.assertTrue(!findAttributeWithouChange);

		// Check on exist ldapGroups attribute with three values
		VsAttributeDto ldapGroupAttribute = wish.getAttributes().stream()
				.filter(attribute -> ldapGroupsName.equals(attribute.getName())).findFirst().get();
		Assert.assertTrue(ldapGroupAttribute.isMultivalue());
		Assert.assertEquals(3, ldapGroupAttribute.getValues().size());

		// Change multivalue attribute
		List<Serializable> changeList = ImmutableList.of("TEST1", changed, "TEST3");
		formService.saveValues(userOne, ldapGroupsName, changeList);
		// Invoke provisioning
		identityService.save(userOne);

		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(2, requests.size());
		VsRequestDto changeRequest = requests.stream().filter(req -> VsOperationType.UPDATE == req.getOperationType())
				.findFirst().get();
		wish = requestService.getWishConnectorObject(changeRequest);
		ldapGroupAttribute = wish.getAttributes().stream()
				.filter(attribute -> ldapGroupsName.equals(attribute.getName())).findFirst().get();
		Assert.assertTrue(ldapGroupAttribute.isMultivalue());
		// Wish must contains three values (all add) ... because previous create
		// request is not realize yet. Wish show changes versus reals state in
		// VsAccount.
		Assert.assertEquals(3, ldapGroupAttribute.getValues().size());

		// We realize the create request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		requestService.realize(createRequest);
		// Refresh wish
		wish = requestService.getWishConnectorObject(changeRequest);
		ldapGroupAttribute = wish.getAttributes().stream()
				.filter(attribute -> ldapGroupsName.equals(attribute.getName())).findFirst().get();
		Assert.assertTrue(ldapGroupAttribute.isMultivalue());
		// Wish must contains four values ... two without change, one delete and
		// one add value
		Assert.assertEquals(4, ldapGroupAttribute.getValues().size());

		// Find unchanged value
		boolean findCorrectTest1Value = ldapGroupAttribute
				.getValues().stream().filter(value -> value.getValue().equals(initList.get(0))
						&& value.getOldValue().equals(initList.get(0)) && value.getChange() == null)
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectTest1Value);

		// Find deleted value
		boolean findCorrectDeletedTest2Value = ldapGroupAttribute.getValues().stream()
				.filter(value -> value.getValue().equals(initList.get(1)) && value.getOldValue().equals(initList.get(1))
						&& VsValueChangeType.REMOVED == value.getChange())
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectDeletedTest2Value);

		// Find added value
		boolean findCorrectCreatedChangedValue = ldapGroupAttribute.getValues().stream()
				.filter(value -> value.getValue().equals(changed) && value.getOldValue() == null
						&& VsValueChangeType.ADDED == value.getChange())
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectCreatedChangedValue);
	}

	@Test
	public void checkSinglevalueInWishObjectTest() {
		String changed = "changed";
		String firstName = "firstName";
		String lastName = "lastName";
		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);

		IdmIdentityDto userOne = helper.createIdentity(USER_ONE_NAME);
		userOne.setFirstName(firstName);
		userOne.setLastName(lastName);
		identityService.save(userOne);

		this.assignRoleSystem(system, userOne, ROLE_ONE_NAME);

		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		requestFilter.setState(VsRequestState.IN_PROGRESS);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto createRequest = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, createRequest.getUid());
		Assert.assertEquals(VsOperationType.CREATE, createRequest.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, createRequest.getState());

		VsConnectorObjectDto wish = requestService.getWishConnectorObject(createRequest);
		boolean findAttributeWithouChange = wish.getAttributes().stream().filter(attribute -> !attribute.isChanged())
				.findFirst().isPresent();
		Assert.assertTrue(!findAttributeWithouChange);

		// Change singlevalue attributes
		userOne.setFirstName(changed);
		userOne.setLastName(changed);
		// Invoke provisioning
		identityService.save(userOne);

		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(2, requests.size());

		// We realize the create request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		requestService.realize(createRequest);
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		// get wish
		wish = requestService.getWishConnectorObject(requests.get(0));
		Assert.assertEquals(2, wish.getAttributes().stream().filter(attr -> attr.isChanged()).count());

		// Find change for firstName value
		boolean findCorrectChangedFirstName = wish.getAttributes().stream()
				.filter(attr -> attr.getValue() != null && attr.getValue().getValue().equals(changed)
						&& attr.getValue().getOldValue().equals(firstName)
						&& VsValueChangeType.UPDATED == attr.getValue().getChange())
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectChangedFirstName);

		// Find change for lastName value
		boolean findCorrectChangedLastName = wish.getAttributes().stream()
				.filter(attr -> attr.getValue() != null && attr.getValue().getValue().equals(changed)
						&& attr.getValue().getOldValue().equals(lastName)
						&& VsValueChangeType.UPDATED == attr.getValue().getChange())
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectChangedLastName);

	}

	@Test
	public void changeUidTest() {
		SysSystemDto system = this.createVirtualSystem(USER_IMPLEMENTER_NAME, null);

		IdmIdentityDto userOne = helper.createIdentity(USER_ONE_NAME);
		identityService.save(userOne);

		this.assignRoleSystem(system, userOne, ROLE_ONE_NAME);

		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(system.getId());
		requestFilter.setUid(USER_ONE_NAME);
		requestFilter.setState(VsRequestState.IN_PROGRESS);
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		VsRequestDto createRequest = requests.get(0);
		Assert.assertEquals(USER_ONE_NAME, createRequest.getUid());
		Assert.assertEquals(VsOperationType.CREATE, createRequest.getOperationType());
		Assert.assertEquals(VsRequestState.IN_PROGRESS, createRequest.getState());

		VsConnectorObjectDto wish = requestService.getWishConnectorObject(createRequest);
		boolean findAttributeWithouChange = wish.getAttributes().stream().filter(attribute -> !attribute.isChanged())
				.findFirst().isPresent();
		Assert.assertTrue(!findAttributeWithouChange);

		// Change username attributes
		userOne.setUsername(USER_ONE_CHANGED_NAME);
		// Invoke provisioning
		identityService.save(userOne);

		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(2, requests.size());

		// We realize the create request
		super.logout();
		loginService.login(new LoginDto(USER_IMPLEMENTER_NAME, new GuardedString("password")));
		requestService.realize(createRequest);
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		// get wish
		wish = requestService.getWishConnectorObject(requests.get(0));
		Assert.assertEquals(1, wish.getAttributes().stream().filter(attr -> attr.isChanged()).count());

		// Find change for firstName value
		boolean findCorrectChangedUserName = wish.getAttributes().stream()
				.filter(attr -> attr.getValue().getValue().equals(USER_ONE_CHANGED_NAME)
						&& attr.getValue().getOldValue().equals(USER_ONE_NAME)
						&& VsValueChangeType.UPDATED == attr.getValue().getChange())
				.findFirst().isPresent();
		Assert.assertTrue(findCorrectChangedUserName);

		SysSystemEntityFilter systemEntityFilter = new SysSystemEntityFilter();
		systemEntityFilter.setSystemId(system.getId());
		systemEntityFilter.setUid(USER_ONE_NAME);
		boolean oldUserNameExist = !systemEntityService.find(systemEntityFilter, null).getContent().isEmpty();
		Assert.assertTrue(oldUserNameExist);
		// Realize change username
		requestService.realize(requests.get(0));
		// We expects change UID in SystemEntity.UID
		oldUserNameExist = !systemEntityService.find(systemEntityFilter, null).getContent().isEmpty();
		Assert.assertTrue(!oldUserNameExist);
		systemEntityFilter.setUid(USER_ONE_CHANGED_NAME);
		boolean changedUserNameExist = !systemEntityService.find(systemEntityFilter, null).getContent().isEmpty();
		Assert.assertTrue(changedUserNameExist);
	}
	
	@Test
	public void dateTest() {
		SysSystemDto virtualSystem = helper.createVirtualSystem(helper.createName());
		IdmRoleDto roleOne = helper.createRole();
		IdmIdentityDto identity = helper.createIdentity((GuardedString) null);
		
		// Assign system to role
		helper.createRoleSystem(roleOne, virtualSystem);
		helper.assignRoles(helper.getPrimeContract(identity.getId()), false, roleOne);
		
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(virtualSystem.getId());
		requestFilter.setUid(identity.getUsername());
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		
		requestFilter.setCreatedAfter(new DateTime().minusSeconds(10));
		requestFilter.setCreatedBefore(new DateTime());
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		
		requestFilter.setCreatedAfter(new DateTime().plusMinutes(10));
		requestFilter.setCreatedBefore(new DateTime().plusMinutes(11));
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(0, requests.size());
		
		requestFilter.setCreatedAfter(new DateTime().minusMinutes(10));
		requestFilter.setCreatedBefore(new DateTime().minusMinutes(9));
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(0, requests.size());
	}

	@Test
	public void systemTest() {
		SysSystemDto virtualSystem = helper.createVirtualSystem(helper.createName());
		IdmRoleDto roleOne = helper.createRole(helper.createName());
		IdmIdentityDto identity = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity2 = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity3 = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity4 = helper.createIdentity((GuardedString) null);
		
		// Assign system to role
		helper.createRoleSystem(roleOne, virtualSystem);
		helper.assignRoles(helper.getPrimeContract(identity.getId()), false, roleOne);
		helper.assignRoles(helper.getPrimeContract(identity2.getId()), false, roleOne);
		
		// Find created requests
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(virtualSystem.getId());
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(2, requests.size());
		
		helper.assignRoles(helper.getPrimeContract(identity3.getId()), false, roleOne);
		helper.assignRoles(helper.getPrimeContract(identity4.getId()), false, roleOne);
		
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(4, requests.size());
		
		requestFilter.setUid(identity.getUsername());
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());// identity uid filter test

	}
	
	@Test
	public void filterTest() {
		SysSystemDto virtualSystem = helper.createVirtualSystem(helper.createName());
		IdmRoleDto roleOne = helper.createRole();
		IdmIdentityDto identity = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity2 = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity3 = helper.createIdentity((GuardedString) null);
		IdmIdentityDto identity4 = helper.createIdentity((GuardedString) null);
		
		// Assign system to role
		helper.createRoleSystem(roleOne, virtualSystem);
		helper.assignRoles(helper.getPrimeContract(identity.getId()), false, roleOne);
		helper.assignRoles(helper.getPrimeContract(identity2.getId()), false, roleOne);
		helper.assignRoles(helper.getPrimeContract(identity3.getId()), false, roleOne);
		helper.assignRoles(helper.getPrimeContract(identity4.getId()), false, roleOne);
		
		VsRequestFilter requestFilter = new VsRequestFilter();
		requestFilter.setSystemId(virtualSystem.getId());
		List<VsRequestDto> requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(4, requests.size());

		VsRequestDto request = requests.get(0);
		requestService.realize(request);
		Assert.assertEquals(VsRequestState.REALIZED, request.getState());
		
		requestFilter.setOnlyArchived(true);
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(1, requests.size());
		
		requestFilter.setOnlyArchived(null);
		requestFilter.setState(VsRequestState.IN_PROGRESS);
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(3, requests.size());
		
		requestFilter.setConnectorKey(request.getConnectorKey());
		requests = requestService.find(requestFilter, null).getContent();
		Assert.assertEquals(3, requests.size());
	}

	/**
	 * Method for create role, assign role to system and to user.
	 * 
	 * @param USER_ONE_NAME
	 * @param USER_IMPLEMENTER_NAME
	 * @param ROLE_ONE_NAME
	 * @return
	 */
	private SysSystemDto assignRoleSystem(SysSystemDto system, IdmIdentityDto userOne, String roleOneName) {
		IdmRoleDto roleOne = helper.createRole(roleOneName);

		// Create policy for vs evaluator and user role
		helper.createAuthorizationPolicy(roleService.getByCode(InitDemoData.DEFAULT_ROLE_NAME).getId(),
				VirtualSystemGroupPermission.VSREQUEST, VsRequest.class, VsRequestByImplementerEvaluator.class,
				IdmBasePermission.ADMIN);

		// Assign system to role
		helper.createRoleSystem(roleOne, system);
		helper.assignRoles(helper.getPrimeContract(userOne.getId()), false, roleOne);
		return system;
	}

	private SysSystemDto createVirtualSystem(String userImplementerName, List<String> attributes) {
		IdmIdentityDto userImplementer = helper.createIdentity(userImplementerName);
		VsSystemDto config = new VsSystemDto();
		config.setName("vsSystemOne" + new Date().getTime());
		config.setImplementers(ImmutableList.of(userImplementer.getId()));
		if (attributes != null) {
			config.setAttributes(attributes);
		}
		SysSystemDto system = helper.createVirtualSystem(config);
		Assert.assertNotNull(system);
		return system;
	}

	public void deleteAll(String userOneName, String userOneChangedName, String userImplementerName,
			String roleOneName) {
		if (identityService.getByUsername(userOneName) != null) {
			identityService.delete(identityService.getByUsername(userOneName));
		}
		if (identityService.getByUsername(userOneChangedName) != null) {
			identityService.delete(identityService.getByUsername(userOneChangedName));
		}
		if (identityService.getByUsername(userImplementerName) != null) {
			identityService.delete(identityService.getByUsername(userImplementerName));
		}
		if (roleService.getByCode(roleOneName) != null) {
			roleService.delete(roleService.getByCode(roleOneName));
		}
	}

}
