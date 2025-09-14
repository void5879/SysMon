package com.sysmon.controller;

import com.sysmon.model.ProcessInfo;
import com.sysmon.service.IPCClient;
import com.sysmon.service.ProcessService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

    /**
     * This method is automatically called after the FXML file has been loaded.
     * It's used to initialize the controller and set up the UI components.
     */
    @FXML
    public void initialize() {
        // 1. Link table columns to the properties in the ProcessInfo model.
        // The string in PropertyValueFactory must exactly match the property name in
        // ProcessInfo.java.
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        ppidColumn.setCellValueFactory(new PropertyValueFactory<>("ppid"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        commandColumn.setCellValueFactory(new PropertyValueFactory<>("processName"));
        // 2. Bind the table's data directly to the result of the background service.
        // This is the core of data binding: the table will automatically update when
        // the service succeeds.
        processTable.itemsProperty().bind(processService.valueProperty());
        // 3. Set up a timeline to restart the service every 3 seconds, refreshing the
        // data.
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (!processService.isRunning()) {
                processService.restart();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        // 4. Perform the initial data load.
        processService.start();
        // 5. Set the action for the "End Task" button.
        btnEndTask.setOnAction(e -> handleEndTask());
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
