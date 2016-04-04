/************************************************************
 * ServerMain for DJ Music Manager (tentative title)  		*
 * starts the Server for the DJ Music Manager	            *
 *                                                          *
 * uses JavaFX                                              *
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary start a server to control a Music System.
 */
package server;


import javafx.application.Application;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.IOException;


public class ServerMain extends Application {
    private static MediaPlayer mediaPlayer;

    /**
     * Create a DJServer and start JavaFX.
     * @param args The MediaPlayer used to play local files.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new DJServer(mediaPlayer);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

    }

}
