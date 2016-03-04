package com.importsource.pattern.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel; // listening for incoming TCP connections
import java.nio.channels.SocketChannel;    // TCP connection
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ReactiveEchoServer implements Runnable {
    private final Selector _selector;
    private final ServerSocketChannel _serverSocketChannel;
    private static final int WORKER_POOL_SIZE = 10;
    private static ExecutorService _workerPool;

    ReactiveEchoServer(int port) throws IOException {
        _selector = Selector.open();
        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.socket().bind(new InetSocketAddress(port));
        _serverSocketChannel.configureBlocking(false);

        // Register _serverSocketChannel with _selector listening on OP_ACCEPT events.
       // Callback: Acceptor, selected when a new connection incomes.
        SelectionKey selectionKey = _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new Acceptor());
    }

    public void run() {
        try {
            // Event Loop
            while (true) {
                _selector.select();
                Iterator it = _selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey sk = (SelectionKey) it.next();
                    it.remove();
                    Runnable r = (Runnable) sk.attachment(); // handler or acceptor callback/runnable
                    if (r != null) {
                        r.run();
                    }
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static ExecutorService getWorkerPool() {
        return _workerPool;
    }

    // Acceptor: if connection is established, assign a handler to it.
    private class Acceptor implements Runnable {
        public void run() {
            try {
                SocketChannel socketChannel = _serverSocketChannel.accept();
                if (socketChannel != null) {
                    new Handler(_selector, socketChannel);
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        _workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);

        try {
            new Thread(new ReactiveEchoServer(9090)).start(); // a single thread blocking on selector for events
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}