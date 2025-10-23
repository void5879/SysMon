/*
 - ENTRY POINT:
 - Main application entry point.
 - This class is responsible for launching the JavaFX application.
 - It loads the FXML view (MainView.fxml), loads the CSS stylesheet (styles.css),
   and displays the primary stage (the main window).
*/

package com.sysmon;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
  @Override
  public void start(Stage stage) throws IOException {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("MainView.fxml"));

    Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
    scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
    stage.setTitle("SysMon");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}
