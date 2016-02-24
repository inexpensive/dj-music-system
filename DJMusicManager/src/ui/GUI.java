/************************************************************
 * GUI-based client for DJ Music Manager (tentative title)  *
 * connects to the server and sends requests to it          *
 * via the buttons in the GUI        						*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class GUI extends JFrame{
	
	private static final long serialVersionUID = 1L;
	private JButton login, play, pause, search, addToPlaylist, skip;
	private JLabel currentlyPlaying;
	private Socket serverSocket;
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	
	protected JTextField username, searchTarget;
	protected JPasswordField password;
	protected JPanel panel;
	protected JComboBox<String> searchResults;
	protected Executor pool = Executors.newFixedThreadPool(5);


	
	public GUI() throws UnknownHostException, IOException{
		//preamble to set the frame up
		super("DJ Music Manager");
		
		//server details
		serverSocket = new Socket("127.0.0.1", 1729);
		inFromServer = new ObjectInputStream(serverSocket.getInputStream());
		outToServer = new ObjectOutputStream(serverSocket.getOutputStream());
		
		//setting up panel
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		//sends a login request
		login = new JButton("Login");
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
		//enter sends a login request
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
		
		//sends a play request
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
		
		//sends a pause request
		pause = new JButton("Pause");
		pause.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try {
					pause();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//sends a search request for what is typed in the box
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
		//enter is equivalent to pressing the search button
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
		
		//send an add request
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
		
		//send a skip request
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
		
		
		//add in the login controls up initialization
		panel.add(username);
		panel.add(password);
		panel.add(login);
		this.add(panel);
		
		//make the frame run closeServer on window closing
		WindowListener exitListener = new WindowAdapter() {

		    @Override
		    public void windowClosing(WindowEvent e) {
		        int confirm = JOptionPane.showOptionDialog(
		             null, "Are You Sure to Close Application?", 
		             "Exit Confirmation", JOptionPane.YES_NO_OPTION, 
		             JOptionPane.QUESTION_MESSAGE, null, null, null);
		        if (confirm == 0) {
		           try {
					closeServer();
		           } 
		          catch (IOException e1) {
					e1.printStackTrace();
		          }
		        }
		    }
		};
		this.addWindowListener(exitListener);
	}
	
	
	//sends an add request and the index of the song to be added to the server
	protected void addSong(int index) throws IOException {
		outToServer.writeObject("add");
		outToServer.writeObject(Integer.valueOf(index));
	}

	//sends a play request to the server
	protected void play() throws IOException {
		outToServer.writeObject("play");
		
	}
	
	//sends a skip request to the server
	protected void skip() throws IOException {
		outToServer.writeObject("skip");
	}
	
	//sends a pause request to the server
	protected void pause() throws IOException {
		outToServer.writeObject("pause");
	}
	
	//sends a login request to the server (including credentials from the respective boxes)
	//waits for an all good response from the server and swaps the controls over to the MusicPlayer controls
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
	
	//sends a close request to the server
	protected void closeServer() throws IOException {
		outToServer.writeObject("close");
		System.exit(0);
	}

	//displays the GUI
	public void display(){
		this.pack();
		this.setVisible(true);
	}
	
	//update the currently playing label with the passed in string
	public void updateCurrentlyPlaying(String deets){
		currentlyPlaying.setText(deets);
		pack();
	}
	
	//sends a search request to the server along with a target string
	//receives a string array of results from the server and passes it to the combobox
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
	
	
	//main method (create a GUI frame)
	public static void main(String[] args) throws UnknownHostException, IOException {
		// make a frame
		GUI g;
		g = new GUI();
		g.display();
	}
	
	
}
