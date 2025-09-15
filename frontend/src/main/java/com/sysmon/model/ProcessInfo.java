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

public class ProcessInfo {
    private final IntegerProperty pid;
    private final IntegerProperty ppid;
    private final StringProperty userName;
    private final StringProperty state;
    private final StringProperty processName;

    public ProcessInfo(int pid, int ppid, String userName, String state, String processName) {
        this.pid = new SimpleIntegerProperty(pid);
        this.ppid = new SimpleIntegerProperty(ppid);
        this.userName = new SimpleStringProperty(userName);
        this.state = new SimpleStringProperty(state);
        this.processName = new SimpleStringProperty(processName);
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
}
