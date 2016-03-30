package server;


import javafx.application.Application;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.IOException;


public class ServerMain extends Application {
    private static MediaPlayer mediaPlayer;

    public static void main(String[] args) throws IOException {
        new DJServer("password", mediaPlayer);
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {

    }

}
