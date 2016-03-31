/************************************************************
 * Server for DJ Music Manager (tentative title)  			*
 * waits for requests from a proxy and passes	            *
 * them to the MusicPlayer	        						*
 *															*
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package server;

import jahspotify.media.Track;

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
	private final String password = "password";
    private ArrayList<LocalSong> localSongs = new ArrayList<>();
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JPanel panel;
    private JSpinner spinner;

	
	DJServer(String pass, MediaPlayer mediaPlayer)  throws IOException {
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

    private void login() throws IOException {
        //set up server and allow a client to connect
        server = new ServerSocket(1729);

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

    private boolean validExtension(String path) {
        return path.contentEquals("m4a") || path.contentEquals("mp3");
    }

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
	
	private void proxy() throws IOException{
		proxies.add(new ServerProxy(server,this));
	}

	//sends a play request to the MusicPlayer if not started and playlist is initialized
	//calls pause if the system is paused
	 void play(){
		if (!started && playlistInit) {
			player.play();
			started = true;
		}
		else if (paused){
			pause();
		}
	}
	
	//sends a pause request to the MusicPlayer	
	void pause(){
		player.pause();
		paused = !paused;
	}
	
	//sends a skip request to the MusicPlayer once enough skipRequests (threshold based on the JSpinner value) are received
	void skip(){
		skipRequestCount++;
		int threshold = (int) spinner.getValue();
		if (skipRequestCount >= threshold) {
			player.skip();
		}
        resetSkipRequests();
	}

    private void resetSkipRequests() {
        for (ServerProxy proxy : proxies) {
            proxy.resetSkipRequested();
        }
    }

    //searches through the local files for something that matches the target and adds on the top ten results from spotify
	ArrayList<Song> search(String target){

        ArrayList<Song> fromLocal = this.searchLocal(target);
        ArrayList<Song> fromSpotify = player.search(target);
        fromLocal.addAll(fromSpotify);
        return fromLocal;
	}

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

    //closes the server
	public void close() throws IOException{
		server.close();
		System.exit(0);
	}

    void add(Song song) {
        player.addSong(song);
        if (!playlistInit) {
            playlistInit = true;
        }
    }

	String trackToString(Track track) {
		return player.trackToString(track);
	}

	String getCurrentlyPlaying() {
		return player.getCurrentlyPlaying();
	}

	String[] getPlaylistDetails() {
		return player.getPlaylistDetails();
	}

	void removeProxy(ServerProxy serverProxy) {
		proxies.remove(serverProxy);
	}

    boolean checkPassword(String check){
        return new String(passwordField.getPassword()).contentEquals(check);
    }

    void adminSkip() {
        player.skip();
        resetSkipRequests();
    }

    //sends the elapsed time to the proxies
	public void sendUpdateElapsed(long elapsed, long duration) {
        for (ServerProxy proxy : proxies) {
            proxy.updateElapsed(elapsed, duration);
        }
	}

    //sends an update request to each proxy
    public void updateCurrentlyPlaying(String currentlyPlaying) throws IOException{
        for (ServerProxy proxy : proxies) {
            proxy.updateCurrentlyPlaying(currentlyPlaying);
            proxy.resetSkipRequested();
        }
        skipRequestCount = 0;
    }

    boolean isStarted() {
        return started;
    }


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
