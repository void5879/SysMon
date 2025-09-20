/*
  ██████╗  ██████╗  ███╗   ██╗ ████████╗ ██████╗   ██████╗  ██╗      ██╗      ███████╗ ██████╗ 
 ██╔════╝ ██╔═══██╗ ████╗  ██║ ╚══██╔══╝ ██╔══██╗ ██╔═══██╗ ██║      ██║      ██╔════╝ ██╔══██╗
 ██║      ██║   ██║ ██╔██╗ ██║    ██║    ██████╔╝ ██║   ██║ ██║      ██║      █████╗   ██████╔╝
 ██║      ██║   ██║ ██║╚██╗██║    ██║    ██╔══██╗ ██║   ██║ ██║      ██║      ██╔══╝   ██╔══██╗
 ╚██████╗ ╚██████╔╝ ██║ ╚████║    ██║    ██║  ██║ ╚██████╔╝ ███████╗ ███████╗ ███████╗ ██║  ██║
  ╚═════╝  ╚═════╝  ╚═╝  ╚═══╝    ╚═╝    ╚═╝  ╚═╝  ╚═════╝  ╚══════╝ ╚══════╝ ╚══════╝ ╚═╝  ╚═╝
*/

/*
 - Main UI controller for the task manager application.
 - Manages process table updates, user interactions, and background services.
 - Implements periodic refresh while preserving scroll position and selection.
*/
package com.sysmon.controller;

import com.sysmon.model.ProcessInfo;
import com.sysmon.service.IPCClient;
import com.sysmon.service.ProcessService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

public class MainController {
    @FXML
    private TableView<ProcessInfo> processTable;
    @FXML
    private TableColumn<ProcessInfo, Integer> pidColumn;
    @FXML
    private TableColumn<ProcessInfo, Integer> ppidColumn;
    @FXML
    private TableColumn<ProcessInfo, String> userColumn;
    @FXML
    private TableColumn<ProcessInfo, String> stateColumn;
    @FXML
    private TableColumn<ProcessInfo, String> commandColumn;
    @FXML
    private Button btnEndTask;

    private final ProcessService processService = new ProcessService();
    private final IPCClient ipcClient = new IPCClient();

    private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        ppidColumn.setCellValueFactory(new PropertyValueFactory<>("ppid"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        commandColumn.setCellValueFactory(new PropertyValueFactory<>("processName"));

        processTable.setItems(processData);

        processService.setOnSucceeded(_ -> {
            ObservableList<ProcessInfo> newData = processService.getValue();
            if (newData != null) {
                ProcessInfo selectedItem = processTable.getSelectionModel().getSelectedItem();

                Platform.runLater(() -> {
                    processData.clear();
                    processData.addAll(newData);

                    if (selectedItem != null) {
                        for (ProcessInfo process : processData) {
                            if (process.getPid() == selectedItem.getPid()) {
                                processTable.getSelectionModel().select(process);
                                break;
                            }
                        }
                    }
                });
            }
        });

        processService.setOnFailed(_ -> {
            Throwable exception = processService.getException();
            System.err.println("ProcessService failed: " + exception.getMessage());
            exception.printStackTrace();
        });

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), _ -> {
            if (!processService.isRunning()) {
                processService.restart();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        processService.start();

        btnEndTask.setOnAction(_ -> handleEndTask());
    }

    private void handleEndTask() {
        ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
        if (selectedProcess != null) {
            new Thread(() -> {
                boolean success = ipcClient.killProcess(selectedProcess.getPid(), 15);
                Platform.runLater(() -> {
                    Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    alert.setTitle("Process Termination");
                    alert.setHeaderText(null);
                    String message = success
                            ? "Successfully sent termination signal to process " + selectedProcess.getPid() + "."
                            : "Failed to terminate process " + selectedProcess.getPid()
                                    + ". (Check backend console for errors)";
                    alert.setContentText(message);
                    alert.showAndWait();
                });
            }).start();
        }
    }
}
