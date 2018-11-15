package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.ExternalIdentifiable;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;

/**
 * Filter for tree node
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 */
public class IdmTreeNodeFilter extends DataFilter implements CorrelationFilter, ExternalIdentifiable {

	public static final String PARAMETER_CODE = "code"; // PARAMETER_CODEABLE_IDENTIFIER can be used too
	/**
	 * Tree type identifier
	 */
	public static final String PARAMETER_TREE_TYPE_ID = "treeTypeId";
	/**
	 * Parent tree node identifier 
	 */
	public static final String PARAMETER_PARENT_TREE_NODE_ID = "treeNodeId";
	/**
	 * Search for tree nodes within the default tree type 
	 */
	public static final String PARAMETER_DEFAULT_TREE_TYPE = "defaultTreeType";
	/**
	 * Tree nodes by tree structure recursively down
	 */
	public static final String PARAMETER_RECURSIVELY = "recursively";
	public static final boolean DEFAULT_RECURSIVELY = true;
	
	/**
	 * Attribute name to search for, like 'code' or 'name'
	 * 
	 * @deprecated @since 8.2.0 use PARAMETER_CORRELATION_PROPERTY
	 */
	@Deprecated
	public static final String PARAMETER_PROPERTY = PARAMETER_CORRELATION_PROPERTY;
	
	/**
	 * Value of the attribute defined in property to search for
	 * 
	 * @deprecated @since 8.2.0 use PARAMETER_CORRELATION_VALUE
	 */
	@Deprecated
	public static final String PARAMETER_VALUE = PARAMETER_CORRELATION_VALUE;
	
	/**
	 * TODO: Search roots - true / false.
	 */
	// public static final String PARAMETER_ROOTS = "roots";

    public IdmTreeNodeFilter() {
        this(new LinkedMultiValueMap<>());
    }

    public IdmTreeNodeFilter(MultiValueMap<String, Object> data) {
        super(IdmTreeNodeDto.class, data);
    }

    public UUID getTreeTypeId() {
        return DtoUtils.toUuid(data.getFirst(PARAMETER_TREE_TYPE_ID));
    }

    public void setTreeTypeId(UUID treeTypeId) {
    	data.set(PARAMETER_TREE_TYPE_ID, treeTypeId);
    }

    public void setTreeNode(UUID treeNode) {
    	data.set(PARAMETER_PARENT_TREE_NODE_ID, treeNode);
    }

    public UUID getTreeNode() {
    	return DtoUtils.toUuid(data.getFirst(PARAMETER_PARENT_TREE_NODE_ID));
    }

    public Boolean getDefaultTreeType() {
    	Object first = data.getFirst(PARAMETER_DEFAULT_TREE_TYPE);
    	if (first == null) {
    		return null;
    	}
    	return BooleanUtils.toBoolean(first.toString());
    }

    public void setDefaultTreeType(Boolean defaultTreeType) {
    	data.set(PARAMETER_DEFAULT_TREE_TYPE, defaultTreeType);
    }

    public boolean isRecursively() {
    	Object first = data.getFirst(PARAMETER_RECURSIVELY);
    	if (first == null) {
    		return DEFAULT_RECURSIVELY;
    	}
    	return BooleanUtils.toBoolean(first.toString());
    }

    public void setRecursively(boolean recursively) {
    	data.set(PARAMETER_RECURSIVELY, recursively);
    }

	@Override
	public String getProperty() {
		return (String) data.getFirst(PARAMETER_CORRELATION_PROPERTY);
	}

	@Override
	public void setProperty(String property) {
		data.set(PARAMETER_CORRELATION_PROPERTY, property);
	}

	@Override
	public String getValue() {
		return (String) data.getFirst(PARAMETER_CORRELATION_VALUE);
	}

	@Override
	public void setValue(String value) {
		data.set(PARAMETER_CORRELATION_VALUE, value);
	}
    
    public String getCode() {
		return (String) data.getFirst(PARAMETER_CODE);
	}

	public void setCode(String code) {
		data.set(PARAMETER_CODE, code);
	}
	
	@Override
	public String getExternalId() {
		return (String) data.getFirst(PROPERTY_EXTERNAL_ID);
	}
	
	@Override
	public void setExternalId(String externalId) {
		data.set(PROPERTY_EXTERNAL_ID, externalId);
	}
	
	
}
