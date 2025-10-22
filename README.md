# SysMon - Linux Task Manager

A cross-language system process monitor for Linux featuring a high-performance C backend and modern JavaFX frontend, communicating through Unix domain sockets.

## 🚀 Features

- **Real-time Process Monitoring**: Live display of running system processes
- **Process Termination**: Send termination signals to processes (SIGTERM)
- **Responsive UI**: Background data fetching keeps the interface smooth
- **Smart Refresh**: Preserves scroll position and selection during updates
- **Cross-language Architecture**: Demonstrates C-Java interprocess communication

## 🏗️ Architecture

### Backend (C)

- **Socket Server**: Handles client connections on Unix domain socket
- **Process Parser**: Reads and parses `/proc` filesystem for process information
- **Process Termination**: Handles kill commands with specified signals
- **Protocol Handler**: Implements custom text-based communication protocol

### Frontend (JavaFX)

- **UI Controller**: Manages table updates and user interactions
- **Background Service**: Non-blocking process list fetching
- **IPC Client**: Unix socket communication with C backend
- **Data Binding**: Automatic UI updates with JavaFX properties

## 🛠️ Technology Stack

| Component     | Technology          | Purpose                                         |
| ------------- | ------------------- | ----------------------------------------------- |
| Backend       | C                   | High-performance system calls and /proc parsing |
| Frontend      | Java 24 + JavaFX    | Modern UI framework with data binding           |
| Communication | Unix Domain Sockets | Low-latency IPC                                 |

## 📁 Project Structure

```
SysMon/
├── backend/
│   └── src/
│       ├── main.c              # Socket server entry point
│       ├── procParser.c        # /proc filesystem parser
│       ├── procParser.h        # Parser interface
│       ├── terminator.c        # Process termination
│       └── terminator.h        # Termination interface
├── frontend/
│   ├── src/main/java/com/sysmon/
│   │   ├── App.java            # JavaFX application entry
│   │   ├── controller/
│   │   │   └── MainController.java    # UI event handling
│   │   ├── model/
│   │   │   └── ProcessInfo.java       # Data model with properties
│   │   └── service/
│   │       ├── IPCClient.java         # Socket communication
│   │       └── ProcessService.java    # Background service
│   ├── src/main/resources/com/sysmon/
│   │   └── MainView.fxml       # UI layout definition
│   └── pom.xml                 # Maven configuration
├── LICENSE                     # MIT License
├── SysMon.sh                   # Automation script
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- **Linux OS** (tested on Arch and Ubuntu)
- **GCC** compiler
- **Java 24** or higher
- **Maven** 3.6+
- **JavaFX** (included in dependencies)

### Building and Running

1. **Clone the repository and run the script**

    ```bash
    git clone https://github.com/void5879/SysMon.git
    cd SysMon
    chmod +x SysMon.sh
    ./SysMon.sh
    ```

### Usage

1. Run the Automation scipt (./SysMon.sh)
2. The process table will automatically populate and refresh every 3 seconds
3. Select a process and click "End Task" to send SIGTERM signal
4. Scroll position and selection are preserved during refreshes

## 🔌 Communication Protocol

The application uses a simple text-based protocol over Unix domain sockets.

## 🚧 Work in Progress

This project is still a WIP. Current functionality includes basic process monitoring and termination.

## 📋 TODO list

- [ ] **Memory Usage Display**
- [ ] **CPU Usage Monitoring**

## 🤝 Contributing

This is a personal learning project, but feel free (highly encourage!) to fork or contribute...feedback and suggestions are welcome!

### Development Focus Areas

- System programming in C
- JavaFX GUI development
- Inter-process communication

## 📊 Performance

- **Backend**: Efficient C implementation with minimal memory footprint
- **Communication**: Unix domain sockets provide low-latency IPC
- **Frontend**: Background threading keeps UI responsive
- **Updates**: Smart refresh preserves user experience

## 🐛 Known Issues

- Selection may be lost if process disappears between refreshes
- No graceful shutdown handling for backend server
- Limited error handling for malformed /proc entries
- Frontend doesn't auto-reconnect if backend restarts

## 📝 License

This project is available under the MIT License. See LICENSE file for details.

## 🎯 Learning Objectives

This project demonstrates:

- **System Programming**: Direct Linux system calls and /proc filesystem
- **Network Programming**: Unix domain socket implementation
- **GUI Development**: Modern JavaFX with FXML and data binding
- **Threading**: Background services and thread-safe UI updates
- **IPC**: Cross-language process communication

---

**Note**: This is an educational project focused on understanding system programming and inter-process communication. It may or may not be actively maintained.
