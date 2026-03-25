import java.io.*;
import java.net.*;

public class Server {
  private Socket socket = null;
  private ServerSocket server = null;
  private DataInputStream in = null;
  private DataOutputStream out = null;

  private static final String SERVER_DIR = "server_files";

  public Server(int port) {
    // Network Connection
    try {
      File dir = new File(SERVER_DIR);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      server = new ServerSocket(port);
      System.out.println("Server Started on " + InetAddress.getLocalHost().getHostAddress());
      System.out.println("Server file directory: " + dir.getAbsolutePath());
      System.out.println("Waiting for client...");

      socket = server.accept();
      System.out.println("Client Accepted.");

      in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      out = new DataOutputStream(socket.getOutputStream());

      out.writeUTF("Hello!");
      out.flush();

      String message = "";

      while (true) {
        try {
          message = in.readUTF();
          System.out.println("Client: " + message);

          if (message.equalsIgnoreCase("bye")) {
              out.writeUTF("disconnected");
              out.flush();
              break;
          }

          File requestedFile = new File(dir, message);

          if (requestedFile.exists() && requestedFile.isFile()) {
            out.writeUTF("FOUND");
          } else {
            out.writeUTF("File not found");
          }
          out.flush();

        } catch (IOException i) {
          System.out.println("Connect error: " + i);
          break;
        }
      }

      System.out.println("Server closing connection.");

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
