package eu.bcvsolutions.idm.core.ecm.api.entity;

import java.nio.charset.Charset;
import java.util.UUID;

import org.springframework.http.MediaType;

import eu.bcvsolutions.idm.core.api.entity.BaseEntity;

/**
 * Entity can have attachments
 * 
 * @author Radek Tomiška
 * @since 7.6.0
 */
public interface AttachableEntity extends BaseEntity {

	Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	String DEFAULT_ENCODING = DEFAULT_CHARSET.name();
	String DEFAULT_MIMETYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;
	
	/**
	 * Entity with {@link UUID} identifier only
	 */
	@Override
	UUID getId();
}
