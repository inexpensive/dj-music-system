/************************************************************
 * Server for DJ Music Manager (tentative title)  			*
 * waits for requests from a proxy and passes	            *
 * them to the MusicPlayer	        						*
 *                                                          *
 * uses the Apache commons library                          *
 *                                                          *
 * uses JavaFX                                              *
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary start a server to control a Music System.
 */
package server;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javafx.scene.media.MediaPlayer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import player.LocalSong;
import player.Message;
import player.MusicPlayer;
import player.Song;

import javax.swing.*;

public class DJServer extends JFrame{
	
	private ServerSocket server;
	private MusicPlayer player;
	private boolean paused = false;
	private boolean playlistInit = false;
	private boolean started = false;
	private int skipRequestCount;
	private final ArrayList<ServerProxy> proxies = new ArrayList<>();
	private final Executor pool = Executors.newCachedThreadPool();
    private ArrayList<LocalSong> localSongs = new ArrayList<>();
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JPanel panel;
    private JSpinner spinner;

    /**
     * Starts the sockets used to send messages back and forth between the server and the client.
     *
     * Uses ServerProxy objects to actually handle the flow of messages between the server and clients.
     *
     * Creates and handles communication to a MusicPlayer, which actually plays the requested music.
     *
     * Requires logging into Spotify with a premium account.
     *
     * @param mediaPlayer the JavaFX Media Player from the main method used to play local music files
     * @throws IOException If an input or output exception has occurred
     */
	DJServer(MediaPlayer mediaPlayer)  throws IOException {
        super("DJ Music Manager");

        player = new MusicPlayer(this, mediaPlayer);

        //set up the login controls for the server
        usernameTextField = new JTextField("Spotify username");
        usernameTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                usernameTextField.setText("");
                usernameTextField.removeMouseListener(this);
            }
        });
        //login request is sent via a RETURN keystroke
        passwordField = new JPasswordField(20);
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    passwordField.removeActionListener(this);
                    login();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        panel = new JPanel(new GridLayout(2,2));
        panel.add(usernameTextField);
        panel.add(passwordField);
        this.add(panel);
        this.pack();
        this.setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


		
	}

    /**
     * Attempts to login to Spotify.
     * If successful, it changes the GUI to have a setable system password and skip threshold.
     *
     * @throws IOException
     */
    private void login() throws IOException {
        //set up server and allow a client to connect
        server = new ServerSocket(1729);
        //log
        String username = usernameTextField.getText();
        String spotifyPassword = new String(passwordField.getPassword());
        player.login(username, spotifyPassword);
        //noinspection UnusedAssignment
        spotifyPassword = null; //clear password
        skipRequestCount = 0;

        //add all local songs to the server
        File songFolder = new File("/home/lawrence/Documents/School/Taylor Swift – 1989/");
        File[] listOfSongs = songFolder.listFiles();
        if (listOfSongs != null) {
            for (File file : listOfSongs) {
                String path = file.getAbsolutePath();
                String ext = FilenameUtils.getExtension(path);
                if (validExtension(ext))
                    localSongs.add(new LocalSong(path));
            }
        }
        JLabel passwordReminderText = new JLabel("System Password:");
        JLabel spinnerReminderText = new JLabel("Skip Request Threshold:");

        panel.remove(usernameTextField);
        panel.remove(passwordField);
        spinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));

        panel.add(passwordReminderText);
        panel.add(passwordField);
        panel.add(spinnerReminderText);
        panel.add(spinner);
        pack();



        this.createNewProxy();
    }

    /**
     * Checks if the given filepath is a .m4a or .mp3 file.
     * @param path the path of a file as a string.
     * @return true if the file extension has .m4a or .mp3 in it. false otherwise.
     */
    private boolean validExtension(String path) {
        return path.contentEquals("m4a") || path.contentEquals("mp3");
    }

    /**
     * Creates a new Server Proxy with which a client can connect to in order to control the server.
     * The Server Proxy is created on a new thread in order to be controlled asynchronously.
     */
    void createNewProxy() {
		Runnable r = () -> {
            try {
                proxy();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        };
		pool.execute(r);
		
	}

    /**
     * Adds a new Server Proxy to the proxies ArrayList.
     * @throws IOException
     */
	private void proxy() throws IOException{
		proxies.add(new ServerProxy(server,this));
	}

    /**
     * Sends a play command to the Music Player if the playlist is initialized, and the Music Player is not started.
     * Otherwise, it invokes the pause method if the Music Player is paused.
     */
	 void play(){
		if (!started && playlistInit) {
			player.play();
			started = true;
		}
		else if (paused){
			pause();
		}
	}

    /**
     * Sends a pause command to the Music Player and flips the status of the paused variable.
     */
	void pause(){
		player.pause();
		paused = !paused;
	}

    /**
     * Increases the skipRequestCount and checks it vs the set threshold (in the GUI).
     * If the skipRequestCount is >= the threshold, a skip command is sent to the MusicPlayer.
     * Sets paused to false.
     */
	void skip(){
		skipRequestCount++;
		int threshold = (int) spinner.getValue();
		if (skipRequestCount >= threshold) {
			player.skip();
            paused = false;
		}

	}

    /**
     * Resets the skipRequested status on each ServerProxy and sets the skipRequestCount to 0.
     * This is called by the MusicPlayer anytime a skip occurs (whether triggered or commanded by a user).
     */
    public void resetSkipRequests() {
        for (ServerProxy proxy : proxies) {
            proxy.resetSkipRequested();
        }
        skipRequestCount = 0;
    }

    /**
     * Searches through the locally indexed songs for the target, adds them to a Song ArrayList and appends the top ten results from Spotify.
     * @param target The search String the user is looking for.
     * @return An ArrayList of Songs that match the target.
     */
	ArrayList<Song> search(String target){

        ArrayList<Song> fromLocal = this.searchLocal(target);
        ArrayList<Song> fromSpotify = player.search(target);
        fromLocal.addAll(fromSpotify);
        return fromLocal;
	}

    /**
     * Search through the locally indexed songs for the target. A match occurs if the song title, artist or album contains a part of the search target.
     * @param target The search String the user is looking for.
     * @return An Arraylist of Songs that match the target.
     */
    private ArrayList<Song> searchLocal(String target) {
        ArrayList<Song> results = new ArrayList<>();
        for (LocalSong song : localSongs) {
            String title = song.getTitle();
            String artist = song.getArtist();
            String album = song.getAlbum();
            if(StringUtils.containsIgnoreCase(title, target)
                    || StringUtils.containsIgnoreCase(artist, target)
                    || StringUtils.containsIgnoreCase(album, target)) {
                results.add(song);
            }
        }
        return results;
    }

    /**
     * Closes the server.
     * @throws IOException
     */
	public void close() throws IOException{
		server.close();
		System.exit(0);
	}


    /**
     * Sends the supplied song to the MusicPlayer and tells it to add it to the playlist.
     * Sets the playlistInit status to true if it hasn't been yet initialized.
     * @param song The song that is to be added to the playlist.
     */
    void add(Song song) {
        player.addSong(song);
        if (!playlistInit) {
            playlistInit = true;
        }
    }

    /**
     * Gets the playlist details from the MusicPlayer as a String array and returns it.
     * @return A String array of the playlist details.
     */
	String[] getPlaylistDetails() {
		return player.getPlaylistDetails();
	}

    /**
     * Removes the given ServerProxy from the proxies ArrayList.
     * @param serverProxy The ServerProxy to be removed.
     */
	void removeProxy(ServerProxy serverProxy) {
		proxies.remove(serverProxy);
	}

    /**
     * Checks if the given password matches the set system password (set in the GUI).
     * @param check The password to be checked.
     * @return true if the password matches, false otherwise.
     */
    boolean checkPassword(String check){
        return (new String(passwordField.getPassword())).contentEquals(check);
    }

    /**
     * Directly sends a skip command to the MusicPlayer without the need of the skipRequestCount.
     * Sets paused to false.
     */
    void adminSkip() {
        player.skip();
        paused = false;
    }

    /**
     * Sends the elapsed and duration times to the ServerProxies in order to update the progress bar in the client UI.
     * @param elapsed The elapsed time of the currently playing song.
     * @param duration The total duration of the currently playing song.
     */
	public void sendUpdateElapsed(long elapsed, long duration) {
        for (ServerProxy proxy : proxies) {
            proxy.updateElapsed(elapsed, duration);
        }
	}

    /**
     * @return true if the MusicPlayer has received a valid play command, false otherwise.
     */
    boolean isStarted() {
        return started;
    }

    /**
     * Takes a recorded message from the client and creates a Message out of it, and adds it to the playlist.
     * @param myByteArray The received message file's bytes from the client.
     */
    void processMessage(byte[] myByteArray) {
        File directory = new File("Recorded Messages");
        String filename = directory.getAbsolutePath().concat("/").concat(new Date(System.currentTimeMillis()).toString()).concat(".m4a");
        File newFile = new File(filename);
        System.out.println(filename);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(newFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(myByteArray, 0 , myByteArray.length);
            bos.flush();
            bos.close();
            player.addSong(new Message(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
