package org.qortal.api.websocket;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.qortal.controller.TradeBot;
import org.qortal.data.crosschain.TradeBotData;
import org.qortal.event.Event;
import org.qortal.event.EventBus;
import org.qortal.event.Listener;
import org.qortal.repository.DataException;
import org.qortal.repository.Repository;
import org.qortal.repository.RepositoryManager;
import org.qortal.utils.Base58;

@WebSocket
@SuppressWarnings("serial")
public class TradeBotWebSocket extends ApiWebSocket implements Listener {

	/** Cache of trade-bot entry states, keyed by trade-bot entry's "trade private key" (base58) */
	private static final Map<String, TradeBotData.State> PREVIOUS_STATES = new HashMap<>();

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(TradeBotWebSocket.class);

		try (final Repository repository = RepositoryManager.getRepository()) {
			List<TradeBotData> tradeBotEntries = repository.getCrossChainRepository().getAllTradeBotData();
			if (tradeBotEntries == null)
				// How do we properly fail here?
				return;

			PREVIOUS_STATES.putAll(tradeBotEntries.stream().collect(Collectors.toMap(entry -> Base58.encode(entry.getTradePrivateKey()), TradeBotData::getState)));
		} catch (DataException e) {
			// No output this time
		}

		EventBus.INSTANCE.addListener(this::listen);
	}

	@Override
	public void listen(Event event) {
		if (!(event instanceof TradeBot.StateChangeEvent))
			return;

		TradeBotData tradeBotData = ((TradeBot.StateChangeEvent) event).getTradeBotData();
		String tradePrivateKey58 = Base58.encode(tradeBotData.getTradePrivateKey());

		synchronized (PREVIOUS_STATES) {
			if (PREVIOUS_STATES.get(tradePrivateKey58) == tradeBotData.getState())
				// Not changed
				return;

			PREVIOUS_STATES.put(tradePrivateKey58, tradeBotData.getState());
		}

		List<TradeBotData> tradeBotEntries = Collections.singletonList(tradeBotData);
		for (Session session : getSessions())
			sendEntries(session, tradeBotEntries);
	}

	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		// Send all known trade-bot entries
		try (final Repository repository = RepositoryManager.getRepository()) {
			List<TradeBotData> tradeBotEntries = repository.getCrossChainRepository().getAllTradeBotData();
			if (tradeBotEntries == null) {
				session.close(4001, "repository issue fetching trade-bot entries");
				return;
			}

			if (!sendEntries(session, tradeBotEntries)) {
				session.close(4002, "websocket issue");
				return;
			}
		} catch (DataException e) {
			// No output this time
		}

		super.onWebSocketConnect(session);
	}

	@OnWebSocketClose
	public void onWebSocketClose(Session session, int statusCode, String reason) {
		super.onWebSocketClose(session, statusCode, reason);
	}

	@OnWebSocketMessage
	public void onWebSocketMessage(Session session, String message) {
		/* ignored */
	}

	private boolean sendEntries(Session session, List<TradeBotData> tradeBotEntries) {
		try {
			StringWriter stringWriter = new StringWriter();
			marshall(stringWriter, tradeBotEntries);

			String output = stringWriter.toString();
			session.getRemote().sendStringByFuture(output);
		} catch (IOException e) {
			// No output this time?
			return false;
		}

		return true;
	}

}
