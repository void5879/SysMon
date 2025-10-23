/*
 - PROCESS DETAILS:
 - Data model for a single process.
 - This class holds the raw data parsed from the C backend (e.g., PID, name,
   totalTime, memRssKb).
 - It also contains the JavaFX Properties (e.g., cpuPercent, memRssString) that
   the TableView columns bind to for automatic UI updates.
*/

package com.sysmon.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProcessInfo {
  private final IntegerProperty pid;
  private final IntegerProperty ppid;
  private final StringProperty userName;
  private final StringProperty state;
  private final StringProperty processName;
  private final long totalTime;
  private final long memRssKb;
  private final DoubleProperty cpuPercent;
  private final DoubleProperty memPercent;
  private final StringProperty memRssString;

  public ProcessInfo(int pid, int ppid, String userName, String state, String processName, long totalTime,
      long memRssKb) {
    this.pid = new SimpleIntegerProperty(pid);
    this.ppid = new SimpleIntegerProperty(ppid);
    this.userName = new SimpleStringProperty(userName);
    this.state = new SimpleStringProperty(state);
    this.processName = new SimpleStringProperty(processName);
    this.totalTime = totalTime;
    this.memRssKb = memRssKb;
    this.cpuPercent = new SimpleDoubleProperty(0.0);
    this.memPercent = new SimpleDoubleProperty(0.0);
    this.memRssString = new SimpleStringProperty("0.0 MB");
  }

  public int getPid() {
    return pid.get();
  }

  public IntegerProperty pidProperty() {
    return pid;
  }

  public int getPpid() {
    return ppid.get();
  }

  public IntegerProperty ppidProperty() {
    return ppid;
  }

  public String getUserName() {
    return userName.get();
  }

  public StringProperty userNameProperty() {
    return userName;
  }

  public String getState() {
    return state.get();
  }

  public StringProperty stateProperty() {
    return state;
  }

  public String getProcessName() {
    return processName.get();
  }

  public StringProperty processNameProperty() {
    return processName;
  }

  public long getTotalTime() {
    return totalTime;
  }

  public long getMemRssKb() {
    return memRssKb;
  }

  public double getCpuPercent() {
    return cpuPercent.get();
  }

  public void setCpuPercent(double value) {
    this.cpuPercent.set(value);
  }

  public DoubleProperty cpuPercentProperty() {
    return cpuPercent;
  }

  public double getMemPercent() {
    return memPercent.get();
  }

  public void setMemPercent(double value) {
    this.memPercent.set(value);
  }

  public DoubleProperty memPercentProperty() {
    return memPercent;
  }

  public String getMemRssString() {
    return memRssString.get();
  }

  public void setMemRssString(String value) {
    this.memRssString.set(value);
  }

  public StringProperty memRssStringProperty() {
    return memRssString;
  }
}
