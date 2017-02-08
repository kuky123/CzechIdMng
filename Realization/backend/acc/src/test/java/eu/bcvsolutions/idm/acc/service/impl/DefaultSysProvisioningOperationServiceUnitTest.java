package eu.bcvsolutions.idm.acc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.domain.ProvisioningContext;
import eu.bcvsolutions.idm.acc.repository.SysProvisioningOperationRepository;
import eu.bcvsolutions.idm.acc.repository.SysProvisioningRequestRepository;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningArchiveService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBatchService;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.notification.service.api.NotificationManager;
import eu.bcvsolutions.idm.core.security.api.domain.ConfidentialString;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.impl.IcConnectorObjectImpl;
import eu.bcvsolutions.idm.test.api.AbstractUnitTest;

/**
 * Test GuardedString vs. ConfidentialString resolving 
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultSysProvisioningOperationServiceUnitTest extends AbstractUnitTest {
	
	@Mock
	SysProvisioningOperationRepository repository;
	@Mock
	private SysProvisioningRequestRepository provisioningRequestRepository;
	@Mock
	private SysProvisioningArchiveService provisioningArchiveService;
	@Mock
	private SysProvisioningBatchService batchService;
	@Mock
	private NotificationManager notificationManager;
	@Mock
	private ConfidentialStorage confidentialStorage;
	
	DefaultSysProvisioningOperationService service;

	@Before
	public void init() {		
		service = new DefaultSysProvisioningOperationService(
				repository,
				provisioningRequestRepository, 
				provisioningArchiveService, 
				batchService, 
				notificationManager, 
				confidentialStorage);
	}
	
	@Test
	public void testReplaceGuardedStringsInEmptyContext() {
		assertEquals(0, service.replaceGuardedStrings(null).size());
	}
	
	@Test
	public void testReplaceGuardedStringsInEmptyAccountObject() {
		ProvisioningContext context = new ProvisioningContext();
		Map<String, Object> accoutObjet = new HashMap<>();
		context.setAccountObject(accoutObjet);		
		Map<String, Serializable> confidentialValues = service.replaceGuardedStrings(context);		
		assertEquals(0, confidentialValues.size());
	}
	
	@Test
	public void testReplaceSingleGuardedStringsInAccountObject() {
		ProvisioningContext context = new ProvisioningContext();
		Map<String, Object> accoutObject = new HashMap<>();
		context.setAccountObject(accoutObject);	
		//
		// fill properties
		String normal = "normal";
		String normalValue = "one";
		accoutObject.put(normal, normalValue);
		String guarded = "guarded";
		GuardedString guardedValue = new GuardedString("one");
		accoutObject.put(guarded, guardedValue);
		//
		// run
		Map<String, Serializable> confidentiaValues =  service.replaceGuardedStrings(context);
		//
		// check
		assertEquals(1, confidentiaValues.size());
		assertEquals(guardedValue.asString(), confidentiaValues.get(service.createAccountObjectPropertyKey(guarded, 0)));
		assertEquals(normalValue, accoutObject.get(normal));
		assertNotEquals(guardedValue, accoutObject.get(guardedValue));
	}
	
	@Test
	public void testReplaceGuardedStringsInConnectorObject() {
		ProvisioningContext context = new ProvisioningContext();
		IcConnectorObjectImpl connectorObject = new IcConnectorObjectImpl();
		context.setConnectorObject(connectorObject);
		//
		// fill properties
		String normalValue = "one";
		IcAttributeImpl normal = new IcAttributeImpl("normal", normalValue);
		connectorObject.getAttributes().add(normal);
		
		GuardedString guardedValue = new GuardedString("one");
		IcAttributeImpl guarded = new IcAttributeImpl("guarded", guardedValue);
		connectorObject.getAttributes().add(guarded);
		//
		// run
		Map<String, Serializable> confidentiaValues =  service.replaceGuardedStrings(context);
		//
		// check
		assertEquals(1, confidentiaValues.size());
		assertEquals(guardedValue.asString(), confidentiaValues.get(service.createConnectorObjectPropertyKey(guarded, 0)));
		assertEquals(normalValue, connectorObject.getAttributes().get(0).getValue());
		assertNotEquals(guardedValue, connectorObject.getAttributes().get(1).getValue());
	}
	
	@Test
	public void testReplaceArrayGuardedStringsInAccountObject() {
		ProvisioningContext context = new ProvisioningContext();
		Map<String, Object> accoutObject = new HashMap<>();
		context.setAccountObject(accoutObject);	
		//
		// fill properties
		String guarded = "guarded";
		GuardedString guardedOne = new GuardedString("one");
		GuardedString guardedTwo = new GuardedString("two");
		accoutObject.put(guarded, new GuardedString[]{ guardedOne, guardedTwo });
		//
		// run
		Map<String, Serializable> confidentiaValues =  service.replaceGuardedStrings(context);
		//
		// check
		assertEquals(2, confidentiaValues.size());
		assertEquals(guardedOne.asString(), confidentiaValues.get(service.createAccountObjectPropertyKey(guarded, 0)));
		assertEquals(guardedTwo.asString(), confidentiaValues.get(service.createAccountObjectPropertyKey(guarded, 1)));
		assertEquals(2, ((Object[])accoutObject.get(guarded)).length);
		assertEquals(service.createAccountObjectPropertyKey(guarded, 0), ((ConfidentialString)((Object[])accoutObject.get(guarded))[0]).getKey());
		assertEquals(service.createAccountObjectPropertyKey(guarded, 1), ((ConfidentialString)((Object[])accoutObject.get(guarded))[1]).getKey());
	}
	
	@Test
	public void testReplaceCollectionGuardedStringsInAccountObject() {
		ProvisioningContext context = new ProvisioningContext();
		Map<String, Object> accoutObject = new HashMap<>();
		context.setAccountObject(accoutObject);	
		//
		// fill properties
		String guarded = "guarded";
		GuardedString guardedOne = new GuardedString("one");
		GuardedString guardedTwo = new GuardedString("two");
		accoutObject.put(guarded, Lists.newArrayList(guardedOne, guardedTwo));
		//
		// run
		Map<String, Serializable> confidentiaValues =  service.replaceGuardedStrings(context);
		//
		// check
		assertEquals(2, confidentiaValues.size());
		assertEquals(guardedOne.asString(), confidentiaValues.get(service.createAccountObjectPropertyKey(guarded, 0)));
		assertEquals(guardedTwo.asString(), confidentiaValues.get(service.createAccountObjectPropertyKey(guarded, 1)));
		assertEquals(2, ((List<?>)accoutObject.get(guarded)).size());
		assertEquals(service.createAccountObjectPropertyKey(guarded, 0), ((ConfidentialString)((List<?>)accoutObject.get(guarded)).get(0)).getKey());
		assertEquals(service.createAccountObjectPropertyKey(guarded, 1), ((ConfidentialString)((List<?>)accoutObject.get(guarded)).get(1)).getKey());
	}
}