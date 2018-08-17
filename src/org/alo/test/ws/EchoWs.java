package org.alo.test.ws;

import java.io.IOException;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/chat")
public class EchoWs {
	private static final Object DUMMY = new Object();
	private static ConcurrentHashMap<Session, Object> clients = new ConcurrentHashMap<>();

	@OnOpen
	public void onOpen(Session s) {
		log("Open Connection " + s.getId() + " total: " + (clients.size() + 1));
		clients.put(s, DUMMY);
		// broadcast("new session (" + clients.size() + " active now)", s);
	}

	@OnClose
	public void onClose(Session s) {
		log("Close Connection " + s.getId() + " total: " + (clients.size() - 1));
		clients.remove(s);
		// broadcast("closed session (" + clients.size() + " active now)", s);
	}

	@OnMessage
	public void onMessage(String message, Session s) {
		broadcast(message, s);
	}

	private void broadcast(String message, Session s) {
		log("broadcasting [" + message + "] from session: " + s.getId() + " to " + (clients.size() - 1)
				+ " other sessions");
		String msg = s.getId() + ": " + message;
		long t = System.currentTimeMillis();
		for (Session otherSession : clients.keySet()) {
			try {
				if (!otherSession.equals(s)) {
					synchronized (otherSession) {
						otherSession.getBasicRemote().sendText(msg);
					}
				}
			} catch (Exception e) {
				try {
					log("closing other session " + s.getId() + " due to error " + e.getClass().getName());
					clients.remove(otherSession);
					otherSession.close();

				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}
		}
		log("  broadcasted to " + (clients.size() - 1) + " sessions in " + (System.currentTimeMillis() - t) + " ms");
	}

	@OnError
	public void onError(Throwable e) {
		log("Error!!");
		e.printStackTrace();
	}

	private void log(String msg) {
		System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] " + msg);
	}
}
