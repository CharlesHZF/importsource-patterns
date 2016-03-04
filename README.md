# pattern
pattern
Understanding Reactor Pattern for Highly Scalable I/O Bound Web Server
Jan 13th, 2015 | Comments

What is the best practice of a server architecture for an I/O bound web application on commodity machines? Why is the answer event-driven reactor pattern with async non-blocking I/O? How to implement an echo web server with reactor pattern in Java? Why does reactor pattern come with JavaScript and Node.js?

To answer these questions, let us first look at how an HTTP request is handled in general. After accepting the incoming request, the server establishes a TCP connection. It reads and parses the content in the request from the socket (CPU bound). Then the request is dispatched to the application level for domain-specific logics, which would probably visit the file system for data. Or even more, since we are investigating a scalable website for high raw data throughput (I/O bound), and all complex components are decoupled, the server will probably execute a network-based task, e.g. fetching data from remote caches and databases. Once finished, the server writes the response to the client, and waits for the next request, or closes the connection.

1. Why async non-blocking I/O?
Since we are assuming it is an I/O bound web application (which is often the case), I/O operations can be extremely slow compared to the processing of data. Think about switching electric current vs. a physical hard drive seeking a track.

Traditionally, we write an application to execute I/O operations in a synchronous and blocking way, that is to say, if the CPU has to wait for the I/O device to load all the data slowly, it has to wait and do nothing else. What? Idle CPU resources? Bad news for us! We should exhaust them!

1
2
3
4
// Sync blocking  I/O example in JavaScript.
var fs = require('fs');
var data = fs.readFileSync('filename'); // wait for returning data, and waste valuable CPU time.
console.log(data);
Why not let the control flow and I/O operations return immediately just a status, and free the CPU from waiting and do other meaningful operations? After all, we can still revisit the status or results later. Here comes the notion of aync non-blocking I/O.

1
2
3
4
5
6
// Async non-blocking I/O example in JavaScript.
var fs = require('fs');
fs.readFile('filename', function(err, data) {
    console.log(data);
}); // returns immediately, function will do the work when the data is ready.
// do other meaningful operations.
It looks quite straightforward in JavaScript as shown above, but how is it implemented under the hood? Intuition told me it was manually done by the application developers with threads, but I was wrong. Actually, there are various ways to do this – different programming languages have their own libraries (e.g. NIO for Java, libuv for JavaScript) on different operating systems. And the operating systems themselves also provide system calls in the kernel level – e.g. select, poll, epoll, and kqueue.

2. Why event-driven?
To handle web requests, there are two competitive web architectures – thread-based one and event-driven one.

2.1 Thread-based Architecture

The most intuitive way to implement a multi-threaded server is to follow the process/thread-per-connection approach.

In reality, the first HTTP server, CERN httpd, was created with a process-per-connection model. Nowadays Apache-MPM prefork still retains the feature for the following reasons.

It is appropriate for sites that need to avoid threading for compatibility with non-thread-safe libraries. It is also the best MPM for isolating each request, so that a problem with a single request will not affect any other.
However, the isolation and thread-safety come at a price. Processes are too heavyweight with slower context-switching and memory-consuming. Therefore, the thread-per-connection approach comes into being for better scalability, though programming with threads is error-prone and hard-to-debug.

In order to tune the number of threads for the best overall performance and avoid thread-creating/destroying overhead, it is a common practice to put a single dispatcher thread (acceptor thread) in front of a bounded blocking queue and a threadpool (worker threads). The dispatcher blocks on the socket for new connections and offers them to the bounded blocking queue. Connections exceeding the limitation of the queue will be dropped, but latencies for accepted connections become predictable. A pool of threads poll the queue for incoming requests, and then process and respond.

Apache-MPM worker takes advantages of both processes and threads (threadpool).

By using threads to serve requests, it is able to serve a large number of requests with fewer system resources than a process-based server. However, it retains much of the stability of a process-based server by keeping multiple processes available, each with many threads.
Here is a simple implementation with a threadpool for connections:

1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
52
53
54
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.*;

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

        @Override
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
Unfortunately, there is always a one-to-one relationship between connections and threads. Long-living connections like Keep-Alive connections give rise to a large number of worker threads waiting in the idle state for whatever it is slow, e.g. file system access, network, etc. In addition, hundreds or even thousands of concurrent threads can waste a great deal of stack space in the memory.

