package ar.com.fmj.feddler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.attribute.StoredResponse;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSockets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.undertow.util.Headers.ACCEPT_LANGUAGE;
import static io.undertow.util.LocaleUtils.getLocalesFromHeader;

public class MessagingEmitterHandler implements HttpHandler {

    private final HttpHandler next;
    private final WebSocketProtocolHandshakeHandler wsHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MessagingEmitterHandler(final HttpHandler next, final WebSocketProtocolHandshakeHandler wsHandler) {
        this.next = next;
        this.wsHandler = wsHandler;
    }

    protected static MessagingEmitterHandler messagingEmitterHandler(final HttpHandler next,
                                                                     final WebSocketProtocolHandshakeHandler wsHandler) {
        return new MessagingEmitterHandler(next, wsHandler);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Object> request = new HashMap<>();
        final Map<String, Object> response = new HashMap<>();
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        final List<String> requestHeadersList = new ArrayList<>(requestHeaders.size());
        requestHeaders.forEach(c -> {
            final String values = c.stream().collect(Collectors.joining(", "));
            requestHeadersList.add(c.getHeaderName() + ": " + values);
        });
        request.put("headers", requestHeadersList);
        request.put("uri", exchange.getRequestURI());
        final SecurityContext sc = exchange.getSecurityContext();
        addAuthentication(request, sc);

        final Map<String, String> requestCookies = exchange.getRequestCookies().entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
        request.put("cookies", requestCookies);
        request.put("locale", getLocalesFromHeader(exchange.getRequestHeaders().get(ACCEPT_LANGUAGE)));
        request.put("method", exchange.getRequestMethod().toString());
        request.put("protocol", exchange.getProtocol().toString());
        request.put("queryString", exchange.getQueryString());
        request.put("remoteAddr", exchange.getSourceAddress());
        request.put("remoteHost", exchange.getSourceAddress().getHostName());
        request.put("scheme", exchange.getRequestScheme());
        request.put("serverPort", exchange.getDestinationAddress().getPort());

        final AttachmentKey<RequestBodyStreamSourceConduit> key =
                AttachmentKey.create(RequestBodyStreamSourceConduit.class);
        exchange.addRequestWrapper((factory, exchange12) -> {
            final RequestBodyStreamSourceConduit requestBodyStreamSourceConduit =
                    new RequestBodyStreamSourceConduit(factory.create());
            exchange.putAttachment(key, requestBodyStreamSourceConduit);
            return requestBodyStreamSourceConduit;
        });

        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            final RequestBodyStreamSourceConduit attachment = exchange.getAttachment(key);
            request.put("body", attachment.getBody());
            addAuthentication(response, sc);

            final HeaderMap responseHeaders = exchange.getResponseHeaders();
            final List<String> responseHeadersList = new ArrayList<>(responseHeaders.size());
            responseHeaders.forEach(c -> {
                final String values = c.stream().collect(Collectors.joining(", "));
                responseHeadersList.add(c.getHeaderName() + ": " + values);
            });
            response.put("headers", responseHeadersList);

            Map<String, String> responseCookies = exchange.getResponseCookies().entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
            response.put("cookies", responseCookies);
            response.put("status", exchange1.getStatusCode());

            final String storedResponse = StoredResponse.INSTANCE.readAttribute(exchange1);
            if (storedResponse != null) {
                response.put("body", storedResponse);
            }

            nextListener.proceed();
            final Map<String, Map<String, Object>> message = new HashMap<>(2);
            message.put("request", request);
            message.put("response", response);
            try {
                final String value = objectMapper.writeValueAsString(message);
                wsHandler.getPeerConnections()
                        .forEach(session -> WebSockets.sendText(value, session, null));
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        next.handleRequest(exchange);
    }

    private void addAuthentication(final Map<String, Object> element, final SecurityContext sc) {
        if (sc != null) {
            if (sc.isAuthenticated()) {
                element.put("authType", sc.getMechanismName());
                element.put("principle", sc.getAuthenticatedAccount().getPrincipal());
            } else {
                element.put("authType", "none");
            }
        }
    }

}
