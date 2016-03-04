package com.importsource.pattern.reactor;
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