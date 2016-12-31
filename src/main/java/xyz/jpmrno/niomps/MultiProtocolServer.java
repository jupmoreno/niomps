package xyz.jpmrno.niomps;

import xyz.jpmrno.niomps.dispatcher.Dispatcher;
import xyz.jpmrno.niomps.dispatcher.SubscriptionType;
import xyz.jpmrno.niomps.handlers.NewConnectionHandler;
import xyz.jpmrno.niomps.io.Closeables;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiProtocolServer implements Runnable {
    private final Map<Integer, ServerSocketChannel> channels;

    private final Dispatcher dispatcher;

    private AtomicBoolean running = new AtomicBoolean(false);

    public MultiProtocolServer(Dispatcher dispatcher) {
        this.channels = new HashMap<>();
        this.dispatcher = dispatcher;
    }

    public void addProtocol(int port, ProtocolHandlerFactory factory) throws IOException {
        if (running.get()) {
            return;
        }

        if (!(port >= 1 && port <= 65535)) {
            throw new IllegalArgumentException("Port number should be between 1 and 65535 "
                    + "inclusive");
        }

        Objects.requireNonNull(factory, "Protocol handler factory can't be null");

        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);

        ConnectionHandlerFactory connectionHandlers = new ProxyConnectionHandlerFactory(
                dispatcher, factory);
        NewConnectionHandler acceptHandler = new Acceptor(channel, dispatcher, connectionHandlers);

        dispatcher.subscribe(channel, acceptHandler).register(SubscriptionType.ACCEPT);

        ServerSocketChannel prev = channels.put(port, channel);

        if (prev != null) {
            Closeables.closeSilently(prev);
        }
    }

    public void removeProtocolOnPort(int port) {
        if (running.get()) {
            return;
        }

        ServerSocketChannel channel = channels.remove(port);
        Closeables.closeSilently(channel);
    }

    public void clearProtocols() {
        if (running.get()) {
            return;
        }

        for (Integer port : channels.keySet()) {
            ServerSocketChannel channel = channels.remove(port);
            Closeables.closeSilently(channel);
        }
    }

    @Override
    public void run() {
        if (running.get()) {
            return;
        }

        if (channels.isEmpty()) {
            throw new IllegalStateException("No protocols specified");
        }

        running.set(true);

        try {
            for (Integer port : channels.keySet()) {
                channels.get(port).bind(new InetSocketAddress(port));
            }
        } catch (Exception exception) {
            running.set(false);
            clearProtocols();
        }

        try {
            while (running.get()) {
                dispatcher.dispatch();
            }
        } catch (Exception exception) {
            // TODO: logger.error("Dispatcher force closed", exception);
        } finally {
            running.set(false);
            clearProtocols();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
