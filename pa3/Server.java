package pa3;
import java.io.*;
import java.net.*;
 
public class Server {
  private ServerSocket server = null;
 
  // Directory on the server where the BMP files are stored
  private static final String SERVER_DIR = "server_files";
 
  // # of bytes to send at a time while streaming the files 
  private static final int BUFFER_SIZE = 4096;
 
  public Server(int port) {
    // Network Connection
    try {
      // Double checking server folder exists
      File dir = new File(SERVER_DIR);
      if (!dir.exists()) {
        dir.mkdirs();
      }
 
      // Server start process — now accepts multiple clients in a loop
      server = new ServerSocket(port);
      System.out.println("Server Started on " + InetAddress.getLocalHost().getHostAddress());
      System.out.println("Server file directory: " + dir.getAbsolutePath());
      System.out.println("Waiting for clients...");
 
      // keep accepting new clients and hand each off to its own thread
      while (true) {
        Socket clientSocket = server.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
 
        // spawn a handler thread so this loop is free to accept the next client immediately
        ClientHandler handler = new ClientHandler(clientSocket, dir);
        Thread t = new Thread(handler);
        t.start();
      }
 
    } catch (IOException i) {
      System.out.println("Server error: " + i);
    }
  }
 
  // handles all communication with a single connected client
  private static class ClientHandler implements Runnable {
 
    private final Socket socket;
    private final File serverDir;
 
    public ClientHandler(Socket socket, File serverDir) {
      this.socket = socket;
      this.serverDir = serverDir;
    }
 
    @Override
    public void run() {
      DataInputStream in = null;
      DataOutputStream out = null;
 
      try {
        // stream made for reading and sending
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(socket.getOutputStream());
 
        out.writeUTF("Hello!");
        out.flush();
 
        String message = "";
 
        // loop to stay on the client connection until disconnection
        while (true) {
          try {
            // reading command from client
            message = in.readUTF();
            System.out.println("Client [" + socket.getInetAddress().getHostAddress() + "]: " + message);
 
            // shut down process
            if (message.equalsIgnoreCase("bye")) {
              out.writeUTF("BYE");
              out.writeUTF("disconnected");
              out.flush();
              break;
            }
 
            // handle the SEND command — build the file list and send the batch
            if (message.equalsIgnoreCase("send")) {
              handleSendBatch(in, out);
              continue;
            }
 
            // anything else is an unrecognized command
            out.writeUTF("ERROR");
            out.writeUTF("Please type a different command");
            out.flush();
 
          } catch (IOException i) {
            System.out.println("Client handler error: " + i);
            break;
          }
        }
 
      } catch (IOException i) {
        System.out.println("Connect error: " + i);
      } finally {
        // closing all resources for this client's thread
        try {
          if (in != null) in.close();
          if (out != null) out.close();
          if (socket != null) socket.close();
        } catch (IOException e) {
          System.out.println("Error closing client resources: " + e);
        }
        System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
      }
    }
 
    // reads the random file ordering from the client and sends the batch
    private void handleSendBatch(DataInputStream in, DataOutputStream out) throws IOException {
      // read how many files the client wants in this batch
      int batchSize = in.readInt();
 
      // read the random ordering array sent by the client
      int[] order = new int[batchSize];
      for (int i = 0; i < batchSize; i++) {
        order[i] = in.readInt();
      }
 
      // collect the file list from the server directory
      File[] allFiles = serverDir.listFiles(f -> f.isFile());
      if (allFiles == null || allFiles.length == 0) {
        out.writeUTF("ERROR");
        out.writeUTF("No files found on server");
        out.flush();
        return;
      }
 
      // signal that the batch is coming and tell the client how many files to expect
      out.writeUTF("OK");
      out.writeInt(batchSize);
      out.flush();
 
      // send each file in the order the client requested
      for (int idx : order) {
        // clamp the index to the available file list
        File fileToSend = allFiles[idx % allFiles.length];
 
        // send filename so the client can save it correctly
        out.writeUTF(fileToSend.getName());
        out.writeLong(fileToSend.length());
 
        FileInputStream fileIn = null;
        try {
          fileIn = new FileInputStream(fileToSend);
 
          byte[] buffer = new byte[BUFFER_SIZE];
          int bytesRead;
 
          while ((bytesRead = fileIn.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
          }
          out.flush();
 
          // finished sending 
          System.out.println("Sent file: " + fileToSend.getName() + " (" + fileToSend.length() + " bytes)");
 
        } catch (IOException e) {
          // log the error server-side only
          System.out.println("File transfer error: " + e);
          throw e;
        } finally {
          // closing the file stream 
          if (fileIn != null) {
            try {
              fileIn.close();
            } catch (IOException e) {
              System.out.println("Error closing file input stream: " + e);
            }
          }
        }
      }
    }
  }
 
  public static void main(String[] args) {
    int port = 5000;
 
    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    }
 
    new Server(port);
  }
}
 