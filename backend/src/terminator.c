/*
 ███████╗ ███╗   ██╗ ██████╗      ████████╗  █████╗  ███████╗ ██╗  ██╗
 ██╔════╝ ████╗  ██║ ██╔══██╗     ╚══██╔══╝ ██╔══██╗ ██╔════╝ ██║ ██╔╝
 █████╗   ██╔██╗ ██║ ██║  ██║        ██║    ███████║ ███████╗ █████╔╝
 ██╔══╝   ██║╚██╗██║ ██║  ██║        ██║    ██╔══██║ ╚════██║ ██╔═██╗
 ███████╗ ██║ ╚████║ ██████╔╝        ██║    ██║  ██║ ███████║ ██║  ██╗
 ╚══════╝ ╚═╝  ╚═══╝ ╚═════╝         ╚═╝    ╚═╝  ╚═╝ ╚══════╝ ╚═╝  ╚═╝
*/

/*
 - Function definition for process termination.
 - Takes the pid and signal code (SIGTERM(15) or SIGKILL(9)) as input.
*/
#define _GNU_SOURCE
#include "terminator.h"
#include <signal.h>
#include <stdio.h>
#include <sys/types.h>

int terminateProcess(pid_t pid, int signal) {
  if (kill(pid, signal) == -1) {
    perror("kill failed");
    return -1;
  }
  return 0;
}
