package eu.bcvsolutions.idm.core.eav.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.hateoas.core.Relation;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;

/**
 * Form instance values
 * 
 * @author Radek Tomiška
 */
@Relation(collectionRelation = "formValues")
public class IdmFormValueDto extends AbstractDto implements Requestable {

	private static final long serialVersionUID = 1L;
	public static final String PROPERTY_FORM_ATTRIBUTE = "formAttribute";
	//
	// @NotNull
	@JsonProperty(access = Access.READ_ONLY)
	private Serializable ownerId;
	// @NotEmpty
	@JsonProperty(access = Access.READ_ONLY)
	private Class<? extends FormableEntity> ownerType;
	@NotNull
	@Embedded(dtoClass = IdmFormAttributeDto.class)
	private UUID formAttribute;
	@NotNull
	private PersistentType persistentType;
	private boolean confidential;
	private String stringValue;
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	private String shortTextValue;
	private Boolean booleanValue;
	private Long longValue;
	private BigDecimal doubleValue;
	private DateTime dateValue;
	private byte[] byteValue;
	private UUID uuidValue;
	@Max(99999)
	private short seq;
	@Embedded(dtoClass = IdmRequestItemDto.class)
	private UUID requestItem; // Isn't persist in the entity
	//
	@JsonIgnore
	private transient FormableEntity owner;
	
	public IdmFormValueDto() {
	}
	
	public IdmFormValueDto(UUID id) {
		super(id);
	}
	
	public IdmFormValueDto(IdmFormAttributeDto formAttribute) {
		Assert.notNull(formAttribute);
		//
		setFormAttributeDto(formAttribute);
	}
	
	/**
	 * Set s owner and all attribute properties.
	 * 
	 * @param owner
	 * @param formAttribute
	 */
	public void setOwnerAndAttribute(FormableEntity owner, IdmFormAttributeDto formAttribute) {
		setOwner(owner);
		if (formAttribute != null) {
			setFormAttributeDto(formAttribute);
		} else {
			setFormAttribute(null);
		}
	}
	
	private void setFormAttributeDto(IdmFormAttributeDto formAttribute) {
		this.formAttribute = formAttribute.getId();
		this.persistentType = formAttribute.getPersistentType();
		this.confidential = formAttribute.isConfidential();
		//
		// set attribute in embedded (jpa model cannot be used on this layer (api))
		this.getEmbedded().put(PROPERTY_FORM_ATTRIBUTE, formAttribute);
	}
	
	public Serializable getOwnerId() {
		return ownerId;
	}
	
	public void setOwnerId(Serializable ownerId) {
		this.ownerId = ownerId;
	}
	
	public Class<? extends FormableEntity> getOwnerType() {
		return ownerType;
	}
	
	public void setOwnerType(Class<? extends FormableEntity> ownerType) {
		this.ownerType = ownerType;
	}

	public UUID getFormAttribute() {
		return formAttribute;
	}

	public void setFormAttribute(UUID formAttribute) {
		this.formAttribute = formAttribute;
	}

	public PersistentType getPersistentType() {
		return persistentType;
	}

	public void setPersistentType(PersistentType persistentType) {
		this.persistentType = persistentType;
	}

	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	public Long getLongValue() {
		return longValue;
	}

	public void setLongValue(Long longValue) {
		this.longValue = longValue;
	}

	public BigDecimal getDoubleValue() {
		return doubleValue;
	}

	public void setDoubleValue(BigDecimal doubleValue) {
		this.doubleValue = doubleValue;
	}

	public DateTime getDateValue() {
		return dateValue;
	}

	public void setDateValue(DateTime dateValue) {
		this.dateValue = dateValue;
	}

	public byte[] getByteValue() {
		return byteValue;
	}

	public void setByteValue(byte[] byteValue) {
		this.byteValue = byteValue;
	}
	
	public void setUuidValue(UUID uuidValue) {
		this.uuidValue = uuidValue;
	}
	
	public UUID getUuidValue() {
		return uuidValue;
	}

	public short getSeq() {
		return seq;
	}

	public void setSeq(short seq) {
		this.seq = seq;
	}
	
	/**
	 * Returns value by persistent type
	 * 
	 * @return
	 */
	@JsonProperty(access = Access.READ_ONLY)
	public Serializable getValue() {
		return getValue(persistentType);
	}

