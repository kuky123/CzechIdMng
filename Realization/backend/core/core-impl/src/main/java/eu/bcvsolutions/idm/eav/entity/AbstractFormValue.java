package eu.bcvsolutions.idm.eav.entity;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.eav.domain.PersistentType;

/**
 * Super class for "extended" attribute values, which can be added to custom
 * abstract entity
 * 
 * @author Radek Tomiška
 *
 * @param <O> Owner entity class
 */
@MappedSuperclass
public abstract class AbstractFormValue<O extends FormableEntity> extends AbstractEntity {

	private static final long serialVersionUID = -5914285774914667917L;

	@ManyToOne(optional = false) // TODO: should we support values without definition?
	@JoinColumn(name = "attribute_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmFormAttribute formAttribute;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "persistent_type", length = 45, nullable = false)
	private PersistentType persistentType;
	
	@NotNull
	@Column(name = "confidental", nullable = false)
	private boolean confidental;

	@Size(max = DefaultFieldLengths.LOG) // TODO: @Lob?
	@Column(name = "string_value", nullable = true, length = DefaultFieldLengths.LOG)
	private String stringValue;

	@Column(name = "boolean_value", nullable = true)
	private Boolean booleanValue;

	@Column(name = "long_value", nullable = true)
	private Long longValue;

	@Column(name = "double_value", nullable = true, precision = 38, scale = 4)
	private BigDecimal doubleValue;

	@Column(name = "date_value")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateValue;

	@Max(99999)
	@Column(name = "seq")
	private int seq;

	public AbstractFormValue() {
	}

	public AbstractFormValue(UUID id) {
		super(id);
	}

	public AbstractFormValue(IdmFormAttribute formAttribute) {
		Assert.notNull(formAttribute);
		//
		this.formAttribute = formAttribute;
		this.persistentType = formAttribute.getPersistentType();
		this.confidental = formAttribute.isConfidental();
	}

	/**
	 * Returns entity, for witch is this attribute value
	 * 
	 * @return
	 */
	public abstract O getOwner();
	
	/**
	 * Sets this attribute value owner
	 * 
	 * @param owner
	 */
	public abstract void setOwner(O owner);

	/**
	 * Returns value by persistent type
	 * 
	 * @return
	 */
	public Object getValue() {
		return getValue(persistentType);
	}

	/**
	 * Returns value by persistent type
	 * 
	 * @param persistentType
	 * @return
	 */
	public Object getValue(PersistentType persistentType) {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
		case LONG:
			return longValue;
		case BOOLEAN:
			return booleanValue;
		case DATE:
		case DATE_TIME:
			return dateValue;
		case DOUBLE:
		case CURRENCY:
			return doubleValue;
		default:
			return stringValue;
		}
	}

	/**
	 * Returns true, if value by persistent type is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
			case LONG:
				return longValue == null;
			case BOOLEAN:
				return booleanValue == null;
			case DATE:
			case DATE_TIME:
				return dateValue == null;
			case DOUBLE:
			case CURRENCY:
				return doubleValue == null;
			default:
				return StringUtils.isEmpty(stringValue);
		}
	}

	/**
	 * Sets value by persintent type
	 *
	 * @param value
	 */
	public void setValue(Object value) {
		Assert.notNull(persistentType);
		//
		switch (persistentType) {
			case LONG:
				if (value == null) {
					setLongValue(null);
				} else if (value instanceof Long) {
					setLongValue((Long) value);
				} else if (value instanceof Integer) {
					setLongValue(((Integer) value).longValue());
				} else if (value instanceof Number) {
					setLongValue(((Number) value).longValue());
				} else {
					throw new IllegalArgumentException(MessageFormat.format("Form value [{0}] has to be [{1}], given [{2}]", 
							formAttribute.getName(), persistentType, value));
				}
				break;
			case BOOLEAN:
				if (value == null) {
					setBooleanValue(null);
				} else if (value instanceof Boolean) {
					setBooleanValue((Boolean) value);
				} else {
					throw new IllegalArgumentException(MessageFormat.format("Form value [{0}] has to be [{1}], given [{2}]", 
							formAttribute.getName(), persistentType, value));
				}
				break;
			case DATE:
			case DATE_TIME:
				if (value == null) {
					setDateValue(null);
				} else if (value instanceof Date) {
					setDateValue((Date) value);
				} else {
					throw new IllegalArgumentException(MessageFormat.format("Form value [{0}] has to be [{1}], given [{2}]", 
							formAttribute.getName(), persistentType, value));
				}
				break;
			case DOUBLE:
			case CURRENCY:
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
				} else {
					throw new IllegalArgumentException(MessageFormat.format("Form value [{0}] has to be [{1}], given [{2}]", 
							formAttribute.getName(), persistentType, value));
				}
				break;
			default:
				if (value == null) {
					setStringValue(null);
				} else if (value instanceof String) {
					setStringValue((String) value);
				} else {
					throw new IllegalArgumentException(MessageFormat.format("Form value [{0}] has to be [{1}], given [{2}]", 
							formAttribute.getName(), persistentType, value));
				}
		}
	}

	/**
	 * Attribute definition
	 * 
	 * @return
	 */
	public IdmFormAttribute getFormAttribute() {
		return formAttribute;
	}

	public void setFormAttribute(IdmFormAttribute formAttribute) {
		this.formAttribute = formAttribute;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public void setPersistentType(PersistentType persistentType) {
		this.persistentType = persistentType;
	}

	public PersistentType getPersistentType() {
		return persistentType;
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

	public Date getDateValue() {
		return dateValue;
	}

	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}
	
	public boolean isConfidental() {
		return confidental;
	}
	
	public void setConfidental(boolean confidental) {
		this.confidental = confidental;
	}
}