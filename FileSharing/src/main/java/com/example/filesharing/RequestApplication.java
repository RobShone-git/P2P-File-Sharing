package com.example.filesharing;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;



public class RequestApplication extends Application {
    private static RequestGUIController controllerInstance;
    private static CountDownLatch latch;
    public static boolean result = false;

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("User Decision");

        // Set the scene and show the stage
        FXMLLoader fxmlLoader = new FXMLLoader(LoginApplication.class.getResource("RequestGUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Get the controller instance
        controllerInstance = fxmlLoader.getController();

        Button acceptButton = controllerInstance.btnAccept;
        Button declineButton = controllerInstance.btnDecline;

        // Define the buttons' actions
        acceptButton.setOnAction(event -> {
            result = true;
            latch.countDown();
            primaryStage.close(); // Close the application
        });

        declineButton.setOnAction(event -> {
            result = false;
            latch.countDown();
            primaryStage.close(); // Close the application
        });
    }

    /**
     * Awaits a button press by blocking the current thread until the latch is released.
     *
     * @return the result of the button press action, which is true if the button was pressed
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean awaitButtonPress() throws InterruptedException {
        latch = new CountDownLatch(1);
        latch.await();
        return result;
    }

    /**
     * Sets the result to false, typically used to reset the result state before awaiting another button press.
     */
    public void setResultBack() {
        result = false;
    }

}
