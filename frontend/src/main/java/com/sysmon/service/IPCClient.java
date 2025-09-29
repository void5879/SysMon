/*
  ██████╗ ██╗      ██╗ ███████╗ ███╗   ██╗ ████████╗
 ██╔════╝ ██║      ██║ ██╔════╝ ████╗  ██║ ╚══██╔══╝
 ██║      ██║      ██║ █████╗   ██╔██╗ ██║    ██║   
 ██║      ██║      ██║ ██╔══╝   ██║╚██╗██║    ██║   
 ╚██████╗ ███████╗ ██║ ███████╗ ██║ ╚████║    ██║   
  ╚═════╝ ╚══════╝ ╚═╝ ╚══════╝ ╚═╝  ╚═══╝    ╚═╝  
*/

/*
 - This class (acts as the client) handles the low-level communication with the C backend.
 - Responsible for the Inter-Process Communication (IPC) part of the application.
 - Connects to the backend over a UNIX domain socket.
 - Sends commands (GET_PROCESSES, KILL) and parses the raw text data received from the backend.  
*/
package com.sysmon.service;

// Javafx's data model.
import com.sysmon.model.ProcessInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// Classes used for the networking part.
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
// part of Java's Non-blocking I/O library.
import java.nio.ByteBuffer; // Container for raw binary data being sent or received.
import java.nio.channels.SocketChannel; // Represents the connection.
import java.nio.charset.StandardCharsets;
// Dynamic arrays.
import java.util.ArrayList;
import java.util.List;

public class IPCClient {
  private static final String SOCKET_PATH = "/tmp/SysMon"; // Path to the socket file.

  public ObservableList<ProcessInfo> getProcessList() {
    // Temporarily hold the parsed ProcessInfo objects.
    List<ProcessInfo> processList = new ArrayList<>();
    // Address object pointing to socket file.
    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(SOCKET_PATH);
    try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
      System.out.println("Attempting to connect to: " + SOCKET_PATH);
      if (channel.connect(address)) {
        System.out.println("Connected successfully!");
        String command = "GET_PROCESSES\n";
        // Command string is converted into sequence of bytes.
        ByteBuffer writeBuffer = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
        // Command sent to the backend.
        channel.write(writeBuffer);
        System.out.println("Sent GET_PROCESSES command");
        // Buffer of 8KB is allocated to receive data.
        ByteBuffer readBuffer = ByteBuffer.allocate(8192);
        StringBuilder response = new StringBuilder();
        while (channel.read(readBuffer) > 0) {
          // NOTE :ByteBuffer has internal pointers for writing and reading.
          // After writing data into the buffer with channel.read(),Must use flip() to
          // reset the position to the beginning so as to read the data just received.
          readBuffer.flip();
          // Bytes in the buffer are decoded back into a string and appended to a
          // StringBuilder.
          response.append(StandardCharsets.UTF_8.decode(readBuffer));
          // Buffer is cleared to prepare for next read operation.
          readBuffer.clear();
          String responseStr = response.toString();
          if (responseStr.contains("END_PROCESS_LIST")) {
            break;
          }
        }
        String responseStr = response.toString();
        System.out.println("Received response (" + responseStr.length() + " chars)");
        // response split to individual line.
        String[] lines = responseStr.split("\n");
        boolean foundBegin = false;
        int lineCount = 0;
        for (String line : lines) {
          lineCount++;
          line = line.trim();
          if (line.isEmpty())
            continue;
          System.out.println("Processing line " + lineCount + ": " + line);
          if ("BEGIN_PROCESS_LIST".equals(line)) {
            foundBegin = true;
            continue;
          }
          if ("END_PROCESS_LIST".equals(line)) {
            break;
          }
          if (foundBegin) {
            try {
              String[] parts = line.split("\t");
              System.out.println("Parsing line with " + parts.length + " parts: "
                  + java.util.Arrays.toString(parts));
              if (parts.length >= 5) {
                int pid = Integer.parseInt(parts[0].trim());
                int ppid = Integer.parseInt(parts[1].trim());
                String userName = parts[2].trim();
                String state = parts[3].trim();
                String processName = parts[4].trim();
                // Creates a processInfo object and adds it to the ArrayList.
                ProcessInfo processInfo = new ProcessInfo(pid, ppid, userName, state, processName);
                processList.add(processInfo);
                System.out.println("Added process: " + pid + " " + processName);
              } else {
                System.err.println("Invalid line format (expected 5 parts): " + line);
              }
            } catch (NumberFormatException e) {
              System.err.println("Error parsing numbers in line: " + line + " - " + e.getMessage());
            }
          }
        }
        System.out.println("Total processes parsed: " + processList.size());
      } else {
        System.err.println("Failed to connect to socket");
      }
    } catch (Exception e) {
      System.err.println("IPCClient Error: Could not connect to backend. Is it running?");
      e.printStackTrace();
    }
    return FXCollections.observableArrayList(processList);
  }

  public boolean killProcess(int pid, int signal) {
    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(SOCKET_PATH);
    try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
      if (channel.connect(address)) {
        String command = "KILL;" + pid + ";" + signal + "\n";
        ByteBuffer writeBuffer = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
        channel.write(writeBuffer);
        System.out.println("Sent command: " + command.trim());
        ByteBuffer readBuffer = ByteBuffer.allocate(256);
        channel.read(readBuffer);
        readBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(readBuffer).toString().trim();
        System.out.println("Kill response: " + response);
        return response.startsWith("OK");
      }
    } catch (Exception e) {
      System.err.println("Error in killProcess: " + e.getMessage());
      e.printStackTrace();
    }
    return false;
  }
}
