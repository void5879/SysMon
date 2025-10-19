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
    perror("Faile to open /proc/stat");
    return NULL;
  }
  char line[512];
  fgets(line, sizeof(line), file);
  uint64_t user;
  uint64_t nice;
  uint64_t system;
  uint64_t currentIdle;
  uint64_t iowait;
  uint64_t irq;
  uint64_t softirq;
  sscanf(line, "%lu %lu %lu %lu %lu %lu %lu", &user, &nice, &system,
         &currentIdle, &iowait, &irq, &softirq);
  uint64_t currentTotal =
      user + nice + system + currentIdle + iowait + irq + softirq;
  if (!prevTotal) {
    prevTotal = currentTotal;
    prevIdle = currentIdle;
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
  return out;
}
