package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import player.MusicPlayer;


public class GUI extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton login, play, pause, search, addToPlaylist, skip;
	private JLabel currentlyPlaying;
	private Socket serverSocket;
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	
	protected boolean loggedIn = false;
	protected JTextField username, searchTarget;
	protected JPasswordField password;
	protected MusicPlayer player;
	protected JPanel panel;
	protected JComboBox<String> searchResults;
	protected Executor pool = Executors.newFixedThreadPool(5);

	
	public GUI() throws UnknownHostException, IOException{
		//preamble to set the frame up
		super("DJ Music Manager");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		serverSocket = new Socket("127.0.0.1", 1729);
		inFromServer = new ObjectInputStream(serverSocket.getInputStream());
		outToServer = new ObjectOutputStream(serverSocket.getOutputStream());
		
		
		player = new MusicPlayer(this);
		panel = new JPanel();
		login = new JButton("Login");
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		
		//logs in using the supplied username/password
		//gets rid of the login info and adds in the controls
		//TODO: only do the swap over to controls if the login is actually successful
		login.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					login();
				} 
				catch (IOException | ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//clears the username field the first time you click in it
		username = new JTextField("username", 20);
		username.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
					username.setText("");
					username.removeMouseListener(this);
			}
		});
		
		//password field
		password = new JPasswordField(20);
		password.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					login();
				} 
				catch (IOException | ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//plays selected song from the search results combo box
		play = new JButton("Play");
		play.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					play();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
		});
		
		//pauses the track if it is playing. resumes it otherwise.
		pause = new JButton("Pause");
		pause.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				player.pause();
			}
		});
		
		//searches for and plays the first result based on what is typed in the box
		search = new JButton("Search");
		search.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					search(searchTarget.getText());
				} 
				catch (ClassNotFoundException | IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		//the search box
		searchTarget = new JTextField(35);
		searchTarget.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					search(searchTarget.getText());
				} 
				catch (IOException | ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});

		//currently playing song label
		currentlyPlaying = new JLabel("No Song Loaded", SwingConstants.LEFT); 
		
		//setup default search result
		String[] defaultSearchResult = new String[1];
		defaultSearchResult[0] = "Search first by";
		searchResults = new JComboBox<String>(defaultSearchResult);
		
		//addtoplaylist button
		addToPlaylist = new JButton("Add to Playlist");
		addToPlaylist.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					addSong(searchResults.getSelectedIndex());
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//skip button
		skip = new JButton("Skip");
		skip.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					skip();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		panel.add(username);
		panel.add(password);
		panel.add(login);
		this.add(panel);
	}
	
	

	protected void addSong(int index) throws IOException {
		outToServer.writeObject("add");
		outToServer.writeObject(Integer.valueOf(index));
		
	}

	protected void play() throws IOException {
		outToServer.writeObject("play");
		
	}
	
	protected void skip() throws IOException {
		outToServer.writeObject("skip");
	}
	
	protected void pause() throws IOException {
		outToServer.writeObject("pause");
	}

	protected void login() throws IOException, ClassNotFoundException {
		outToServer.writeObject("login");
		outToServer.writeObject(username.getText());
		outToServer.writeObject(new String(password.getPassword()));
		String result = (String) inFromServer.readObject();
		System.out.println(result);
		if (result.equals("yes")) {
			panel.remove(username);
			panel.remove(password);
			panel.remove(login);
			panel.add(play);
			panel.add(pause);
			panel.add(search);
			panel.add(searchTarget);
			panel.add(currentlyPlaying);
			panel.add(searchResults);
			panel.add(addToPlaylist);
			panel.add(skip);
			pack();
		}
		
	}

	//displays the GUI
	public void display(){
		this.pack();
		this.setVisible(true);
	}
	
	//update the currently playing label with info pulled from the MusicPlayer
	public void updateCurrentlyPlaying(){
		currentlyPlaying.setText(player.getSongTitle() + " by " + player.getArtist() + " on " + player.getAlbum());
		pack();
	}
	
	protected void search(String text) throws IOException, ClassNotFoundException {
		outToServer.writeObject("search");
		outToServer.writeObject(searchTarget.getText());
		String[] results = (String[]) inFromServer.readObject();
		this.updateSearchComboBox(results);
	}
	
	//fill the combo box with results from the search
	private void updateSearchComboBox(String[] results){
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) searchResults.getModel();
		model.removeAllElements();
		for (String item : results){
			model.addElement(item);
		}
		pack();
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		// make a frame
		GUI g;
		g = new GUI();
		g.display();
	}
}
