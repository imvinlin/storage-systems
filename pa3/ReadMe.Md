# PA3 – Multi-threaded TCP File Transfer Client/Server (Java)

Builds on PA2's and PA1's TCP client/server by supporting batch file transfer with multithreading.
The client requests BMP files by name; the server validates and streams them back.
RTT is measured per transfer and statistics are reported at session end.

Course: CIS4930 – Internet Storage Systems

---

## Project Files

- Server.java – TCP server with file transfer
- Client.java – TCP client with file receiving and RTT measurement
- Makefile – Compilation and setup
- server_files/ – Place the 10 sample BMP files here before running
- client_downloads/ – Received files are saved here automatically

---

## Requirements

- Java JDK 21 or newer
- Two computers on the same network
- sample01.bmp through sample10.bmp placed in server_files/

---

## Setup & Compilation
```
make setup   # creates directories and copies BMP files into server_files/
make         # compiles Server.java and Client.java
```

---

## Running

### On Computer A (Server)
Get your local IP:
- macOS: `ipconfig getifaddr en0`
- Linux: `ip addr show`
- Windows: `ipconfig`

Start the server:
```
java Server <port_number>
```

### On Computer B (Client)
```
java Client <server_ip> <port_number>
```

---

## Usage

- Enter SEND to send files towards the client. 
- After SEND, enter 1, 2, or 3 batch files to send.
- The file will be saved to `client_downloads/`
- RTT is printed after each successful transfer
- Type `bye` to disconnect
- RTT statistics (min, mean, max, std dev) are tracked for each batch size, and printed every 5 trials, with a final summary when disconnecting.

---

## Notes

- The server must be started before the client
- Files must exist in `server_files/` on the server machine
- RTT includes local disk write time in addition to network latency
- The server can now handle multiple client session per run