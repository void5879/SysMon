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
