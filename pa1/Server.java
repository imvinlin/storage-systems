import java.io.*;
import java.net.*;

public class Server {
  private Socket socket = null;
  private ServerSocket server = null;
  private DataInputStream in = null;
  private DataOutputStream out = null;

  public Server(int port) {
    // Network Connection
    try {
      server = new ServerSocket(port);
      System.out.println("Server Started on " + InetAddress.getLocalHost().getHostAddress());
      System.out.println("Waiting for client...");

      socket = server.accept();
      System.out.println("Client Accepted.");

      in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      out = new DataOutputStream(socket.getOutputStream());

      out.writeUTF("Hello!");

      String message = "";

      while (true) {
        try {
          message = in.readUTF();
          System.out.println("Client: " + message);

          if (message.equalsIgnoreCase("bye")) {
              out.writeUTF("disconnected");
              break;
          }

          if (isAlpha(message)) {
              String result = message.toUpperCase();
              out.writeUTF(result);
          } else {
              out.writeUTF("Error: alphabets only. Try again.");
          }

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

  public static boolean isAlpha(String s) {
    if (s == null || s.isEmpty()) return false;
    return s.matches("[a-zA-Z]+");
  }

  public static void main(String[] args) {

    int port = 5000;

    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    }
    
    new Server(port);
  }
}
