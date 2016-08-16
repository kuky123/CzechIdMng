package eu.bcvsolutions.idm.core.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;

import eu.bcvsolutions.idm.core.model.domain.DefaultFieldLengths;

@Entity
@Table(name = "idm_organization", indexes = { @Index(name = "ux_organization_name", columnList = "name") })
public class IdmOrganization extends AbstractEntity {
	
	private static final long serialVersionUID = -3099001738101202320L;

	@NotEmpty
	@Size(min = 0, max = DefaultFieldLengths.NAME)
	@Column(name = "name", length = DefaultFieldLengths.NAME, nullable = false, unique = true)
	private String name;
	
	@NotNull
	@Column(name = "disabled", nullable = false)
	private boolean disabled = false;
	
	@ManyToOne(optional = true)
	@JoinColumn(name = "parent_id", referencedColumnName = "id")
	private IdmOrganization parent;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public void setParent(IdmOrganization parent) {
		this.parent = parent;
	}
	
	public IdmOrganization getParent() {
		return this.parent;
	}
}
