// Copyright (c) 2025 void5879. All Rights Reserved.
/*
 ██████╗   █████╗   ██████╗ ██╗  ██╗ ███████╗ ███╗   ██╗ ██████╗
 ██╔══██╗ ██╔══██╗ ██╔════╝ ██║ ██╔╝ ██╔════╝ ████╗  ██║ ██╔══██╗
 ██████╔╝ ███████║ ██║      █████╔╝  █████╗   ██╔██╗ ██║ ██║  ██║
 ██╔══██╗ ██╔══██║ ██║      ██╔═██╗  ██╔══╝   ██║╚██╗██║ ██║  ██║
 ██████╔╝ ██║  ██║ ╚██████╗ ██║  ██╗ ███████╗ ██║ ╚████║ ██████╔╝
 ╚═════╝  ╚═╝  ╚═╝  ╚═════╝ ╚═╝  ╚═╝ ╚══════╝ ╚═╝  ╚═══╝ ╚═════╝

 - This file provides the implementation details for the backend functions
 employed by the appliation.
 - readProcStat: reads the stat file in the proc dir and extracts the pid, ppid,
 state and processName.
 - readProcStatus: reads the status file in the proc dir and extracts the uid to
 get the userName.
 - scanProcDir: uses the readProcStat and readProcStatus to get and return the
 ongoing list of processess.
 - formatProcessList: formats the list returned by the scanProcDir so as to send
 it to the client.
*/
#include "procParser.h"
#include <ctype.h>
#include <dirent.h>
#include <pwd.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/statvfs.h>
#include <sys/types.h>

static void readProcStat(const char *filepath, ProcessData *p) {
  p->pid = 0;
  FILE *file = fopen(filepath, "r");
  if (file == NULL) {
    return;
  }

  char buffer[4096];

  if (!fgets(buffer, sizeof(buffer), file)) {
    fclose(file);
    return;
  }
  fclose(file);

  p->pid = atoi(buffer);

  char *leftParen = strchr(buffer, '(');
  char *rightParen = strrchr(buffer, ')');
  if (!leftParen || !rightParen || rightParen < leftParen) {
    p->pid = 0;
    return;
  }
  size_t len = rightParen - leftParen - 1;
  if (len >= sizeof(p->processName)) {
    len = sizeof(p->processName) - 1;
  }
  strncpy(p->processName, leftParen + 1, len);
  p->processName[len] = '\0';
  if (rightParen != NULL) {
    sscanf(rightParen + 2, "%c %d", &p->state, &p->ppid);
  }
}

static void readProcStatus(const char *filepath, ProcessData *p) {
  uid_t uid = -1;
  FILE *file = fopen(filepath, "r");
  if (file == NULL) {
    return;
  }

  char buffer[256];

  while (fgets(buffer, sizeof(buffer), file)) {
    if (strncmp(buffer, "Uid:", 4) == 0) {
      sscanf(buffer, "Uid:\t%u", &uid);
      break;
    }
  }
  fclose(file);

  struct passwd *userInfo = getpwuid(uid);
  if (userInfo != NULL) {
    strcpy(p->userName, userInfo->pw_name);
  } else {
    snprintf(p->userName, sizeof(p->userName), "%u", uid);
  }
}

ProcessData *scanProcDir(size_t *processCount) {
  size_t capacity = 256;
  *processCount = 0;

  ProcessData *processList = malloc(capacity * sizeof(ProcessData));
  if (processList == NULL) {
    perror("Failed to allocate initial memory");
    return NULL;
  }

  DIR *procDir = opendir("/proc");
  if (procDir == NULL) {
    perror("Failed to open /proc");
    free(processList);
    return NULL;
  }

  struct dirent *entry;
  while ((entry = readdir(procDir)) != NULL) {
    if (isdigit(entry->d_name[0])) {
      if (*processCount == capacity) {
        capacity *= 2;
        ProcessData *newList =
            realloc(processList, capacity * sizeof(ProcessData));
        if (newList == NULL) {
          perror("Failed to reallocate memory");
          closedir(procDir);
          free(processList);
          return NULL;
        }
        processList = newList;
      }

      ProcessData *p = &processList[*processCount];
      char statFilepath[512];
      snprintf(statFilepath, sizeof(statFilepath), "/proc/%s/stat",
               entry->d_name);
      readProcStat(statFilepath, p);

      if (p->pid > 0) {
        char statusFilepath[512];
        snprintf(statusFilepath, sizeof(statusFilepath), "/proc/%s/status",
                 entry->d_name);
        readProcStatus(statusFilepath, p);
        (*processCount)++;
      }
    }
  }

  closedir(procDir);
  return processList;
}

char *formatProcessList(ProcessData *processList, size_t processCount) {
  size_t capacity = 4096;
  char *response = malloc(capacity);
  if (response == NULL) {
    return NULL;
  }

  strcpy(response, "BEGIN_PROCESS_LIST\n");
  size_t currentLen = strlen(response);

  for (size_t i = 0; i < processCount; i++) {
    char lineBuffer[1024];
    ProcessData *p = &processList[i];

    int lineLen =
        snprintf(lineBuffer, sizeof(lineBuffer), "%d\t%d\t%s\t%c\t%s\n", p->pid,
                 p->ppid, p->userName, p->state, p->processName);

    if (currentLen + lineLen >= capacity) {
      capacity *= 2;
      char *newResponse = realloc(response, capacity);
      if (newResponse == NULL) {
        free(response);
        return NULL;
      }
      response = newResponse;
    }

    strcat(response, lineBuffer);
    currentLen += lineLen;
  }

  const char *footer = "END_PROCESS_LIST\n";
  if (currentLen + strlen(footer) >= capacity) {
    capacity += strlen(footer) + 1;
    char *newResponse = realloc(response, capacity);
    if (newResponse == NULL) {
      free(response);
      return NULL;
    }
    response = newResponse;
  }
  strcat(response, footer);

  return response;
}

