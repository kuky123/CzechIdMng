package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.forest.index.service.api.ForestIndexService;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmTreeNodeFilter;
import eu.bcvsolutions.idm.core.exception.TreeNodeException;
import eu.bcvsolutions.idm.core.model.entity.IdmForestIndexEntity;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.model.service.api.IdmTreeNodeForestContentService;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Tree nodes 
 * - referential integrity
 * - node move operation
 * - reindex
 * 
 * @author Radek Tomiška
 */
public class DefaultIdmTreeNodeServiceIntegrationTest extends AbstractIntegrationTest {
	
	@Autowired private ApplicationContext context;
	@Autowired private IdmTreeNodeForestContentService treeNodeForestContentService;
	@Autowired private ForestIndexService<IdmForestIndexEntity, UUID> forestIndexService;
	//
	private DefaultIdmTreeNodeService service;
	
	@Before
	public void init() {
		service = context.getAutowireCapableBeanFactory().createBean(DefaultIdmTreeNodeService.class);
	}
	
	@Transactional
	@Test(expected = TreeNodeException.class)
	public void testReferentialIntegrityDeleteNodeWithContracts() {
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmTreeNodeDto treeNode = getHelper().createTreeNode();
		getHelper().createIdentityContact(identity, treeNode);
	    // tree node cannot be deleted, when some contract are defined on this node
	    service.delete(treeNode);
	}
	
	@Transactional
	@Test(expected = TreeNodeException.class)
	public void testReferentialIntegrityDeleteNodeWithChildren() {
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		IdmTreeNodeDto treeNode = getHelper().createTreeNode(treeType, null);
		getHelper().createTreeNode(treeType, treeNode);
	    // tree node cannot be deleted, when some contract are defined on this node
	    service.delete(treeNode);
	}
	
	@Transactional
	@Test(expected = TreeNodeException.class)
	public void testReferentialIntegrityDeleteNodeWithContractPositions() {
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmTreeNodeDto treeNode = getHelper().createTreeNode();
		IdmIdentityContractDto contract = getHelper().createIdentityContact(identity);
		getHelper().createContractPosition(contract, treeNode);
	    // tree node cannot be deleted, when some contract are defined on this node
	    service.delete(treeNode);
	}
	
