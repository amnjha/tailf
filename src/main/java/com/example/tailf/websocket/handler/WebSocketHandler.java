package com.example.tailf.websocket.handler;

import com.example.tailf.file.ChangePublisher;
import com.example.tailf.websocket.config.BeanProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;


public class WebSocketHandler extends AbstractWebSocketHandler {
	private String watchFilePath;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		ChangePublisher changePublisher = BeanProvider.getBean(ChangePublisher.class);
		changePublisher.registerSession(session);
		session.sendMessage(new TextMessage("Starting Publish"));
		for(String message : changePublisher.getInitialMessage()){
			session.sendMessage(new TextMessage(message));
		}
		super.afterConnectionEstablished(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		ChangePublisher publisher = BeanProvider.getBean(ChangePublisher.class);
		publisher.invalidateSession(session);
		super.afterConnectionClosed(session, status);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		super.handleTextMessage(session, message);
	}
}
