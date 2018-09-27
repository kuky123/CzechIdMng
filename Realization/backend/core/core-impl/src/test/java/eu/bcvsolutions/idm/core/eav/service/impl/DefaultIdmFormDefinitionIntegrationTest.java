package eu.bcvsolutions.idm.core.eav.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.repository.IdmFormAttributeRepository;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * EAV definition tests
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultIdmFormDefinitionIntegrationTest extends AbstractIntegrationTest {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmFormDefinitionIntegrationTest.class);

	@Autowired private ApplicationContext context;
	@Autowired private IdmFormAttributeRepository formAttributeRepository;
	@Autowired private IdmFormAttributeService formAttributeService;
	//
	private DefaultIdmFormDefinitionService formDefinitionService;	
	
	private Random r = new Random();
	
	@Before
	public void init() {
		formDefinitionService = context.getAutowireCapableBeanFactory().createBean(DefaultIdmFormDefinitionService.class);
	}
	
	/**
	 * Creates definition
	 * 
	 * @param name
	 * @param randomAttributes with random count of attributes
	 */
	private ResultHolder createDefinition(String code, boolean randomAttributes, boolean log) {
		ResultHolder result = new ResultHolder();
		
		IdmFormDefinitionDto formDefinition = new IdmFormDefinitionDto();
		formDefinition.setType("test_type");
		formDefinition.setCode(code);
		if (log) {
			LOG.info("Before definition save [{}]", code);
		}
		long startTime = System.currentTimeMillis();
		formDefinition = formDefinitionService.save(formDefinition);
		if (log) {
			result.createTime = System.currentTimeMillis() - startTime;
			LOG.info("--- {}ms:  After definition save [{}]", result.createTime, code);
		}
		int attributeCount = r.nextInt(40);
		if (log) {
			LOG.info("Before definition [{}] attributes save, attributes count [{}]", code, attributeCount);
		}
		startTime = System.currentTimeMillis();
		for(int i = 0; i < attributeCount; i++) {
			IdmFormAttributeDto attributeDefinition = new IdmFormAttributeDto();
			attributeDefinition.setFormDefinition(formDefinition.getId());
			attributeDefinition.setCode("name_" + i);
			attributeDefinition.setName(attributeDefinition.getCode());
			attributeDefinition.setPersistentType(PersistentType.TEXT);			
			attributeDefinition = formAttributeService.save(attributeDefinition);
		}
		if (log) {
			result.childrenCreateTime = (double) System.currentTimeMillis() - startTime;
			LOG.info("--- {}ms:  After definition [{}] attributes save, attributes count [{}]", result.childrenCreateTime, code, attributeCount);
			if(attributeCount > 0) {
				result.childrenCreateTime = result.childrenCreateTime / attributeCount;
			}
			startTime = System.currentTimeMillis();
			int realAttributeCount = formAttributeRepository.findByFormDefinition_IdOrderBySeq(formDefinition.getId()).size();
			assertEquals(attributeCount, realAttributeCount);
			result.childrenLoadTime = System.currentTimeMillis() - startTime;
			LOG.info("--- {}ms:  After definition [{}] attributes load, attributes count [{}]", result.childrenLoadTime, code, realAttributeCount);
		}
		result.formDefinition = formDefinition;
		return result;
	}
	
	@Test
	@Transactional
	public void testDeleteDefinitionWithAttributes() {
		IdmFormDefinitionDto formDefinition = createDefinition("one", true, true).formDefinition;
		
		formDefinitionService.delete(formDefinition);
		
		assertEquals(0, formAttributeRepository.findByFormDefinition_IdOrderBySeq(formDefinition.getId()).size());
	}
	
	@Test
	@Ignore
	public void generateFormDefinitionsOneByOne() {
		int start = 5000000;
		int end = 5000100;
		//
		ResultHolder averageResult = new ResultHolder();
		for (int i = start; i < end; i++) {
			ResultHolder result = createDefinition(i + "_def", true, true);
			averageResult.createTime += result.createTime;
			averageResult.childrenCreateTime += result.childrenCreateTime;
			averageResult.childrenLoadTime += result.childrenLoadTime;
		}
		LOG.info("----");
		LOG.info("---- Average definition save: {}ms", averageResult.createTime / (end - start));
		LOG.info("---- Average children save: {}ms", averageResult.childrenCreateTime / (end - start));
		LOG.info("---- Average children load: {}ms", averageResult.childrenLoadTime / (end - start));
	}
	
	@Test
	@Ignore
	public void generateFormDefinitions() throws InterruptedException {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(75);
		
		for (int i = 4555000; i < 5000000; i++) {
			Runnable creator = new FormDefinitionCreator(i);
            executor.execute(creator);
            int queueSize = executor.getQueue().size();
            if (queueSize > 5000) {
            	LOG.warn("Form definition generator has full queue [{}], pausing ...", queueSize);
            	Thread.sleep(5000);
            }
		}
		
		executor.shutdown();
        while (!executor.isTerminated()) {
        }
        LOG.info("Form definition generator - finished all threads");
	}
	
	@Test
	@Transactional
	public void testSwitchMainDefinition() {
		String type = getHelper().createName();
		IdmFormDefinitionDto formDefinitionOne = new IdmFormDefinitionDto();
		formDefinitionOne.setType(type);
		formDefinitionOne.setCode(getHelper().createName());
		formDefinitionOne.setMain(true);
		formDefinitionOne = formDefinitionService.save(formDefinitionOne);
		Assert.assertTrue(formDefinitionOne.isMain());
		formDefinitionOne = formDefinitionService.save(formDefinitionOne);
		Assert.assertTrue(formDefinitionOne.isMain());
		//
		IdmFormDefinitionDto formDefinitionTwo = new IdmFormDefinitionDto();
		formDefinitionTwo.setType(type);
		formDefinitionTwo.setCode(getHelper().createName());
		formDefinitionTwo.setMain(true);
		formDefinitionTwo = formDefinitionService.save(formDefinitionTwo);
		formDefinitionOne = formDefinitionService.get(formDefinitionOne);
		//
		Assert.assertFalse(formDefinitionOne.isMain());
		Assert.assertTrue(formDefinitionTwo.isMain());
		//
		// update
		formDefinitionTwo.setMain(true);
		formDefinitionTwo = formDefinitionService.save(formDefinitionTwo);
		formDefinitionOne = formDefinitionService.get(formDefinitionOne);
		//
		Assert.assertFalse(formDefinitionOne.isMain());
		Assert.assertTrue(formDefinitionTwo.isMain());
	}
	
	@Test
	@Transactional
	public void testUpdateDefinition() {
		List<IdmFormAttributeDto> attributes = new ArrayList<>();
		attributes.add(new IdmFormAttributeDto("code", "Code", PersistentType.TEXT));
		attributes.add(new IdmFormAttributeDto("name", "Name", PersistentType.TEXT));		
		IdmFormDefinitionDto formDefinition = formDefinitionService.updateDefinition(IdmIdentityDto.class, getHelper().createName(), attributes);
		// after create
		Assert.assertEquals(2, formDefinition.getFormAttributes().size());
		IdmFormAttributeDto code = formDefinition.getFormAttributes().get(0);
		IdmFormAttributeDto name = formDefinition.getFormAttributes().get(1);
		Assert.assertEquals("code", code.getCode());
		Assert.assertEquals("Code", code.getName());
		Assert.assertNull(code.getDescription());
		Assert.assertFalse(code.isReadonly());
		Assert.assertFalse(code.isRequired());
		Assert.assertFalse(code.isMultiple());
		Assert.assertFalse(code.isUnmodifiable());
		Assert.assertNull(code.getFaceType());
		Assert.assertNull(code.getPlaceholder());
		Assert.assertEquals(0, code.getSeq().shortValue());
		Assert.assertEquals("name", name.getCode());
		Assert.assertEquals("Name", name.getName());
		Assert.assertEquals(1, name.getSeq().shortValue());
		// update
		code.setSeq((short) 2);
		code.setName("Code update");
		code.setDefaultValue("default");
		code.setReadonly(true);
		code.setRequired(true);
		code.setMultiple(true);
		code.setUnmodifiable(true);
		code.setPlaceholder("placeholder");
		code.setDescription("description");
		code.setFaceType("face");
		IdmFormAttributeDto date = new IdmFormAttributeDto("date", "Date", PersistentType.DATE);
		date.setSeq((short) 0);
		formDefinition = formDefinitionService.updateDefinition(IdmIdentityDto.class, formDefinition.getCode(), Lists.newArrayList(code, name, date));
		// after update
		Assert.assertEquals(3, formDefinition.getFormAttributes().size());
		code = formDefinition.getMappedAttributeByCode("code");
		name = formDefinition.getMappedAttributeByCode("name");
		date = formDefinition.getMappedAttributeByCode("date");
		//
		Assert.assertEquals(0, date.getSeq().shortValue());
		Assert.assertEquals(1, name.getSeq().shortValue());
		Assert.assertEquals(2, code.getSeq().shortValue());
		//
		Assert.assertEquals("code", code.getCode());
		Assert.assertEquals("Code update", code.getName());
		Assert.assertEquals("description", code.getDescription());
		Assert.assertTrue(code.isReadonly());
		Assert.assertTrue(code.isRequired());
		Assert.assertTrue(code.isMultiple());
		Assert.assertTrue(code.isUnmodifiable());
		Assert.assertEquals("face", code.getFaceType());
		Assert.assertEquals("placeholder", code.getPlaceholder());
	}
	
	@Test(expected = ResultCodeException.class)
	@Transactional
	public void testUpdateDefinitionPersistentType() {
		IdmFormAttributeDto attribute = new IdmFormAttributeDto("code", "Code", PersistentType.TEXT);	
		IdmFormDefinitionDto formDefinition = formDefinitionService.updateDefinition(IdmIdentityDto.class, getHelper().createName(), Lists.newArrayList(attribute));
		Assert.assertEquals(1, formDefinition.getFormAttributes().size());
		//
		// update
		attribute.setPersistentType(PersistentType.DATE);
		formDefinitionService.updateDefinition(IdmIdentityDto.class, formDefinition.getCode(), Lists.newArrayList(attribute));
	}
	
	@Test(expected = ResultCodeException.class)
	@Transactional
	public void testUpdateDefinitionConfidential() {
		IdmFormAttributeDto attribute = new IdmFormAttributeDto("code", "Code", PersistentType.TEXT);	
		IdmFormDefinitionDto formDefinition = formDefinitionService.updateDefinition(IdmIdentityDto.class, getHelper().createName(), Lists.newArrayList(attribute));
		Assert.assertEquals(1, formDefinition.getFormAttributes().size());
		//
		// update
		attribute.setConfidential(true);
		formDefinitionService.updateDefinition(IdmIdentityDto.class, formDefinition.getCode(), Lists.newArrayList(attribute));
	}
	
	@Test
	@Transactional
	public void testUpdateDefinitionRemoveAttribute() {
		// nothing happens, it's not supported operation (filled data are lost)
		IdmFormAttributeDto attributeOne = new IdmFormAttributeDto("code", "Code", PersistentType.TEXT);
		IdmFormAttributeDto attributeTwo = new IdmFormAttributeDto("two", "Code", PersistentType.TEXT);	
		IdmFormDefinitionDto formDefinition = formDefinitionService.updateDefinition(
				IdmIdentityDto.class, 
				getHelper().createName(), 
				Lists.newArrayList(attributeOne, attributeTwo));
		Assert.assertEquals(2, formDefinition.getFormAttributes().size());
		//
		formDefinition = formDefinitionService.updateDefinition(IdmIdentityDto.class, formDefinition.getCode(), Lists.newArrayList(attributeTwo));
		Assert.assertEquals(2, formDefinition.getFormAttributes().size());
	}
	
	private class FormDefinitionCreator implements Runnable {

		private final int id;
		
		public FormDefinitionCreator(int id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			createDefinition(id + "_def", true, ((id % 100) == 99));
		}
		
	}
	
	private class ResultHolder {
		public IdmFormDefinitionDto formDefinition;
		public double createTime;
		public double childrenCreateTime;
		public double childrenLoadTime;
	}
}
