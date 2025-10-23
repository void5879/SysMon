/*
 - BACKGROUND WORKER:
 - A JavaFX Service that runs the ipcClient.getProcessList() method on a
   background thread.
 - Its sole purpose is to fetch the full process list asynchronously, preventing
   the UI (main thread) from freezing during the update.
 - The MainController observes this service for a "succeeded" event to update
   the process table.
*/

package com.sysmon.service;

import com.sysmon.model.ProcessInfo;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ProcessService extends Service<ObservableList<ProcessInfo>> {
  private final IPCClient ipcClient = new IPCClient();

  @Override
  protected Task<ObservableList<ProcessInfo>> createTask() {
    return new Task<>() {
      @Override
      protected ObservableList<ProcessInfo> call() throws Exception {
        return ipcClient.getProcessList();
      }
    };
  }
}
