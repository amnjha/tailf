package com.example.tailf.websocket.config;

import com.example.tailf.websocket.handler.WebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

	private WebSocketHandler handler;

	public WebSocketConfiguration() {
		handler = new WebSocketHandler();
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
		webSocketHandlerRegistry.addHandler(handler, "/logs").setAllowedOrigins("*");
	}

	public WebSocketHandler getHandler() {
		return handler;
	}
}
