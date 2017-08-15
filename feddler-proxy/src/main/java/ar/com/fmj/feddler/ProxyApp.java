package ar.com.fmj.feddler;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.StoredResponseHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static ar.com.fmj.feddler.MessagingEmitterHandler.messagingEmitterHandler;
import static io.undertow.Handlers.path;
import static io.undertow.Handlers.proxyHandler;
import static io.undertow.Handlers.websocket;
import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_404;

public class ProxyApp {

    public static void main(String[] args) {
        LoadBalancingProxyClient loadBalancer;
        try {
            final String target = args[0];
            loadBalancer = new LoadBalancingProxyClient()
                    .addHost(new URI(target))
                    .setConnectionsPerThread(20);
        } catch (final URISyntaxException exception) {
            throw new RuntimeException(exception);
        }

        final WebSocketProtocolHandshakeHandler websocket = websocket((exchange, channel) -> {
            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(final WebSocketChannel channel,
                                                 final BufferedTextMessage message) {
                    final String messageData = message.getData();
                    channel.getPeerConnections()
                            .forEach(session -> WebSockets.sendText(messageData, session, null));
                }
            });
            channel.resumeReceives();
        });

        Undertow server = Undertow.builder()
                .addHttpListener(Integer.valueOf(args[1]), "0.0.0.0",
                        path(createRootResourceHandler())
                                .addPrefixPath("/echo", websocket)
                                .addPrefixPath("/static", createStaticResourceHandler()))
                .addHttpListener(Integer.valueOf(args[2]), "0.0.0.0",
                        messagingEmitterHandler(
                                new StoredResponseHandler(proxyHandler(loadBalancer, 30000, HANDLE_404)), websocket))
                .build();
        server.start();
    }

    private static HttpHandler createStaticResourceHandler() {
        final ResourceManager staticResources =
                new ClassPathResourceManager(ProxyApp.class.getClassLoader(), "monitor/static");
        final ResourceManager cachedResources =
                new CachingResourceManager(100, 65536,
                        new DirectBufferCache(1024, 10, 10480),
                        staticResources,
                        (int) Duration.ofDays(1).getSeconds());
        return new ResourceHandler(cachedResources);
    }

    private static HttpHandler createRootResourceHandler() {
        final ResourceManager staticResources =
                new ClassPathResourceManager(ProxyApp.class.getClassLoader(), "monitor");
        final ResourceManager cachedResources =
                new CachingResourceManager(100, 65536,
                        new DirectBufferCache(1024, 10, 10480),
                        staticResources,
                        (int) Duration.ofDays(1).getSeconds());
        return new ResourceHandler(cachedResources);
    }

}
