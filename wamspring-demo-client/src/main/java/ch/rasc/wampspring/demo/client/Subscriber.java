package ch.rasc.wampspring.demo.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WelcomeMessage;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class Subscriber {

	private final static CountDownLatch latch = new CountDownLatch(1_000_000);

	@Bean
	public WebSocketClient webSocketClient() {
		return new StandardWebSocketClient();
	}

	@Bean
	public SubscribeWebSocketHandler clientWebSocketHandler() {
		return new SubscribeWebSocketHandler();
	}

	@Bean
	public WebSocketConnectionManager webSocketConnectionManager(
			WebSocketClient webSocketClient, WebSocketHandler webSocketHandler) {
		WebSocketConnectionManager manager = new WebSocketConnectionManager(
				webSocketClient, webSocketHandler, "ws://localhost:8080/wamp");
		manager.setAutoStartup(true);
		return manager;
	}

	public static void main(String[] args) throws InterruptedException {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Subscriber.class)) {

			if (!latch.await(3, TimeUnit.MINUTES)) {
				System.out.println("SOMETHING WENT WRONG");
			}
			System.out.println("THE END");
		}
	}

	static class SubscribeWebSocketHandler extends TextWebSocketHandler {

		private final JsonFactory jsonFactory = new MappingJsonFactory(new ObjectMapper());

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message)
				throws Exception {
			WampMessage wampMessage = WampMessage.fromJson(jsonFactory,
					message.getPayload());

			if (wampMessage instanceof WelcomeMessage) {
				System.out.println("WELCOME received: " + wampMessage);

				SubscribeMessage subscribe = new SubscribeMessage("/test/myqueue");
				session.sendMessage(new TextMessage(subscribe.toJson(jsonFactory)));
			}
			else if (wampMessage instanceof EventMessage) {
				latch.countDown();
			}

		}
	}

}
