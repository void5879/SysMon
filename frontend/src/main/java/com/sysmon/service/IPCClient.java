/*
 - IPC CLIENT:
 - Handles all Inter-Process Communication (IPC) with the C backend.
 - This class connects to the UNIX domain socket using Java NIO.
 - It sends commands (e.g., "GET_PROCESSES", "GET_CPU_STATS", "KILL") and
   parses the raw text responses into the correct Java data models
   (ProcessInfo, SystemUpdate).
*/

package com.sysmon.service;

import com.sysmon.model.ProcessInfo;
import com.sysmon.model.SystemUpdate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IPCClient {
  private static final String SOCKET_PATH = "/tmp/SysMon";

  private SocketChannel connect() throws IOException {
    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(SOCKET_PATH);
    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
    if (channel.connect(address)) {
      return channel;
    } else {
      throw new IOException("Failed to connect to socket at " + SOCKET_PATH);
    }
  }

  private String sendCommand(SocketChannel channel, String command) throws IOException {
    ByteBuffer writeBuffer = ByteBuffer.wrap((command + "\n").getBytes(StandardCharsets.UTF_8));
    channel.write(writeBuffer);
    ByteBuffer readBuffer = ByteBuffer.allocate(4096);
    channel.read(readBuffer);
    readBuffer.flip();
    return StandardCharsets.UTF_8.decode(readBuffer).toString().trim();
  }

  public ObservableList<ProcessInfo> getProcessList() {
    List<ProcessInfo> processList = new ArrayList<>();
    try (SocketChannel channel = connect()) {
      String command = "GET_PROCESSES\n";
      ByteBuffer writeBuffer = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
      channel.write(writeBuffer);
      ByteBuffer readBuffer = ByteBuffer.allocate(131072); // 128KB buffer
      StringBuilder response = new StringBuilder();
      while (channel.read(readBuffer) > 0) {
        readBuffer.flip();
        response.append(StandardCharsets.UTF_8.decode(readBuffer));
        readBuffer.clear();
        if (response.toString().contains("END_PROCESS_LIST")) {
          break;
        }
      }
      String responseStr = response.toString();
      String[] lines = responseStr.split("\n");
      boolean foundBegin = false;
      for (String line : lines) {
        line = line.trim();
        if (line.isEmpty())
          continue;
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
            if (parts.length >= 7) {
              int pid = Integer.parseInt(parts[0].trim());
              int ppid = Integer.parseInt(parts[1].trim());
              String userName = parts[2].trim();
              String state = parts[3].trim();
              String processName = parts[4].trim();
              long totalTime = Long.parseLong(parts[5].trim());
              long memRssKb = Long.parseLong(parts[6].trim());
              ProcessInfo processInfo = new ProcessInfo(pid, ppid, userName, state, processName, totalTime, memRssKb);
              processList.add(processInfo);
            } else {
              System.err.println("Invalid line format (expected 7 parts): " + line);
            }
          } catch (NumberFormatException e) {
            System.err.println("Error parsing numbers in line: " + line + " - " + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      System.err.println("IPCClient Error (getProcessList): " + e.getMessage());
      e.printStackTrace();
    }
    return FXCollections.observableArrayList(processList);
  }

  public SystemUpdate getSystemUpdate() {
    SystemUpdate stats = new SystemUpdate();
    try (SocketChannel channel = connect()) {
      String cpuResponse = sendCommand(channel, "GET_CPU_STATS");
      if (cpuResponse.startsWith("CPU;")) {
        String[] parts = cpuResponse.split(";");
        if (parts.length >= 3) {
          stats.setCpuUsage(Double.parseDouble(parts[1]));
          stats.setSystemTotalTime(Long.parseLong(parts[2]));
        }
      }
      String memResponse = sendCommand(channel, "GET_MEM_STATS");
      for (String line : memResponse.split("\n")) {
        String[] parts = line.split(";");
        if (parts.length < 2)
          continue;
        String key = parts[0];
        long value = Long.parseLong(parts[1]);
        switch (key) {
          case "MEM_TOTAL":
            stats.setMemTotal(value);
            break;
          case "MEM_FREE":
            stats.setMemFree(value);
            break;
          case "MEM_AVAIL":
            stats.setMemAvailable(value);
            break;
          case "BUFFERS":
            stats.setBuffers(value);
            break;
          case "CACHED":
            stats.setCached(value);
            break;
          case "SWAP_TOTAL":
            stats.setSwapTotal(value);
            break;
          case "SWAP_FREE":
            stats.setSwapFree(value);
            break;
        }
      }
      String netResponse = sendCommand(channel, "GET_NET_STATS");
      if (netResponse.startsWith("NET;")) {
        String[] parts = netResponse.split(";");
        if (parts.length >= 3) {
          stats.setNetDownSpeed(Long.parseLong(parts[1]));
          stats.setNetUpSpeed(Long.parseLong(parts[2]));
        }
      }
      String diskResponse = sendCommand(channel, "GET_DISK_STATS");
      if (diskResponse.startsWith("DISK;")) {
        String[] parts = diskResponse.split(";");
        if (parts.length >= 3) {
          stats.setDiskUsed(Long.parseLong(parts[1]));
          stats.setDiskTotal(Long.parseLong(parts[2]));
        }
      }

    } catch (Exception e) {
      System.err.println("Failed to get system update: " + e.getMessage());
      e.printStackTrace();
    }
    return stats;
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