	/**
	 * Returns value by persistent type
	 * 
	 * @param persistentType
	 * @return
	 */
	public Serializable getValue(PersistentType persistentType) {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
		case INT:
			return longValue == null ? null : longValue.intValue();
		case LONG:
			return longValue;
		case BOOLEAN:
			return booleanValue;
		case DATE:
		case DATETIME:
			return dateValue;
		case DOUBLE:
			return doubleValue;
		case CHAR: 
			return stringValue == null ? null : stringValue.charAt(0);
		case BYTEARRAY: {
			return byteValue;
		}
		case ATTACHMENT:
		case UUID: {
			return uuidValue;
		}
		case SHORTTEXT: {
			return shortTextValue;
		}
		default:
			return stringValue;
		}
	}

	/**
	 * Returns true, if value by persistent type is empty (null or empty strings are evaluated)
	 *
	 * @return
	 */
	@JsonProperty(access = Access.READ_ONLY)
	public boolean isEmpty() {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
			case INT:
			case LONG:
				return longValue == null;
			case BOOLEAN:
				return booleanValue == null;
			case DATE:
			case DATETIME:
				return dateValue == null;
			case DOUBLE:
				return doubleValue == null;
			case BYTEARRAY: {
				return byteValue == null || byteValue.length == 0;
			}
			case ATTACHMENT:
			case UUID: {
				return uuidValue == null;
			}
			case SHORTTEXT: {
				return StringUtils.isEmpty(shortTextValue);
			}
			default:
				return StringUtils.isEmpty(stringValue);
		}
	}
	
	/**
	 * Returns true, if value by persistent type is {@code null}. Empty string are filled
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isNull() {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
			case INT:
			case LONG:
				return longValue == null;
			case BOOLEAN:
				return booleanValue == null;
			case DATE:
			case DATETIME:
				return dateValue == null;
			case DOUBLE:
				return doubleValue == null;
			case BYTEARRAY: {
				return byteValue == null;
			}
			case ATTACHMENT:
			case UUID: {
				return uuidValue == null;
			}
			case SHORTTEXT: {
				return shortTextValue == null;
			}
			default:
				return stringValue == null;
		}
	}
	
	/**
	 * Returns {@code true}, when value by persistent type is equal. 
	 * Returns {@code false}, when other is null.
	 * Returns {@code false}, when persistent types differs.
	 * 
	 * @param other
	 * @return
	 */
	public boolean isEquals(IdmFormValueDto other) {
		if (other == null) {
			return false;
		}
		if (!Objects.equals(persistentType, other.getPersistentType())) {
			return false;
		}
		if (isEmpty() && other.isEmpty()) {
			return true;
		}
		if((isEmpty() && !other.isEmpty()) || (!isEmpty() && other.isEmpty())) {
			return false;
		}
		//
		Assert.notNull(persistentType);
		switch (persistentType) {
			case DATE:
			case DATETIME:
				// date from FE vs DB has different chronology - we are using isEquals method
				return dateValue.isEqual(other.getDateValue());
			default:
				return Objects.equals(getValue(), other.getValue());
		}
	}

	/**
	 * Sets value by persistent type
	 *
	 * @param value
	 */
	public void setValue(Serializable value) {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
			case INT:
			case LONG:
				if (value == null) {
					setLongValue(null);
				} else if (value instanceof Long) {
					setLongValue((Long) value);
				} else if (value instanceof Integer) {
					setLongValue(((Integer) value).longValue());
				} else if (value instanceof Number) {
					setLongValue(((Number) value).longValue());
				} else if (value instanceof String) {
					setLongValue(Long.valueOf((String) value));
				} else {
					throw wrongType(value, null);
				}
				break;
			case BOOLEAN:
				if (value == null) {
					setBooleanValue(null);
				} else if (value instanceof Boolean) {
					setBooleanValue((Boolean) value);
				} else if (value instanceof String) {
					setBooleanValue(Boolean.valueOf((String) value));
				} else {
					throw wrongType(value, null);
				}
				break;
			case DATE:
			case DATETIME:
				if (value == null) {
					setDateValue(null);
				} else if (value instanceof DateTime) {
					setDateValue((DateTime) value);
				} else if (value instanceof LocalDate) {
					setDateValue(((LocalDate) value).toDateTimeAtStartOfDay());
				} else if (value instanceof Date) {
					setDateValue(new DateTime((Date) value));
				} else if (value instanceof Long) {
					setDateValue(new DateTime(( Long) value));
				} else if (value instanceof String) {
					try {
						setDateValue(DateTime.parse((String) value));
					} catch(IllegalArgumentException ex) {
						throw new ResultCodeException(
								CoreResultCode.FORM_VALUE_WRONG_TYPE, 
								"Form value [%s] for attribute [%s] with type [%s] supports ISO format only.", 
								ImmutableMap.of(
									"value", Objects.toString(value), 
									"formAttribute", formAttribute == null ? Objects.toString(formAttribute) : formAttribute, 
									"persistentType", persistentType, 
									"valueType", value == null ?  Objects.toString(null) : value.getClass().getCanonicalName()
								), 
								ex);
					}
				} else {
					throw wrongType(value, null);
				}
				break;
			case DOUBLE:
				if (value == null) {
					setDoubleValue(null);
				} else if (value instanceof BigDecimal) {
					setDoubleValue((BigDecimal) value);
				} else if (value instanceof Integer) {
					setDoubleValue(BigDecimal.valueOf((Integer) value));
				} else if (value instanceof Long) {
					setDoubleValue(BigDecimal.valueOf((Long) value));
				} else if (value instanceof Double) {
					setDoubleValue(BigDecimal.valueOf((Double) value));
				} else if (value instanceof Float) {
					setDoubleValue(BigDecimal.valueOf(((Float) value).doubleValue()));
				} else if (value instanceof Number) {
					setDoubleValue(BigDecimal.valueOf(((Number) value).doubleValue()));
				} else if (value instanceof String) {
					// TODO: parse, but how to solve separator by locale?
					throw wrongType(value, null);
				} else {
					throw wrongType(value, null);
				}
				break;
			case BYTEARRAY: {
				if (value == null) {
					setByteValue(null);
				} else if (value instanceof byte[]) {
					setByteValue((byte[]) value);
				} else if (value instanceof String) {
					// TODO: parse, but how ... byte64 or [45, 46, 78, 45]?
					throw wrongType(value, null);
				} else {
					throw wrongType(value, null);
				}
				break;
			}
			case ATTACHMENT:
			case UUID: {
				try {
					setUuidValue(DtoUtils.toUuid(value));
				} catch (ClassCastException ex) {
					throw wrongType(value, ex);
				}
				break;
			}
			case SHORTTEXT:
				if (value == null) {
					setShortTextValue(null);
				} else if (value instanceof String) {
					setShortTextValue((String) value);
				} else {
					throw wrongType(value, null);
				}
			default:
				if (value == null) {
					setStringValue(null);
				} else if (value instanceof String) {
					setStringValue((String) value);
				} else {
					throw wrongType(value, null);
				}
		}
	}
	
	/**
	 * Throws {@link ResultCodeException} exception only - value has wrong type
	 * 
	 * @param value
	 */
	private ResultCodeException wrongType(Serializable value, Exception ex) {
		return new ResultCodeException(CoreResultCode.FORM_VALUE_WRONG_TYPE, ImmutableMap.of(
				"value", Objects.toString(value), 
				"formAttribute", formAttribute == null ? Objects.toString(formAttribute) : formAttribute, 
				"persistentType", persistentType, 
				"valueType", value == null ?  Objects.toString(null) : value.getClass().getCanonicalName()
				), ex);
	}
	
	/**
	 * Clears all values
	 */
	public void clearValues() {
		this.booleanValue = null;
		this.stringValue = null;
		this.shortTextValue = null;
		this.dateValue = null;
		this.longValue = null;
		this.doubleValue = null;
		this.byteValue = null;
		this.uuidValue = null;
	}
	
	/**
	 * Sets all filled values from given value
	 * 
	 * @param value
	 * @since 9.3.0
	 */
	public void setValues(IdmFormValueDto value) {
		this.booleanValue = value.getBooleanValue();
		this.stringValue = value.getStringValue();
		this.shortTextValue = value.getShortTextValue();
		this.dateValue = value.getDateValue();
		this.longValue = value.getLongValue();
		this.doubleValue = value.getDoubleValue();
		this.byteValue = value.getByteValue();
		this.uuidValue = value.getUuidValue();
	}
	
	public void setOwner(FormableEntity owner) {
		this.owner = owner;
		setOwnerId(owner == null ? null : (UUID) owner.getId());
		setOwnerType(owner == null ? null : ((FormableEntity) owner).getClass());
	}
	
	public FormableEntity getOwner() {
		return owner;
	}
	
	public void setShortTextValue(String shortTextValue) {
		this.shortTextValue = shortTextValue;
	}
	
	public String getShortTextValue() {
		return shortTextValue;
	}
	
	@Override
	public UUID getRequestItem() {
		return requestItem;
	}

	@Override
	public void setRequestItem(UUID requestItem) {
		this.requestItem = requestItem;
	}
}
