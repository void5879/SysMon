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
