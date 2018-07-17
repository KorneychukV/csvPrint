package ru.vsplatforma.evt85.csvPrint;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(final Stage primaryStage) throws Exception{

        // Create the FXMLLoader
        FXMLLoader loader = new FXMLLoader();
        // Path to the FXML File
        String fxmlPath = "/fxml/mainWindow.fxml";
        Parent root = (Parent) loader.load(getClass().getResourceAsStream(fxmlPath));

        primaryStage.setTitle("csvPrint");
        //primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);

        Scene scene = new Scene(root, 1000, 735);
        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}