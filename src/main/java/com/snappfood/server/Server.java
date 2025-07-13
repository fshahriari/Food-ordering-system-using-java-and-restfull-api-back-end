package com.snappfood.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        try {
            // 1. Create a non-blocking server socket channel
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT));

            // 2. Create a selector to monitor the channel
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 3. Create a fixed-size thread pool for worker threads
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            System.out.println("Server started on port: " + PORT);

            // 4. Start the main event loop
            while (true) {
                selector.select(); // Wait for network activity

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        // 5. Accept new connections
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
                    } else if (key.isReadable()) {
                        // 6. Read data from a client
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        // --- FIX IS HERE ---
                        // Get the address before reading, in case the read causes a disconnect.
                        SocketAddress remoteAddress = clientChannel.getRemoteAddress();

                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead;

                        try {
                            bytesRead = clientChannel.read(buffer);
                        } catch (IOException e) {
                            // This can happen if the client disconnects abruptly.
                            bytesRead = -1;
                        }


                        if (bytesRead == -1) {
                            // Client has disconnected.
                            System.out.println("Client disconnected: " + remoteAddress);
                            clientChannel.close(); // Now it's safe to close.
                            continue;
                        }

                        // Hand off the request to a worker thread
                        executorService.submit(new RequestHandler(new String(buffer.array(), 0, bytesRead), clientChannel));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
