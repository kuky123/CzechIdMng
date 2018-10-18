package eu.bcvsolutions.idm.core.config.web;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.security.api.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.service.impl.JwtAuthenticationMapper;

/**
 * Websocket configuration
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebSocketConfig.class);

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("objectMapper")
	private ObjectMapper mapper;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(BaseDtoController.BASE_PATH + "/websocket-info").setAllowedOrigins("*").withSockJS();
	}

	/**
	 * Register custom interceptor, for CIDMST token preset.
	 */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.setInterceptors(new ChannelInterceptorAdapter() {

			private JwtAuthenticationMapper jwtTokenMapper;

			JwtAuthenticationMapper getJwtTokenMapper() {
				// lazy init is needed - this bean is instantiate to early
				if (jwtTokenMapper == null) {
					jwtTokenMapper = context.getBean(JwtAuthenticationMapper.class);
				}
				return jwtTokenMapper;
			}

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				// read CIDMST token, construct and validate authentication.
				// Validates expiration only.
				try {
					String token = accessor.getFirstNativeHeader(JwtAuthenticationMapper.AUTHENTICATION_TOKEN_NAME);
					IdmJwtAuthentication authentication = getJwtTokenMapper().readToken(token);
					if (authentication == null) {
						LOG.warn("Websocket security token for user [] not found");
						return message;
					}
					if (!authentication.isExpired()) {
						accessor.setUser(authentication);
					} else {
						LOG.info("Websocket security token for user [{}] is expired", authentication.getCurrentUsername());
					}
				} catch (IOException ex) {
					LOG.warn("Websocket security token is not valid", ex);
				}
				return message;
			}
		});
	}
}
