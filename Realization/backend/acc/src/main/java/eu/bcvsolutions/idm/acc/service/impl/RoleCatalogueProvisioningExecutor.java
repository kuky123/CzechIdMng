package eu.bcvsolutions.idm.acc.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccRoleCatalogueAccountDto;
import eu.bcvsolutions.idm.acc.dto.EntityAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.EntityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.RoleCatalogueAccountFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccAccountManagementService;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccRoleCatalogueAccountService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningExecutor;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleCatalogue;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleCatalogueService;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Service for do role Catalogue provisioning
 * 
 * @author svandav
 *
 */
@Service
@Qualifier(value=RoleCatalogueProvisioningExecutor.NAME)
public class RoleCatalogueProvisioningExecutor extends AbstractProvisioningExecutor<IdmRoleCatalogue> {
 
	public static final String NAME = "roleCatalogueProvisioningService";
	private final AccRoleCatalogueAccountService catalogueAccountService;
	private final IdmRoleCatalogueService catalogueService;
	
	@Autowired
	public RoleCatalogueProvisioningExecutor(SysSystemMappingService systemMappingService,
			SysSystemAttributeMappingService attributeMappingService, IcConnectorFacade connectorFacade,
			SysSystemService systemService, SysRoleSystemService roleSystemService,
			AccAccountManagementService accountManagementService,
			SysRoleSystemAttributeService roleSystemAttributeService, SysSystemEntityService systemEntityService,
			AccAccountService accountService, AccRoleCatalogueAccountService catalogueAccountService,
			ProvisioningExecutor provisioningExecutor, IdmRoleCatalogueService catalogueService,
			EntityEventManager entityEventManager) {
		
		super(systemMappingService, attributeMappingService, connectorFacade, systemService, roleSystemService,
				accountManagementService, roleSystemAttributeService, systemEntityService, accountService,
				provisioningExecutor, entityEventManager);
		
		Assert.notNull(catalogueAccountService);
		Assert.notNull(catalogueService);
		
		this.catalogueAccountService = catalogueAccountService;
		this.catalogueService = catalogueService;
	}
	
	@Override
	protected Object getAttributeValue(String uid, IdmRoleCatalogue entity, AttributeMapping attribute) {
		Object idmValue = super.getAttributeValue(uid, entity, attribute);

		if (attribute.isEntityAttribute()
				&& TreeSynchronizationExecutor.PARENT_FIELD.equals(attribute.getIdmPropertyName())) {
			// For Tree we need do transform parent (IdmTreeNode) to resource
			// parent format (UID of parent)
			if (idmValue instanceof IdmRoleCatalogue) {
				// Generally we expect IdmRoleCatalogue as parent (we will do
				// transform)
				RoleCatalogueAccountFilter catalogueAccountFilter = new RoleCatalogueAccountFilter();
				catalogueAccountFilter.setSystemId(attribute.getSchemaAttribute().getObjectClass().getSystem().getId());
				catalogueAccountFilter.setEntityId(((IdmRoleCatalogue) idmValue).getId());
				List<AccRoleCatalogueAccountDto> treeAccounts = catalogueAccountService.find(catalogueAccountFilter, null).getContent();
				if (treeAccounts.isEmpty()) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_TREE_PARENT_ACCOUNT_NOT_FOUND,
							ImmutableMap.of("parentNode", idmValue));
				}
				if (treeAccounts.size() != 1) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_TREE_TOO_MANY_PARENT_ACCOUNTS,
							ImmutableMap.of("parentNode", idmValue));
				}
				AccRoleCatalogueAccountDto treeAccount = treeAccounts.get(0);
				String parentUid = accountService.get(treeAccount.getAccount()).getUid();
				return parentUid;
			} else {
				// If is parent not instance of IdmTreeNode, then we set value
				// without any transform
				return idmValue;
			}
		}
		return idmValue;
	}
	
	@Override
	protected List<SysRoleSystemAttribute> findOverloadingAttributes(String uid, IdmRoleCatalogue entity, SysSystem system,
			List<? extends EntityAccountDto> idenityAccoutnList, SystemEntityType entityType) {
		// Overloading attributes is not implemented for RoleCatalogue
		return new ArrayList<>();
	}
	

	@Override
	protected EntityAccountFilter createEntityAccountFilter() {
		return new RoleCatalogueAccountFilter();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected ReadWriteDtoService getEntityAccountService() {
		return catalogueAccountService;
	}

	@Override
	protected EntityAccountDto createEntityAccountDto() {
		return new AccRoleCatalogueAccountDto();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected ReadWriteDtoService getEntityService() {
		return catalogueService;
	}

	@Override
	public boolean supports(SystemEntityType delimiter) {
		return SystemEntityType.ROLE_CATALOGUE == delimiter;
	}
}