	@Test
	// @Transactional - TODO: fix recount index in transaction
	public void testForestIndexAfterBulkMoveWithoutTransaction() {
		int rootCount = 5;
		// prepare new tree type
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		// create root nodes
		for (int i = 0; i < rootCount; i++) {
			getHelper().createTreeNode(treeType, null);
		}
		// move nodes to the first node
		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setTreeTypeId(treeType.getId());
		List<IdmTreeNodeDto> nodes = service.find(filter, null).getContent();
		IdmTreeNodeDto root = nodes.get(0);
		for (int i = 0; i < nodes.size(); i++) {
			IdmTreeNodeDto node = nodes.get(i);
			if (node.equals(root)) {
				continue;
			}
			node.setParent(root.getId());
			node = service.save(node);
		}		
		// check
		Assert.assertEquals(1L, service.findRoots(treeType.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, service.findChildrenByParent(root.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, treeNodeForestContentService.findDirectChildren(root.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, treeNodeForestContentService.findAllChildren(root.getId(), null).getTotalElements());
	}
	
	@Test
	@Transactional
	public void testForestIndexAfterBulkMoveWithTransaction() {
		int rootCount = 5;
		// prepare new tree type
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		// create root nodes
		for (int i = 0; i < rootCount; i++) {
			getHelper().createTreeNode(treeType, null);
		}
		// move nodes to the first node
		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setTreeTypeId(treeType.getId());
		List<IdmTreeNodeDto> nodes = service.find(filter, null).getContent();
		IdmTreeNodeDto root = nodes.get(0);
		for (int i = 0; i < nodes.size(); i++) {
			IdmTreeNodeDto node = nodes.get(i);
			if (node.equals(root)) {
				continue;
			}
			node.setParent(root.getId());
			node = service.save(node);
		}		
		// check
		Assert.assertEquals(1L, service.findRoots(treeType.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, service.findChildrenByParent(root.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, treeNodeForestContentService.findDirectChildren(root.getId(), null).getTotalElements());
		Assert.assertEquals(rootCount - 1, treeNodeForestContentService.findAllChildren(root.getId(), null).getTotalElements());
	}
	
	/**
	 * Move childern to new parent a delete previous parent
	 * 
	 */
	@Test
	@Transactional
	public void testMoveChildren() {
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		// create root node
		IdmTreeNodeDto root = getHelper().createTreeNode(treeType, null);
		IdmTreeNodeDto subRoot = getHelper().createTreeNode(treeType, root);
		// create children
		IdmTreeNodeDto nodeOne = getHelper().createTreeNode(treeType, subRoot);
		IdmTreeNodeDto nodeTwo = getHelper().createTreeNode(treeType, subRoot);
		IdmTreeNodeDto nodeThree = getHelper().createTreeNode(treeType, subRoot);
		//
		Assert.assertEquals(4, treeNodeForestContentService.findAllChildren(root.getId(), null).getTotalElements());
		Assert.assertEquals(3, treeNodeForestContentService.findAllChildren(subRoot.getId(), null).getTotalElements());
		//
		// move children to other parent
		IdmTreeNodeDto subRootTwo = getHelper().createTreeNode(treeType, root);
		nodeOne.setParent(subRootTwo.getId());
		nodeOne = service.save(nodeOne);
		nodeTwo.setParent(subRootTwo.getId());
		nodeTwo = service.save(nodeTwo);
		nodeThree.setParent(subRootTwo.getId());
		nodeThree = service.save(nodeThree);
		//
		// delete previous parent
		service.delete(subRoot);
		//
		Assert.assertEquals(4, treeNodeForestContentService.findAllChildren(root.getId(), null).getTotalElements());
		Assert.assertEquals(3, treeNodeForestContentService.findAllChildren(subRootTwo.getId(), null).getTotalElements());
	}
	
	@Test
	@Transactional
	public void testBadTreeTypeUpdate() {
		IdmTreeTypeDto parent1 = getHelper().createTreeType();
		IdmTreeTypeDto parent2 = getHelper().createTreeType();
		//
		IdmTreeNodeDto node1 = getHelper().createTreeNode(parent1, null);
		IdmTreeNodeDto node2 = getHelper().createTreeNode(parent1, node1);
		IdmTreeNodeDto node3 = getHelper().createTreeNode(parent1, node2);
		//
		node3.setTreeType(parent2.getId());
		try {
			node3 = service.save(node3);
			Assert.fail();
		} catch (TreeNodeException ex) { 
			Assert.assertTrue(ex.getMessage().contains("bad type"));
		} catch (Exception e) {
			Assert.fail();
		}
		//
		node1.setTreeType(parent2.getId());
		try {
			node1 = service.save(node1);
			Assert.fail();
		} catch (TreeNodeException ex) { 
			Assert.assertTrue(ex.getMessage().contains("bad type"));
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	@Transactional
	public void testBadTreeTypeCreate() {
		IdmTreeTypeDto parent1 = getHelper().createTreeType();
		IdmTreeTypeDto parent2 = getHelper().createTreeType();
		//
		IdmTreeNodeDto node1 = getHelper().createTreeNode(parent1, null);
		IdmTreeNodeDto node2 = getHelper().createTreeNode(parent1, node1);
		IdmTreeNodeDto node3 = getHelper().createTreeNode(parent1, node2);
		//
		try {
			getHelper().createTreeNode(parent2, node1);
			Assert.fail();
		} catch (TreeNodeException ex) { 
			Assert.assertTrue(ex.getMessage().contains("bad type"));
		} catch (Exception e) {
			Assert.fail();
		}
		//
		try {
			getHelper().createTreeNode(parent2, node3);
			Assert.fail();
		} catch (TreeNodeException ex) { 
			Assert.assertTrue(ex.getMessage().contains("bad type"));
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	@Transactional
	public void testFindAllParents() {
		// TODO: map in filter and move to rest test
		//
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		IdmTreeNodeDto node1 = getHelper().createTreeNode(treeType, null);
		IdmTreeNodeDto node2 = getHelper().createTreeNode(treeType, node1);
		IdmTreeNodeDto node3 = getHelper().createTreeNode(treeType, node2);
		getHelper().createTreeNode(treeType, node3);
		//
		List<IdmTreeNodeDto> parents = service.findAllParents(node3.getId(), null);
		//
		Assert.assertEquals(2, parents.size());
		Assert.assertTrue(parents.stream().anyMatch(n -> n.equals(node1)));
		Assert.assertTrue(parents.stream().anyMatch(n -> n.equals(node2)));
	}
	
	@Test
	public void testRebuildIndex() {
		IdmTreeTypeDto treeType = getHelper().createTreeType();
		IdmTreeNodeDto node1 = getHelper().createTreeNode(treeType, null);
		IdmTreeNodeDto node2 = getHelper().createTreeNode(treeType, node1);
		IdmTreeNodeDto node3 = getHelper().createTreeNode(treeType, node2);
		IdmTreeNodeDto node4 = getHelper().createTreeNode(treeType, node3);
		// before index will be droped
		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setTreeNode(node2.getId());
		filter.setRecursively(true);
		//
		List<IdmTreeNodeDto> results = service.find(filter, null).getContent();
		//
		Assert.assertEquals(2, results.size());
		Assert.assertTrue(results.stream().anyMatch(n -> n.equals(node3)));
		Assert.assertTrue(results.stream().anyMatch(n -> n.equals(node4)));
		//
		// drop indexes
		forestIndexService.dropIndexes(IdmTreeNode.toForestTreeType(treeType.getId()));
		//
		results = service.find(filter, null).getContent();
		Assert.assertEquals(0, results.size());
		//
		// reindex tree type
		service.rebuildIndexes(treeType.getId());
		//
		results = service.find(filter, null).getContent();
		Assert.assertEquals(2, results.size());
		Assert.assertTrue(results.stream().anyMatch(n -> n.equals(node3)));
		Assert.assertTrue(results.stream().anyMatch(n -> n.equals(node4)));
	}
}
