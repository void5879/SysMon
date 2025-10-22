// Copyright (c) 2025 void5879. All Rights Reserved.
/*
 ███████╗ ███████╗ ██████╗  ██╗   ██╗ ███████╗ ██████╗
 ██╔════╝ ██╔════╝ ██╔══██╗ ██║   ██║ ██╔════╝ ██╔══██╗
 ███████╗ █████╗   ██████╔╝ ██║   ██║ █████╗   ██████╔╝
 ╚════██║ ██╔══╝   ██╔══██╗ ╚██╗ ██╔╝ ██╔══╝   ██╔══██╗
 ███████║ ███████╗ ██║  ██║  ╚████╔╝  ███████╗ ██║  ██║
 ╚══════╝ ╚══════╝ ╚═╝  ╚═╝   ╚═══╝   ╚══════╝ ╚═╝  ╚═╝

 - Implementation of the server.
 - Utilizes Stream sockets to ensure the data is sent and received.
*/
#include "procParser.h"
#include "terminator.h"
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

#define SOCKET_PATH "/tmp/SysMon"
#define BUFFER_SIZE 1024
#define LISTEN_BACKLOG 10

int main(void) {
  int sfd, cfd;
  struct sockaddr_un address;

  sfd = socket(AF_UNIX, SOCK_STREAM, 0);
  if (sfd == -1) {
    perror("socket failed");
    exit(EXIT_FAILURE);
  }

  unlink(SOCKET_PATH);

  memset(&address, 0, sizeof(struct sockaddr_un));
  address.sun_family = AF_UNIX;
  strncpy(address.sun_path, SOCKET_PATH, sizeof(address.sun_path) - 1);

  if (bind(sfd, (struct sockaddr *)&address, sizeof(struct sockaddr_un)) ==
      -1) {
    perror("bind failed");
    close(sfd);
    exit(EXIT_FAILURE);
  }

  if (listen(sfd, LISTEN_BACKLOG) == -1) {
    perror("listen failed");
    close(sfd);
    exit(EXIT_FAILURE);
  }

  printf("Server is listening on %s\n", SOCKET_PATH);

  for (;;) {
    cfd = accept(sfd, NULL, NULL);
    if (cfd == -1) {
      perror("accept failed");
      continue;
    }

    printf("Client connected.\n");
    char buffer[BUFFER_SIZE];
    ssize_t bytesRead = read(cfd, buffer, sizeof(buffer) - 1);

    if (bytesRead > 0) {
      buffer[bytesRead] = '\0';
      buffer[strcspn(buffer, "\r\n")] = 0;

      printf("Received command: '%s' (%zd bytes)\n", buffer, bytesRead);

      if (strcmp(buffer, "GET_PROCESSES") == 0) {
        printf("Processing GET_PROCESSES command...\n");
        size_t processCount;
        ProcessData *processList = scanProcDir(&processCount);

        if (processList && processCount > 0) {
          char *formattedString = formatProcessList(processList, processCount);
          if (formattedString) {
            size_t responseLen = strlen(formattedString);
            ssize_t bytesWritten = write(cfd, formattedString, responseLen);
            printf("Sent %zd bytes to client (process count: %zu)\n",
                   bytesWritten, processCount);
            free(formattedString);
          } else {
            const char *errorMsg = "ERROR;Failed to format process list\n";
            write(cfd, errorMsg, strlen(errorMsg));
            printf("Error: Failed to format process list\n");
          }
          free(processList);
        } else {
          const char *errorMsg = "ERROR;No processes found or scan failed\n";
          write(cfd, errorMsg, strlen(errorMsg));
          printf("Error: No processes found or scan failed\n");
        }

      } else if (strncmp(buffer, "KILL", 4) == 0) {
        printf("Processing KILL command: %s\n", buffer);
        int pid, signal;
        if (sscanf(buffer, "KILL;%d;%d", &pid, &signal) == 2) {
          printf("Attempting to kill PID %d with signal %d\n", pid, signal);
          if (terminateProcess(pid, signal) == 0) {
            write(cfd, "OK\n", 3);
            printf("Successfully sent signal %d to PID %d\n", signal, pid);
          } else {
            write(cfd, "ERROR;kill failed\n", 18);
            printf("Failed to send signal %d to PID %d\n", signal, pid);
          }
        } else {
          write(cfd, "ERROR;invalid kill command format\n", 35);
          printf("Error: Invalid kill command format: %s\n", buffer);
        }
      } else if (strcmp(buffer, "GET_CPU_STATS") == 0) {
        printf("Processing GET_CPU_STATS command...\n");
        char *cpuData = getCpuUsage();
        if (cpuData) {
          write(cfd, cpuData, strlen(cpuData));
          free(cpuData);
        } else {
          const char *errorMsg = "ERROR;Failed to get CPU stats\n";
          write(cfd, errorMsg, strlen(errorMsg));
          printf("Error: getCpuUsage returned NULL\n");
        }

      } else if (strcmp(buffer, "GET_MEM_STATS") == 0) {
        printf("Processing GET_MEM_STATS command...\n");
        char *memData = getMemUsage();
        if (memData) {
          write(cfd, memData, strlen(memData));
          free(memData);
        } else {
          const char *errorMsg = "ERROR;Failed to get MEM stats\n";
          write(cfd, errorMsg, strlen(errorMsg));
          printf("Error: getMemUsage returned NULL\n");
        }

      } else if (strcmp(buffer, "GET_NET_STATS") == 0) {
        printf("Processing GET_NET_STATS command...\n");
        char *netData = getNetUsage();
        if (netData) {
          write(cfd, netData, strlen(netData));
          free(netData);
        } else {
          const char *errorMsg = "ERROR;Failed to get NET stats\n";
          write(cfd, errorMsg, strlen(errorMsg));
          printf("Error: getNetUsage returned NULL\n");
        }

      } else if (strcmp(buffer, "GET_DISK_STATS") == 0) {
        printf("Processing GET_DISK_STATS command...\n");
        char *diskData = getDiskUsage();
        if (diskData) {
          write(cfd, diskData, strlen(diskData));
          free(diskData);
        } else {
          const char *errorMsg = "ERROR;Failed to get DISK stats\n";
          write(cfd, errorMsg, strlen(errorMsg));
          printf("Error: getDiskUsage returned NULL\n");
        }
      } else {
        write(cfd, "ERROR;unknown command\n", 22);
        printf("Error: Unknown command: %s\n", buffer);
      }
    } else if (bytesRead == 0) {
      printf("Client closed connection\n");
    } else {
      printf("Read error: %s\n", strerror(errno));
    }

    close(cfd);
    printf("Client disconnected.\n\n");
  }

  close(sfd);
  return 0;
}
