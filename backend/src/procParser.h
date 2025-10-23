#pragma once

#include <stdint.h>
#include <stdlib.h>

typedef struct {
  int pid;
  uint64_t totalTime;
  uint64_t memRssKb;
  char processName[256];
  char state;
  int ppid;
  char userName[256];
} ProcessData;

ProcessData *scanProcDir(size_t *processCount);
char *formatProcessList(ProcessData *processList, size_t processCount);

char *getCpuUsage(void);
char *getMemUsage(void);
char *getNetUsage(void);
char *getDiskUsage(void);