static uint64_t prevTotal = 0;
static uint64_t prevIdle = 0;

char *getCpuUsage(void) {
  FILE *file = fopen("/proc/stat", "r");
  if (!file) {
    perror("Failed to open /proc/stat");
    return NULL;
  }
  char line[512];
  fgets(line, sizeof(line), file);
  uint64_t user = 0;
  uint64_t nice = 0;
  uint64_t system = 0;
  uint64_t currentIdle = 0;
  uint64_t iowait = 0;
  uint64_t irq = 0;
  uint64_t softirq = 0;
  sscanf(line, "%lu %lu %lu %lu %lu %lu %lu", &user, &nice, &system,
         &currentIdle, &iowait, &irq, &softirq);
  uint64_t currentTotal =
      user + nice + system + currentIdle + iowait + irq + softirq;
  if (!prevTotal) {
    prevTotal = currentTotal;
    prevIdle = currentIdle;
    fclose(file);
    return "CPU;0.0\n";
  }
  uint64_t deltaTotal = currentTotal - prevTotal;
  uint64_t deltaIdle = currentIdle - prevIdle;
  double usagePercent =
      (1.0 - ((double)deltaIdle / (double)deltaTotal)) * 100.0;
  prevTotal = currentTotal;
  prevIdle = currentIdle;
  char *out = malloc(50 * sizeof(char));
  snprintf(out, sizeof(out), "CPU;%.1f\n", usagePercent);
  fclose(file);
  return out;
}

char *getMemUsage(void) {
  FILE *file = fopen("/proc/meminfo", "r");
  if (!file) {
    perror("Failed to open /proc/meminfo");
    return NULL;
  }
  char line[100];
  uint64_t memTotal = 0;
  uint64_t memFree = 0;
  uint64_t memAvail = 0;
  uint64_t buffers = 0;
  uint64_t cached = 0;
  uint64_t swapTotal = 0;
  uint64_t swapFree = 0;
  int found = 0;
  while ((fgets(line, sizeof(line), file)) && found < 7) {
    found += (sscanf(line, "MemTotal: %lu kB", &memTotal) == 1);
    found += (sscanf(line, "MemFree: %lu kB", &memFree) == 1);
    found += (sscanf(line, "MemAvailable: %lu kB", &memAvail) == 1);
    found += (sscanf(line, "Buffers: %lu kB", &buffers) == 1);
    found += (sscanf(line, "Cached: %lu kB", &cached) == 1);
    found += (sscanf(line, "SwapTotal: %lu kB", &swapTotal) == 1);
    found += (sscanf(line, "SwapFree: %lu kB", &swapFree) == 1);
  }
  char *out = malloc(512 * sizeof(char));
  snprintf(out, sizeof(out),
           "MEM_TOTAL;%lu\nMEM_FREE;%lu\nMEM_AVAIL;%lu\nBUFFERS;%lu\nCACHED;%"
           "lu\nSWAP_TOTAL;%lu\nSWAP_FREE;%lu\n",
           memTotal, memFree, memAvail, buffers, cached, swapTotal, swapFree);
  fclose(file);
  return out;
}

static uint64_t prevRecBytes = 0;
static uint64_t prevTxBytes = 0;

char *getNetUsage(void) {
  FILE *file = fopen("/proc/net/dev", "r");
  if (!file) {
    perror("Failed to open /proc/net/dev");
    return NULL;
  }
  char line[512];
  int i = 0;
  while (i < 2 && fgets(line, sizeof(line), file) != NULL) {
    ++i;
  }
  uint64_t currentTotalRecv = 0;
  uint64_t currentTotalTx = 0;
  uint64_t recBytes = 0;
  uint64_t txBytes = 0;
  fgets(line, sizeof(line), file);
  sscanf(line, "%*s %lu %*s %*s %*s %*s %*s %*s %*s %lu", &recBytes, &txBytes);
  currentTotalRecv += recBytes;
  currentTotalTx += txBytes;
  if (!prevRecBytes) {
    prevRecBytes = currentTotalRecv;
    prevTxBytes = currentTotalTx;
    fclose(file);
    return "NET;0.0\n";
  }
  uint64_t downSpeedBytes = currentTotalRecv - prevRecBytes;
  uint64_t upSpeedBytes = currentTotalTx - prevTxBytes;
  prevRecBytes = currentTotalRecv;
  prevTxBytes = currentTotalTx;
  char *out = malloc(50 * sizeof(char));
  snprintf(out, sizeof(out), "NET;%lu;%lu;\n", downSpeedBytes, upSpeedBytes);
  fclose(file);
  return out;
}

char *getDiskUsage(void) {
  struct statvfs diskStats;
  if ((statvfs("/", &diskStats) == 0) {
    uint64_t blockSize = diskStats.f_frsize;
    uint64_t totalBlocks = diskStats.f_blocks;
    uint64_t freeBlockForUser = diskStats.f_bavail;
    uint64_t totalSpace = totalBlocks * blockSize;
    uint64_t availSpace = freeBlockForUser * blockSize;
    uint64_t usedSpace = totalSpace - availSpace;
    char *out = malloc(50 * sizeof(char));
    snprintf(out, sizeof(out), "DISK;%lu;%lu\n", usedSpace, totalSpace);
    return out;
  } else {
    perror("Failed to call statvfs");
    return NULL;
  }
}
