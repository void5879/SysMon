/*
 - Main C backend server implementation.
 - Creates and binds a UNIX domain socket at /tmp/SysMon.
 - Listens for and accepts client connections.
 - Handles multiple commands (e.g., GET_PROCESSES, GET_CPU_STATS, KILL)
   from a single client over a persistent connection loop.
 - Calls functions from procParser.h and terminator.h to get data
   or execute actions.
*/

#include "procParser.h"
#include "terminator.h"
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
    while (1) {
      char buffer[BUFFER_SIZE];
      ssize_t bytesRead = read(cfd, buffer, sizeof(buffer) - 1);
      if (bytesRead > 0) {
        buffer[bytesRead] = '\0';
        buffer[strcspn(buffer, "\r\n")] = 0;
        printf("Received command: '%s'\n", buffer);
        if (strcmp(buffer, "GET_PROCESSES") == 0) {
          printf("Processing GET_PROCESSES command...\n");
          size_t processCount;
          ProcessData *processList = scanProcDir(&processCount);
          if (processList && processCount > 0) {
            char *formattedString =
                formatProcessList(processList, processCount);
            if (formattedString) {
              write(cfd, formattedString, strlen(formattedString));
              free(formattedString);
            }
            free(processList);
          } else {
            const char *errorMsg = "BEGIN_PROCESS_LIST\nEND_PROCESS_LIST\n";
            write(cfd, errorMsg, strlen(errorMsg));
          }
        } else if (strncmp(buffer, "KILL", 4) == 0) {
          printf("Processing KILL command: %s\n", buffer);
          int pid, signal;
          if (sscanf(buffer, "KILL;%d;%d", &pid, &signal) == 2) {
            if (terminateProcess(pid, signal) == 0) {
              write(cfd, "OK\n", 3);
            } else {
              write(cfd, "ERROR;kill failed\n", 18);
            }
          } else {
            write(cfd, "ERROR;invalid kill format\n", 26);
          }
        } else if (strcmp(buffer, "GET_CPU_STATS") == 0) {
          char *cpuData = getCpuUsage();
          if (cpuData) {
            write(cfd, cpuData, strlen(cpuData));
            free(cpuData);
          } else {
            write(cfd, "ERROR;cpu\n", 10);
          }
        } else if (strcmp(buffer, "GET_MEM_STATS") == 0) {
          char *memData = getMemUsage();
          if (memData) {
            write(cfd, memData, strlen(memData));
            free(memData);
          } else {
            write(cfd, "ERROR;mem\n", 10);
          }
        } else if (strcmp(buffer, "GET_NET_STATS") == 0) {
          char *netData = getNetUsage();
          if (netData) {
            write(cfd, netData, strlen(netData));
            free(netData);
          } else {
            write(cfd, "ERROR;net\n", 10);
          }
        } else if (strcmp(buffer, "GET_DISK_STATS") == 0) {
          char *diskData = getDiskUsage();
          if (diskData) {
            write(cfd, diskData, strlen(diskData));
            free(diskData);
          } else {
            write(cfd, "ERROR;disk\n", 11);
          }
        } else {
          write(cfd, "ERROR;unknown command\n", 22);
        }
      } else if (bytesRead == 0) {
        printf("Client closed connection\n");
        break;
      } else {
        perror("read error");
        break;
      }
    }

    close(cfd);
    printf("Client disconnected.\n\n");
  }

  close(sfd);
  return 0;
}
