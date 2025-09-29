/*
 ██████╗   █████╗   ██████╗ ██╗  ██╗  ██████╗  ██████╗   ██████╗  ██╗   ██╗ ███╗   ██╗ ██████╗      ██╗    ██╗  ██████╗  ██████╗  ██╗  ██╗ ███████╗ ██████╗ 
 ██╔══██╗ ██╔══██╗ ██╔════╝ ██║ ██╔╝ ██╔════╝  ██╔══██╗ ██╔═══██╗ ██║   ██║ ████╗  ██║ ██╔══██╗     ██║    ██║ ██╔═══██╗ ██╔══██╗ ██║ ██╔╝ ██╔════╝ ██╔══██╗
 ██████╔╝ ███████║ ██║      █████╔╝  ██║  ███╗ ██████╔╝ ██║   ██║ ██║   ██║ ██╔██╗ ██║ ██║  ██║     ██║ █╗ ██║ ██║   ██║ ██████╔╝ █████╔╝  █████╗   ██████╔╝
 ██╔══██╗ ██╔══██║ ██║      ██╔═██╗  ██║   ██║ ██╔══██╗ ██║   ██║ ██║   ██║ ██║╚██╗██║ ██║  ██║     ██║███╗██║ ██║   ██║ ██╔══██╗ ██╔═██╗  ██╔══╝   ██╔══██╗
 ██████╔╝ ██║  ██║ ╚██████╗ ██║  ██╗ ╚██████╔╝ ██║  ██║ ╚██████╔╝ ╚██████╔╝ ██║ ╚████║ ██████╔╝     ╚███╔███╔╝ ╚██████╔╝ ██║  ██║ ██║  ██╗ ███████╗ ██║  ██║
 ╚═════╝  ╚═╝  ╚═╝  ╚═════╝ ╚═╝  ╚═╝  ╚═════╝  ╚═╝  ╚═╝  ╚═════╝   ╚═════╝  ╚═╝  ╚═══╝ ╚═════╝       ╚══╝╚══╝   ╚═════╝  ╚═╝  ╚═╝ ╚═╝  ╚═╝ ╚══════╝ ╚═╝  ╚═╝
*/

/*
 - This is a wrapper around the IPCClient.
 - It runs the socket communication in a background thread.
 - Keeps the UI responsive.
*/
package com.sysmon.service;

import com.sysmon.model.ProcessInfo;
import javafx.collections.ObservableList;
import javafx.concurrent.Service; // Service is a high-level manager that that executes a Task.
import javafx.concurrent.Task; // Task is the object that contains the actual code to be run in the background. 
// The ProcessService class inherits the background-processing capabilities from  the Service class.
/*
 * NOTE :
 * In JavaFX, Service<T> is a high-level API for running long-running background tasks (like querying processes, fetching data, etc.) without blocking the UI thread.
 * Service<T> is an abstract class.
 * Need to subclass it and implement its createTask() method, which defines the work to be done in the background.
 * The result type of that background work is T.
*/

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
