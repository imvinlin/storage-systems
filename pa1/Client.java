import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

  private Socket socket = null;
  private BufferedReader in = null;
  private DataOutputStream out = null;
  private DataInputStream serverIn = null;

  public Client(String addr, int port) {

    ArrayList<Long> roundTripTimes = new ArrayList<>();

    try {
      socket = new Socket(addr,port);
      System.out.println("Connected to server.");

      // in = new DataInputStreamn(System.in);
      in = new BufferedReader(new InputStreamReader(System.in));
      out = new DataOutputStream(socket.getOutputStream());
      serverIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

      String greeting = serverIn.readUTF();
      System.out.println("Server: " + greeting);

    } catch (UnknownHostException u) {
      System.out.println("Unknown Host: " + u);
      return;
    } catch (IOException i) {
      System.out.println("Connection error: " + i);
      return;
    }

    String message = "";

    while (true) {
      try {
        System.out.print("Enter string: ");
        message = in.readLine();

        long start = System.currentTimeMillis();

        out.writeUTF(message);

        String response = serverIn.readUTF();

        long end = System.currentTimeMillis();
        long rtt = end - start;

        System.out.println("Server: " + response);

        if (!message.equalsIgnoreCase("bye")) {
          roundTripTimes.add(rtt);
          System.out.println("RTT: " + rtt + " ms");
        }

        if (response.equals("disconnected")) {
          System.out.println("exit");
          break;
        }

      } catch (IOException i) {
        System.out.println("Error " + i);
        break;
      }
    }

    if (roundTripTimes.size() > 0) {

      long min = Collections.min(roundTripTimes);
      long max = Collections.max(roundTripTimes);

      double mean =
              roundTripTimes.stream().mapToLong(Long::longValue).average().getAsDouble();

      double variance = 0;
      for (long t : roundTripTimes) {
          variance += Math.pow(t - mean, 2);
      }
      variance /= roundTripTimes.size();

      double std = Math.sqrt(variance);

      System.out.println("\n---- RTT Statistics ----");
      System.out.println("Trials: " + roundTripTimes.size());
      System.out.println("Min: " + min + " ms");
      System.out.println("Mean: " + mean + " ms");
      System.out.println("Max: " + max + " ms");
      System.out.println("Std Dev: " + std + " ms");
    }


    try {
      in.close();
      out.close();
      serverIn.close();
      socket.close();
    } catch (IOException i) {
      System.out.println(i);
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java Client <serverIP [port]");
      return;
    } 

    String serverIp = args[0];
    int port = 5000;

    if (args.length == 2) {
      port = Integer.parseInt(args[1]);
    }
    new Client(serverIp, port);
  }
}
