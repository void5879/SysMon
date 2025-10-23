/*
 - BACKGROUND WORKER:
 - A JavaFX Service that runs the ipcClient.getSystemUpdate() method on a
   background thread.
 - It runs in parallel with ProcessService to fetch all system-wide stats
   (CPU, Mem, Net) asynchronously for the dashboard.
 - This keeps the UI responsive while dashboard data is being fetched.
*/

package com.sysmon.service;

import com.sysmon.model.SystemUpdate;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SystemUpdateService extends Service<SystemUpdate> {
  private final IPCClient ipcClient = new IPCClient();

  @Override
  protected Task<SystemUpdate> createTask() {
    return new Task<>() {
      @Override
      protected SystemUpdate call() throws Exception {
        return ipcClient.getSystemUpdate();
      }
    };
  }
}
