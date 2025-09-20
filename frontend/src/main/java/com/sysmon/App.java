/*
 ███████╗ ███╗   ██╗ ████████╗ ██████╗  ██╗   ██╗     ██████╗   ██████╗  ██╗ ███╗   ██╗ ████████╗
 ██╔════╝ ████╗  ██║ ╚══██╔══╝ ██╔══██╗ ╚██╗ ██╔╝     ██╔══██╗ ██╔═══██╗ ██║ ████╗  ██║ ╚══██╔══╝
 █████╗   ██╔██╗ ██║    ██║    ██████╔╝  ╚████╔╝      ██████╔╝ ██║   ██║ ██║ ██╔██╗ ██║    ██║   
 ██╔══╝   ██║╚██╗██║    ██║    ██╔══██╗   ╚██╔╝       ██╔═══╝  ██║   ██║ ██║ ██║╚██╗██║    ██║   
 ███████╗ ██║ ╚████║    ██║    ██║  ██║    ██║        ██║      ╚██████╔╝ ██║ ██║ ╚████║    ██║   
 ╚══════╝ ╚═╝  ╚═══╝    ╚═╝    ╚═╝  ╚═╝    ╚═╝        ╚═╝       ╚═════╝  ╚═╝ ╚═╝  ╚═══╝    ╚═╝
*/

/*
 - JavaFX application entry point.
 - Loads the main FXML layout and initializes the primary application window.
*/
package com.sysmon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("MainView.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("SysMon");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
