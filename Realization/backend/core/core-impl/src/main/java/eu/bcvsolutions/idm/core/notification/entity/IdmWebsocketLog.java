package eu.bcvsolutions.idm.core.notification.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Log for messages sent through websocket
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Entity
@Table(name = "idm_notification_websocket")
@Deprecated
public class IdmWebsocketLog extends IdmNotificationLog {

	private static final long serialVersionUID = 2144902485645685319L;
	@Deprecated
	public static final String NOTIFICATION_TYPE = "websocket";

	@Override
	public String getType() {
		return NOTIFICATION_TYPE;
	}

}
