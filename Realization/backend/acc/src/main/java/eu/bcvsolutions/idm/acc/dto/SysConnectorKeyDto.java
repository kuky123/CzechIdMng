package eu.bcvsolutions.idm.acc.dto;

import java.io.Serializable;

import eu.bcvsolutions.idm.acc.entity.SysConnectorKey;
import eu.bcvsolutions.idm.ic.api.IcConnectorKey;

/**
 * DTO for {@link SysConnectorKey}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class SysConnectorKeyDto implements IcConnectorKey, Serializable {

	private static final long serialVersionUID = 430337513097070131L;

	private String framework;
	private String connectorName;
	private String bundleName;
	private String bundleVersion;

	public SysConnectorKeyDto() {
	}

	public SysConnectorKeyDto(IcConnectorKey key) {
		this.framework = key.getFramework();
		this.connectorName = key.getConnectorName();
		this.bundleName = key.getBundleName();
		this.bundleVersion = key.getBundleVersion();
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public String getConnectorName() {
		return connectorName;
	}

	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
	}

	public String getBundleName() {
		return bundleName;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public String getBundleVersion() {
		return bundleVersion;
	}

	public void setBundleVersion(String bundleVersion) {
		this.bundleVersion = bundleVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundleName == null) ? 0 : bundleName.hashCode());
		result = prime * result + ((bundleVersion == null) ? 0 : bundleVersion.hashCode());
		result = prime * result + ((connectorName == null) ? 0 : connectorName.hashCode());
		result = prime * result + ((framework == null) ? 0 : framework.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SysConnectorKeyDto)) {
			return false;
		}
		SysConnectorKeyDto other = (SysConnectorKeyDto) obj;
		if (bundleName == null) {
			if (other.bundleName != null) {
				return false;
			}
		} else if (!bundleName.equals(other.bundleName)) {
			return false;
		}
		if (bundleVersion == null) {
			if (other.bundleVersion != null) {
				return false;
			}
		} else if (!bundleVersion.equals(other.bundleVersion)) {
			return false;
		}
		if (connectorName == null) {
			if (other.connectorName != null) {
				return false;
			}
		} else if (!connectorName.equals(other.connectorName)) {
			return false;
		}
		if (framework == null) {
			if (other.framework != null) {
				return false;
			}
		} else if (!framework.equals(other.framework)) {
			return false;
		}
		return true;
	}
}
