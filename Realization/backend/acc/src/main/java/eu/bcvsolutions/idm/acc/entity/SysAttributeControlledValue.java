package eu.bcvsolutions.idm.acc.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;

/**
 * Controlled values (merge) for the mapped attribute
 * 
 * @author svandav
 *
 */
@Entity
@Table(name = "sys_attribute_contr_value")
public class SysAttributeControlledValue extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Audited
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "attribute_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey(name = "none")
	private SysSystemAttributeMapping attributeMapping;

	@Audited
	@Column(name = "historic_value", nullable = false)
	private boolean historicValue = false;

	@Audited
	@Column(name = "value", length = Integer.MAX_VALUE)
	private Serializable value;

	public SysSystemAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	public void setAttributeMapping(SysSystemAttributeMapping attributeMapping) {
		this.attributeMapping = attributeMapping;
	}

	public boolean isHistoricValue() {
		return historicValue;
	}

	public void setHistoricValue(boolean historicValue) {
		this.historicValue = historicValue;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}
}
