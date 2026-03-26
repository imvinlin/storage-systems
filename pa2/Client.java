import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

  private Socket socket = null;
  private BufferedReader in = null;
  private DataOutputStream out = null;
  private DataInputStream serverIn = null;

  // Client side folder for receving files 
  private static final String CLIENT_DIR = "client_downloads";

  // # of bytes to read at a time 
  private static final int BUFFER_SIZE = 4096;

  public Client(String addr, int port) {

    ArrayList<Long> roundTripTimes = new ArrayList<>();

    try {
      // check the local file exists 
      File dir = new File(CLIENT_DIR);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      // connection to the server 
      socket = new Socket(addr,port);
      System.out.println("Connected to server.");

      // data streams needed for reading and sending 
      in = new BufferedReader(new InputStreamReader(System.in));
      out = new DataOutputStream(socket.getOutputStream());
      serverIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

      // greeting from the server 
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
        // asking for the file name 
        System.out.print("Enter file name: ");
        message = in.readLine();

        // RTT start 
        long start = System.currentTimeMillis();

        // sending filename to the server 
        out.writeUTF(message);
        out.flush();

        // reading status
        String status = serverIn.readUTF();
        
        // shutdown process 
        if (status.equals("BYE")) {
          String res = serverIn.readUTF();

          long end = System.currentTimeMillis();
          long rtt = end - start;

          System.out.println("Server: " + res);
          System.out.println("exit");
          break;
        }

        // Error cases like missing files 
        if (status.equals("ERROR")) {
          String res = serverIn.readUTF();

          long end = System.currentTimeMillis();
          long rtt = end - start;

          System.out.println("Server: " + res);
          //only count rtt time if the message isn't an exit message or an incorrect file name warning 
          if (!message.equalsIgnoreCase("bye") && !res.equals("File not found")) {
            System.out.println("RTT: " + rtt + " ms");
            roundTripTimes.add(rtt);
          }

          continue;
        }

        if (status.equals("OK")) {
          // file size and making sure file path is there 
          long fileSize = serverIn.readLong();
          File outputFile = new File(CLIENT_DIR, message);

          // Output stream to receive the file 
          FileOutputStream fileOut = null;
          try {
            fileOut = new FileOutputStream(outputFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = fileSize;

            while (remaining > 0) {
              // clamping to not exceed bytes needed to read 
              int bytesToRead = (int) Math.min(BUFFER_SIZE, remaining);
              int bytesRead = serverIn.read(buffer, 0, bytesToRead);

              if (bytesRead == -1) {
                throw new IOException("Connection closed during the file transfer");
              }

              fileOut.write(buffer,0,bytesRead);
              remaining -= bytesRead;
            }
            fileOut.flush();

          } finally {
            // closing the output stream 
            if (fileOut != null) {
              try {
                fileOut.close();
              } catch (IOException e) {
                System.out.println("Error closing file stream: " + e);
              }
            }
          }

          long end = System.currentTimeMillis();
          long rtt = end - start;

          System.out.println("Server: File transfer done");
          System.out.println("Saved file to: " + outputFile.getAbsolutePath());
          System.out.println("RTT: " + rtt + " ms");

          if (!message.equalsIgnoreCase("bye")) {
            roundTripTimes.add(rtt);
          }

          continue;
        }

        System.out.println("Server sent unknown status: " + status);
      } catch (IOException i) {
        System.out.println("Error " + i);
        break;
      }
    }

    // RTT summary
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

    // clsoing client resources
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
