package pa3;
import java.io.*;
import java.net.*;
import java.util.*;
 
public class Client {
 
  private Socket socket = null;
  private BufferedReader in = null;
  private DataOutputStream out = null;
  private DataInputStream serverIn = null;
 
  // Client side folder for receiving files 
  private static final String CLIENT_DIR = "client_downloads";
 
  // # of bytes to read at a time 
  private static final int BUFFER_SIZE = 4096;
 
  // total number of files available on the server
  private static final int TOTAL_FILES = 10;
 
  // number of rounds needed before statistics are printed
  private static final int STAT_INTERVAL = 5;
 
  public Client(String addr, int port) {
 
    // separate RTT lists per batch size so we can compare them
    ArrayList<Long> rttBatch1 = new ArrayList<>();
    ArrayList<Long> rttBatch2 = new ArrayList<>();
    ArrayList<Long> rttBatch3 = new ArrayList<>();
 
    try {
      // check the local file exists 
      File dir = new File(CLIENT_DIR);
      if (!dir.exists()) {
        dir.mkdirs();
      }
 
      // connection to the server 
      socket = new Socket(addr, port);
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
 
    Random rand = new Random();
    String message = "";
 
    while (true) {
      try {
        // ask user for a command (SEND, bye, or anything else)
        System.out.print("Enter command (SEND / bye): ");
        message = in.readLine();
 
        // shut down on bye
        if (message.equalsIgnoreCase("bye")) {
          out.writeUTF(message);
          out.flush();
 
          String status = serverIn.readUTF();
          if (status.equals("BYE")) {
            String res = serverIn.readUTF();
            System.out.println("Server: " + res);
            System.out.println("exit");
          }
          break;
        }
 
        // handle the SEND workflow — ask user which batch size to use
        if (message.equalsIgnoreCase("send")) {
          System.out.print("Enter batch size (1, 2, or 3): ");
          String batchInput = in.readLine();
          int batchSize;
          try {
            batchSize = Integer.parseInt(batchInput.trim());
            if (batchSize < 1 || batchSize > 3) {
              System.out.println("Batch size must be 1, 2, or 3.");
              continue;
            }
          } catch (NumberFormatException e) {
            System.out.println("Invalid batch size.");
            continue;
          }
 
          // generate a random ordering of file indices for this request
          int[] order = generateRandomOrder(rand, TOTAL_FILES, batchSize);
 
          // RTT start 
          long start = System.currentTimeMillis();
 
          // send the SEND command
          out.writeUTF("SEND");
          out.flush();
 
          // send the batch size and the random ordering to the server
          out.writeInt(batchSize);
          for (int idx : order) {
            out.writeInt(idx);
          }
          out.flush();
 
          // read status back from the server
          String status = serverIn.readUTF();
 
          if (status.equals("ERROR")) {
            String res = serverIn.readUTF();
            System.out.println("Server: " + res);
            continue;
          }
 
          if (status.equals("OK")) {
            // server confirms how many files it will send
            int filesIncoming = serverIn.readInt();
 
            // receive each file in the batch
            for (int i = 0; i < filesIncoming; i++) {
              // read the filename the server chose for this slot
              String fileName = serverIn.readUTF();
              long fileSize = serverIn.readLong();
              File outputFile = new File(CLIENT_DIR, fileName);
 
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
 
                  fileOut.write(buffer, 0, bytesRead);
                  remaining -= bytesRead;
                }
                fileOut.flush();
 
                if (remaining != 0) {
                  System.out.println("Warning: File transfer incomplete. " + remaining + " bytes missing.");
                }
 
                System.out.println("Received: " + fileName + " -> " + outputFile.getAbsolutePath());
 
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
            }
 
            long end = System.currentTimeMillis();
            long rtt = end - start;
 
            System.out.println("Server: File transfer done");
            System.out.println("RTT: " + rtt + " ms");
 
            // store RTT in the correct batch-size bucket
            if (batchSize == 1) {
              rttBatch1.add(rtt);
              // print stats every STAT_INTERVAL rounds
              if (rttBatch1.size() % STAT_INTERVAL == 0) {
                printStats("Batch Size 1", rttBatch1);
              }
            } else if (batchSize == 2) {
              rttBatch2.add(rtt);
              if (rttBatch2.size() % STAT_INTERVAL == 0) {
                printStats("Batch Size 2", rttBatch2);
              }
            } else if (batchSize == 3) {
              rttBatch3.add(rtt);
              if (rttBatch3.size() % STAT_INTERVAL == 0) {
                printStats("Batch Size 3", rttBatch3);
              }
            }
 
            continue;
          }
 
          System.out.println("Server sent unknown status: " + status);
          continue;
        }
 
        // anything else — send it and print the error the server returns
        out.writeUTF(message);
        out.flush();
 
        String status = serverIn.readUTF();
        if (status.equals("ERROR")) {
          String res = serverIn.readUTF();
          System.out.println("Server: " + res);
        } else {
          System.out.println("Server sent unknown status: " + status);
        }
 
      } catch (IOException i) {
        System.out.println("Error " + i);
        break;
      }
    }
 
    // final RTT summary for all batch sizes that were used
    if (!rttBatch1.isEmpty()) printStats("Batch Size 1 (Final)", rttBatch1);
    if (!rttBatch2.isEmpty()) printStats("Batch Size 2 (Final)", rttBatch2);
    if (!rttBatch3.isEmpty()) printStats("Batch Size 3 (Final)", rttBatch3);
 
    // closing client resources
    try {
      in.close();
      out.close();
      serverIn.close();
      socket.close();
    } catch (IOException i) {
      System.out.println(i);
    }
  }
 
  // builds a random sequence of `count` indices drawn from [0, total)
  // each index appears at most once to avoid sending the same file twice in a batch
  private int[] generateRandomOrder(Random rand, int total, int count) {
    List<Integer> pool = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      pool.add(i);
    }
    Collections.shuffle(pool, rand);
    int[] order = new int[count];
    for (int i = 0; i < count; i++) {
      order[i] = pool.get(i);
    }
    return order;
  }
 
  // prints min, mean, max and std dev for a list of RTT samples
  private void printStats(String label, ArrayList<Long> times) {
    long min = Collections.min(times);
    long max = Collections.max(times);
    double mean = times.stream().mapToLong(Long::longValue).average().getAsDouble();
 
    double variance = 0;
    for (long t : times) {
      variance += Math.pow(t - mean, 2);
    }
    variance /= times.size();
    double std = Math.sqrt(variance);
 
    System.out.println("\n---- RTT Statistics [" + label + "] ----");
    System.out.println("Trials: " + times.size());
    System.out.println("Min: " + min + " ms");
    System.out.println("Mean: " + mean + " ms");
    System.out.println("Max: " + max + " ms");
    System.out.println("Std Dev: " + std + " ms");
  }
 
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java Client <serverIP> [port]");
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
 