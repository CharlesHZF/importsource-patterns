package com.importsource.pattern.reactor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoServer {
    private static final int PORT = 4000;
    private static final int BUFFER_SZ = 1024;

    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(PORT);
            ExecutorService executor = Executors.newCachedThreadPool();
            while (true) {
                Socket s = server.accept();
                executor.submit(new Handler(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Handler implements Runnable {
        Socket _s;

        public Handler(Socket s) {
            _s = s;
        }

        public void run() {
            try {
                InputStream in = _s.getInputStream();
                OutputStream out = _s.getOutputStream();
                int read = 0;
                byte[] buf = new byte[BUFFER_SZ];
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    _s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}