2.2 Event-driven Architecture

Event-driven approach can separate threads from connections, which only uses threads for events on specific callbacks/handlers.

3. Reactor Pattern
The reactor pattern is one implementation technique of the event-driven architecture. In simple words, it uses a single threaded event loop blocking on resources emitting events and dispatches them to corresponding handlers/callbacks. There is no need to block on I/O, as long as handlers/callbacks for events are registered to take care of them. Events are like incoming a new connection, ready for read, ready for write, etc. Those handlers/callbacks may utilize a threadpool in multi-core environments.

This pattern decouples modular application-level code from reusable reactor implementation.

3.1 Reactor Pattern Explained with Echo Web Server in Java

Wait! Talk is cheap and show me the code :) Yeah, now let’s build an echo web server that can be tested with telnet localhost 9090. You can also try to build with Netty, a NIO client server framework.

In the following code, a single boss thread is in an event loop blocking on a selector, which is registered with several channels and handlers. Associated handlers will be executed by the boss thread for specific events (accept, read, write operations) coming from those channels. In terms of processing the request, a threadpool is still used.

1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
52
53
54
55
56
57
58
59
60
61
62
63
64
65
66
67
68
69
70
71
72
73
74
75
76
77
78
79
80
81
package reactor;

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
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
52
53
54
55
56
57
58
59
60
61
62
63
64
65
66
67
68
69
70
71
72
73
74
75
76
77
78
79
80
81
82
83
84
85
86
87
88
89
90
91
92
93
94
95
96
97
98
99
100
101
102
package reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class Handler implements Runnable {
   private final SocketChannel _socketChannel;
   private final SelectionKey _selectionKey;

   private static final int READ_BUF_SIZE = 1024;
   private static final int WRiTE_BUF_SIZE = 1024;
   private ByteBuffer _readBuf = ByteBuffer.allocate(READ_BUF_SIZE);
   private ByteBuffer _writeBuf = ByteBuffer.allocate(WRiTE_BUF_SIZE);

   public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
       _socketChannel = socketChannel;
       _socketChannel.configureBlocking(false);

       // Register _socketChannel with _selector listening on OP_READ events.
       // Callback: Handler, selected when the connection is established and ready for READ
       _selectionKey = _socketChannel.register(selector, SelectionKey.OP_READ);
       _selectionKey.attach(this);
       selector.wakeup(); // let blocking select() return
   }

   public void run() {
       try {
           if (_selectionKey.isReadable()) {
               read();
           }
           else if (_selectionKey.isWritable()) {
               write();
           }
       }
       catch (IOException ex) {
           ex.printStackTrace();
       }
   }

   // Process data by echoing input to output
   synchronized void process() {
       _readBuf.flip();
       byte[] bytes = new byte[_readBuf.remaining()];
       _readBuf.get(bytes, 0, bytes.length);
       System.out.print("process(): " + new String(bytes, Charset.forName("ISO-8859-1")));

       _writeBuf = ByteBuffer.wrap(bytes);

       // Set the key's interest to WRITE operation
       _selectionKey.interestOps(SelectionKey.OP_WRITE);
       _selectionKey.selector().wakeup();
   }

   synchronized void read() throws IOException {
       try {
           int numBytes = _socketChannel.read(_readBuf);
           System.out.println("read(): #bytes read into '_readBuf' buffer = " + numBytes);

           if (numBytes == -1) {
               _selectionKey.cancel();
               _socketChannel.close();
               System.out.println("read(): client connection might have been dropped!");
           }
           else {
               ReactiveEchoServer.getWorkerPool().execute(new Runnable() {
                   public void run() {
                       process();
                   }
               });
           }
       }
       catch (IOException ex) {
           ex.printStackTrace();
           return;
       }
   }

   void write() throws IOException {
       int numBytes = 0;

       try {
           numBytes = _socketChannel.write(_writeBuf);
           System.out.println("write(): #bytes read from '_writeBuf' buffer = " + numBytes);

           if (numBytes > 0) {
               _readBuf.clear();
               _writeBuf.clear();

               // Set the key's interest-set back to READ operation
               _selectionKey.interestOps(SelectionKey.OP_READ);
               _selectionKey.selector().wakeup();
           }
       }
       catch (IOException ex) {
           ex.printStackTrace();
       }
   }
}
