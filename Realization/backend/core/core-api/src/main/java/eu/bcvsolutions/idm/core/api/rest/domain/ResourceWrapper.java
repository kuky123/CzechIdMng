package eu.bcvsolutions.idm.core.api.rest.domain;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * @param <T> dto for resource
 * @author svandav
 * @deprecated use Resource or Resources from spring instead
 */
@Deprecated
@JsonIgnoreProperties({ "id" })
public class ResourceWrapper<T> extends ResourceSupport {

	@JsonUnwrapped
	private T resource;
	
	public ResourceWrapper(T resource) {
		this.resource = resource;
	}
	
	public T getResource() {
		return resource;
	}
}