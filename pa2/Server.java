import java.io.*;
import java.net.*;

public class Server {
  private Socket socket = null;
  private ServerSocket server = null;
  private DataInputStream in = null;
  private DataOutputStream out = null;

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

      // Server start process and waiting for client connection
      server = new ServerSocket(port);
      System.out.println("Server Started on " + InetAddress.getLocalHost().getHostAddress());
      System.out.println("Server file directory: " + dir.getAbsolutePath());
      System.out.println("Waiting for client...");

      socket = server.accept();
      System.out.println("Client Accepted.");

      // stream made for reading and sending
      in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      out = new DataOutputStream(socket.getOutputStream());

      out.writeUTF("Hello!");
      out.flush();

      String message = "";

      // loop to stay on the client connection until disconnection
      while (true) {
        try {
          // reading file name
          message = in.readUTF();
          System.out.println("Client: " + message);

          // shut down process
          if (message.equalsIgnoreCase("bye")) {
              out.writeUTF("BYE");
              out.writeUTF("disconnected");
              out.flush();
              break;
          }

          // look for the file requested and block path traversal attempts
          File requestedFile = new File(dir, message);
          if (!requestedFile.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
            out.writeUTF("ERROR");
            out.writeUTF("File not found");
            out.flush();
            continue;
          }
          if (!requestedFile.exists() || !requestedFile.isFile()) {
            out.writeUTF("ERROR");
            out.writeUTF("File not found");
            out.flush();
            continue;
          }

          // if the file exists then we start sending 
          FileInputStream fileIn = null;
          try {
            fileIn = new FileInputStream(requestedFile);

            // status that things went fine 
            out.writeUTF("OK");

            // byte size of file 
            out.writeLong(requestedFile.length());

            // sending it by chunks 
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
              out.write(buffer, 0, bytesRead);
            }
            out.flush();

            // finished sending 
            System.out.println("Sent file: " + requestedFile.getName() + " (" + requestedFile.length() + " bytes)");
          } catch (IOException e) {
            // log the error server-side only
            System.out.println("File transfer error: " + e);
            try { 
              socket.close(); 
            } catch (IOException ex) { /* ignore */ }
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

        } catch (IOException i) {
          System.out.println("Connect error: " + i);
          break;
        }
      }

      System.out.println("Server closing connection.");

      // closing all network resources 
      socket.close();
      in.close();
      out.close();
      server.close();

    } catch (IOException i) {
      System.out.println("Server error: "+ i);
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
