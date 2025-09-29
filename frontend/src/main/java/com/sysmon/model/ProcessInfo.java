/*
 ██████╗   █████╗  ████████╗  █████╗      ███╗   ███╗  ██████╗  ██████╗  ███████╗ ██╗     
 ██╔══██╗ ██╔══██╗ ╚══██╔══╝ ██╔══██╗     ████╗ ████║ ██╔═══██╗ ██╔══██╗ ██╔════╝ ██║     
 ██║  ██║ ███████║    ██║    ███████║     ██╔████╔██║ ██║   ██║ ██║  ██║ █████╗   ██║     
 ██║  ██║ ██╔══██║    ██║    ██╔══██║     ██║╚██╔╝██║ ██║   ██║ ██║  ██║ ██╔══╝   ██║     
 ██████╔╝ ██║  ██║    ██║    ██║  ██║     ██║ ╚═╝ ██║ ╚██████╔╝ ██████╔╝ ███████╗ ███████╗
 ╚═════╝  ╚═╝  ╚═╝    ╚═╝    ╚═╝  ╚═╝     ╚═╝     ╚═╝  ╚═════╝  ╚═════╝  ╚══════╝ ╚══════╝
*/

/*
 - This is the Data model.
 - It is basically a template or blueprint for how a single process is represented in the application.
 - Kinda like a struct in C but with Java's OOP features.
*/
package com.sysmon.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/*
 * NOTE : 
 * IntegerProperty and StringProperty are special wrapper classes that hold an int or String value respectively.
 * They allow other parts of the application to listen to changes to their values.
 * SimpleIntegerProperty and SimpleStringProperty are the concrete implementations of the IntegerProperty and StringProperty classes.
*/

public class ProcessInfo {
  // Declaring the member variables of the class.
  private final IntegerProperty pid;
  private final IntegerProperty ppid;
  private final StringProperty userName;
  private final StringProperty state;
  private final StringProperty processName;

  // Constructor
  public ProcessInfo(int pid, int ppid, String userName, String state, String processName) {
    this.pid = new SimpleIntegerProperty(pid);
    this.ppid = new SimpleIntegerProperty(ppid);
    this.userName = new SimpleStringProperty(userName);
    this.state = new SimpleStringProperty(state);
    this.processName = new SimpleStringProperty(processName);
  }

  // Standard getter, it returns the raw integer value that is inside the property
  // object.
  public int getPid() {
    return pid.get();
  }

  // Special method for data binding, returns the property object itself.
  // NOTE :UI elements use this method to bind directly to the property, so they
  // can automatically update when the value changes.
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
}
