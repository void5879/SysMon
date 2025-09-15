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
    // UI Components injected from FXML
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

    // Services for handling backend communication
    private final ProcessService processService = new ProcessService();
    private final IPCClient ipcClient = new IPCClient();

    // Keep a persistent list to avoid scroll position reset
    private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();

    /**
     * This method is automatically called after the FXML file has been loaded.
     * It's used to initialize the controller and set up the UI components.
     */
    @FXML
    public void initialize() {
        // 1. Link table columns to the properties in the ProcessInfo model.
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        ppidColumn.setCellValueFactory(new PropertyValueFactory<>("ppid"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        commandColumn.setCellValueFactory(new PropertyValueFactory<>("processName"));

        // 2. Set the table items to our persistent list
        processTable.setItems(processData);

        // 3. Set up success handler to update data while preserving scroll position
        processService.setOnSucceeded(_ -> {
            ObservableList<ProcessInfo> newData = processService.getValue();
            if (newData != null) {
                // Store current scroll position and selection
                ProcessInfo selectedItem = processTable.getSelectionModel().getSelectedItem();

                // Update the existing list instead of replacing it
                Platform.runLater(() -> {
                    processData.clear();
                    processData.addAll(newData);

                    // Restore selection if the process still exists
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

        // 4. Set up error handler
        processService.setOnFailed(_ -> {
            Throwable exception = processService.getException();
            System.err.println("ProcessService failed: " + exception.getMessage());
            exception.printStackTrace();
        });

        // 5. Set up a timeline to restart the service every 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), _ -> {
            if (!processService.isRunning()) {
                processService.restart();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // 6. Perform the initial data load
        processService.start();

        // 7. Set the action for the "End Task" button
        btnEndTask.setOnAction(_ -> handleEndTask());
    }

    /**
     * Handles the logic for the "End Task" button click.
     */
    private void handleEndTask() {
        ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
        if (selectedProcess != null) {
            // Run the kill command on a background thread to keep the UI responsive.
            new Thread(() -> {
                // Send SIGTERM (15) for a graceful shutdown request.
                boolean success = ipcClient.killProcess(selectedProcess.getPid(), 15);
                // Show a confirmation dialog back on the UI thread.
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
