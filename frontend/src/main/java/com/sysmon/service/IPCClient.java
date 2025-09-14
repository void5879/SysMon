package com.sysmon.service;

import com.sysmon.model.ProcessInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IPCClient {

    private static final String SOCKET_PATH = "/tmp/SysMon";

    public ObservableList<ProcessInfo> getProcessList() {
        List<ProcessInfo> processList = new ArrayList<>();
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(SOCKET_PATH);

        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            System.out.println("Attempting to connect to: " + SOCKET_PATH);

            if (channel.connect(address)) {
                System.out.println("Connected successfully!");

                // Send command using ByteBuffer
                String command = "GET_PROCESSES\n";
                ByteBuffer writeBuffer = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
                channel.write(writeBuffer);
                System.out.println("Sent GET_PROCESSES command");

                // Read response using ByteBuffer
                ByteBuffer readBuffer = ByteBuffer.allocate(8192);
                StringBuilder response = new StringBuilder();

                while (channel.read(readBuffer) > 0) {
                    readBuffer.flip();
                    response.append(StandardCharsets.UTF_8.decode(readBuffer));
                    readBuffer.clear();

                    // Check if received the complete response
                    String responseStr = response.toString();
                    if (responseStr.contains("END_PROCESS_LIST")) {
                        break;
                    }
                }

                String responseStr = response.toString();
                System.out.println("Received response (" + responseStr.length() + " chars)");

                // Parse the response
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
                // Send the formatted KILL command
                String command = "KILL;" + pid + ";" + signal + "\n";
                ByteBuffer writeBuffer = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
                channel.write(writeBuffer);
                System.out.println("Sent command: " + command.trim());

                // Read response
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
