package xyz.jpmrno.niomps;

import xyz.jpmrno.niomps.dispatcher.Dispatcher;
import xyz.jpmrno.niomps.dispatcher.SingleSelectorDispatcher;
import xyz.jpmrno.niomps.dispatcher.Subscription;
import xyz.jpmrno.niomps.handlers.AcceptorBuilder;
import xyz.jpmrno.niomps.handlers.NCHandlerBuilder;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Dispatcher dispatcher;
        try {
            dispatcher = new SingleSelectorDispatcher();
        } catch (IOException exception) {
            System.err.println("Can't initialize dispatcher");
            exception.printStackTrace();
            return;
        }

        NCHandlerBuilder nchBuilder = new AcceptorBuilder();
        MultiProtocolServer server = new MultiProtocolServer(dispatcher, nchBuilder);
        server.run();
    }
}
