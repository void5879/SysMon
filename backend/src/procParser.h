#pragma once

#include <stdlib.h>

typedef struct {
  int pid;
  char processName[256];
  char state;
  int ppid;
  char userName[256];
} ProcessData;

ProcessData *scanProcDir(size_t *processCount);
char *formatProcessList(ProcessData *processList, size_t processCount);
