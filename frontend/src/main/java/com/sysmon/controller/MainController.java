/*
 - CONTROLLER:
 - The main UI controller for the SysMon application.
 - This class connects the FXML view (MainView.fxml) to the application logic.
 - It manages the background services (ProcessService, SystemUpdateService) and
   the refresh Timeline.
 - It contains all logic for calculating deltas (e.g., per-process CPU %)
   and formatting data to display in the dashboard charts, labels, and table.
 - It also handles all user interactions (e.g., the "Kill Selected Process" button).
*/

package com.sysmon.controller;

import com.sysmon.model.ProcessInfo;
import com.sysmon.model.SystemUpdate;
import com.sysmon.service.IPCClient;
import com.sysmon.service.ProcessService;
import com.sysmon.service.SystemUpdateService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class MainController {
  @FXML
  private TableView<ProcessInfo> processTable;
  @FXML
  private TableColumn<ProcessInfo, Integer> pidColumn;
  @FXML
  private TableColumn<ProcessInfo, String> userColumn;
  @FXML
  private TableColumn<ProcessInfo, String> stateColumn;
  @FXML
  private TableColumn<ProcessInfo, String> commandColumn;
  @FXML
  private PieChart memChart;
  @FXML
  private Label lblMemTotal;
  @FXML
  private Label lblMemUsed;
  @FXML
  private Label lblMemFree;
  @FXML
  private PieChart cpuChart;
  @FXML
  private Label lblCpuUsage;
  @FXML
  private Label lblCpuCores;
  @FXML
  private Label lblTotalProcesses;
  @FXML
  private Label lblCached;
  @FXML
  private Label lblBuffers;
  @FXML
  private Label lblSwapTotal;
  @FXML
  private Label lblSwapFree;
  @FXML
  private Label lblNetDown;
  @FXML
  private Label lblNetUp;
  @FXML
  private CheckBox chkAutoRefresh;
  @FXML
  private TableColumn<ProcessInfo, Double> cpuPercentColumn;
  @FXML
  private TableColumn<ProcessInfo, Double> memPercentColumn;
  @FXML
  private TableColumn<ProcessInfo, String> memRssColumn;
  @FXML
  private Button btnEndTask;

  private final ProcessService processService = new ProcessService();
  private final SystemUpdateService systemUpdateService = new SystemUpdateService();
  private final IPCClient ipcClient = new IPCClient();
  private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();
  private Timeline timeline;

  private long prevSystemTotalTime = 0;
  private long systemTimeDelta = 0;
  private long memTotalKb = 1;
  private final Map<Integer, Long> prevProcessTimes = new HashMap<>();
  private final DecimalFormat percentFormat = new DecimalFormat("0.00'%'");
  private final DecimalFormat memFormat = new DecimalFormat("#,##0.00");

  @FXML
  public void initialize() {
    setupProcessTable();
    setupDashboardCharts();
    setupServices();
    setupToolbar();

    processTable.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldSelection, newSelection) -> btnEndTask.setDisable(newSelection == null));

    startAllServices();
    timeline.play();
  }

  private void setupProcessTable() {
    pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
    userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
    stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
    commandColumn.setCellValueFactory(new PropertyValueFactory<>("processName"));
    cpuPercentColumn.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
    memPercentColumn.setCellValueFactory(new PropertyValueFactory<>("memPercent"));
    memRssColumn.setCellValueFactory(new PropertyValueFactory<>("memRssString"));

    cpuPercentColumn.setCellFactory(col -> createPercentCell());
    memPercentColumn.setCellFactory(col -> createPercentCell());
    processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    processTable.setItems(processData);
  }

  private TableCell<ProcessInfo, Double> createPercentCell() {
    return new TableCell<>() {
      @Override
      protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
          setText(percentFormat.format(item));
          setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        }
      }
    };
  }

  private void setupDashboardCharts() {
    PieChart.Data cpuActiveSlice = new PieChart.Data("Active", 0);
    PieChart.Data cpuIdleSlice = new PieChart.Data("Idle", 100);
    cpuChart.setData(FXCollections.observableArrayList(cpuActiveSlice, cpuIdleSlice));
    cpuChart.setTitle("CPU");
    PieChart.Data memUsedSlice = new PieChart.Data("Used", 0);
    PieChart.Data memFreeSlice = new PieChart.Data("Free", 0);
    PieChart.Data memCachedSlice = new PieChart.Data("Cached", 0);
    memChart.setData(FXCollections.observableArrayList(memUsedSlice, memFreeSlice, memCachedSlice));
    memChart.setTitle("Memory");
  }

  private void setupServices() {
    processService.setOnSucceeded(_ -> {
      ObservableList<ProcessInfo> newData = processService.getValue();
      if (newData != null) {
        Platform.runLater(() -> updateProcessTable(newData));
      }
    });
    systemUpdateService.setOnSucceeded(_ -> {
      SystemUpdate stats = systemUpdateService.getValue();
      if (stats != null) {
        Platform.runLater(() -> updateDashboard(stats));
      }
    });
    processService.setOnFailed(_ -> processService.getException().printStackTrace());
    systemUpdateService.setOnFailed(_ -> systemUpdateService.getException().printStackTrace());

    timeline = new Timeline(
        new KeyFrame(
            Duration.seconds(3),
            _ -> {
              if (chkAutoRefresh.isSelected()) {
                startAllServices();
              }
            }));
    timeline.setCycleCount(Timeline.INDEFINITE);
  }

  private void setupToolbar() {
    chkAutoRefresh.setOnAction(_ -> {
      if (chkAutoRefresh.isSelected()) {
        timeline.play();
      } else {
        timeline.pause();
      }
    });
    btnEndTask.setOnAction(_ -> handleEndTask());
  }

  private void startAllServices() {
    if (!processService.isRunning()) {
      processService.restart();
    }
    if (!systemUpdateService.isRunning()) {
      systemUpdateService.restart();
    }
  }

  private void updateProcessTable(ObservableList<ProcessInfo> newData) {
    ProcessInfo selectedItem = processTable.getSelectionModel().getSelectedItem();
    int cores = Runtime.getRuntime().availableProcessors();
    Map<Integer, Long> newProcessTimes = new HashMap<>();
    for (ProcessInfo process : newData) {
      int pid = process.getPid();
      long currentProcessTime = process.getTotalTime();
      long memRssKb = process.getMemRssKb();
      long prevProcessTime = prevProcessTimes.getOrDefault(pid, 0L);
      long processTimeDelta = currentProcessTime - prevProcessTime;
      double cpuPercent = 0.0;
      if (systemTimeDelta > 0) {
        cpuPercent = ((double) processTimeDelta / (double) systemTimeDelta) * 100.0 * cores;
      }
      process.setCpuPercent(cpuPercent);
      double memPercent = (memTotalKb > 0) ? ((double) memRssKb / (double) this.memTotalKb) * 100.0 : 0.0;
      process.setMemPercent(memPercent);
      process.setMemRssString(formatRss(memRssKb));
      newProcessTimes.put(pid, currentProcessTime);
    }
    processData.setAll(newData);
    if (selectedItem != null) {
      for (ProcessInfo process : processData) {
        if (process.getPid() == selectedItem.getPid()) {
          processTable.getSelectionModel().select(process);
          break;
        }
      }
    } else {
      processTable.getSelectionModel().clearSelection();
    }
    lblTotalProcesses.setText(String.valueOf(newData.size()));
    prevProcessTimes.clear();
    prevProcessTimes.putAll(newProcessTimes);
  }

  private void updateDashboard(SystemUpdate stats) {
    long currentSystemTime = stats.getSystemTotalTime();
    if (prevSystemTotalTime > 0) {
      this.systemTimeDelta = currentSystemTime - prevSystemTotalTime;
    }
    this.prevSystemTotalTime = currentSystemTime;
    this.memTotalKb = stats.getMemTotal();
    double cpuUsage = stats.getCpuUsage();
    cpuChart.getData().get(0).setPieValue(cpuUsage);
    cpuChart.getData().get(1).setPieValue(100.0 - cpuUsage);
    lblCpuUsage.setText(String.format("%.1f%%", cpuUsage));
    lblCpuCores.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));
    double gibFactor = 1024.0 * 1024.0;
    double memTotalGiB = stats.getMemTotal() / gibFactor;
    double memAvailGiB = stats.getMemAvailable() / gibFactor;
    double memCachedGiB = stats.getCached() / gibFactor;
    double memUsedGiB = memTotalGiB - memAvailGiB;
    double memFreeGiB = stats.getMemFree() / gibFactor;
    memChart.getData().get(0).setPieValue(memUsedGiB);
    memChart.getData().get(1).setPieValue(memFreeGiB);
    memChart.getData().get(2).setPieValue(memCachedGiB);
    lblMemTotal.setText(memFormat.format(memTotalGiB) + " GiB");
    lblMemUsed.setText(String.format("%s GiB (%.0f%%)", memFormat.format(memUsedGiB),
        (memTotalGiB > 0 ? (memUsedGiB / memTotalGiB) * 100 : 0)));
    lblMemFree.setText(memFormat.format(memFreeGiB) + " GiB");
    lblCached.setText(memFormat.format(memCachedGiB) + " GiB");
    lblBuffers.setText(memFormat.format(stats.getBuffers() / gibFactor) + " GiB");
    lblSwapTotal.setText(memFormat.format(stats.getSwapTotal() / gibFactor) + " GiB");
    lblSwapFree.setText(memFormat.format(stats.getSwapFree() / gibFactor) + " GiB");
    lblNetDown.setText(formatSpeed(stats.getNetDownSpeed()));
    lblNetUp.setText(formatSpeed(stats.getNetUpSpeed()));
  }

  private String formatSpeed(long bytesPerSecond) {
    if (bytesPerSecond < 1024) {
      return bytesPerSecond + " B/s";
    }
    double kib = bytesPerSecond / 1024.0;
    if (kib < 1024) {
      return String.format("%.1f KiB/s", kib);
    }
    double mib = kib / 1024.0;
    return String.format("%.1f MiB/s", mib);
  }

  private String formatRss(long rssKb) {
    if (rssKb < 1024) {
      return rssKb + " KB";
    }
    double mib = rssKb / 1024.0;
    if (mib < 1024) {
      return String.format("%.2f MB", mib);
    }
    double gib = mib / 1024.0;
    return String.format("%.2f GB", gib);
  }

  private void handleEndTask() {
    ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
    if (selectedProcess == null) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("No Process Selected");
      alert.setHeaderText(null);
      alert.setContentText("Please select a process from the table to kill.");
      alert.showAndWait();
      return;
    }
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Confirm Kill Process");
    confirmAlert
        .setHeaderText("Kill Process: " + selectedProcess.getPid() + " (" + selectedProcess.getProcessName() + ")");
    confirmAlert.setContentText("Are you sure you want to send a termination signal (SIGTERM) to this process?");
    confirmAlert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        new Thread(() -> {
          boolean success = ipcClient.killProcess(selectedProcess.getPid(), 15); // SIGTERM
          Platform.runLater(() -> {
            Alert resultAlert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            resultAlert.setTitle("Process Termination Result");
            resultAlert.setHeaderText(null);
            String message = success
                ? "Successfully sent termination signal to process " + selectedProcess.getPid() + "."
                : "Failed to terminate process " + selectedProcess.getPid() + ".";
            resultAlert.setContentText(message);
            resultAlert.showAndWait();
            if (success) {
              startAllServices();
            }
          });
        }).start();
      }
    });
  }
}
