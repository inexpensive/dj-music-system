import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import player.MusicPlayer;


public class GUI extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton login, play, pause, search;
	private JLabel currentlyPlaying;
	
	protected JTextField username, searchTarget;
	protected JPasswordField password;
	protected MusicPlayer player;
	protected JPanel panel;

	
	public GUI(){
		//preamble to set the frame up
		super("DJ Music Manager");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		player = new MusicPlayer();
		panel = new JPanel();
		login = new JButton("Login");
		
		//logs in using the supplied username/password
		//gets rid of the login info and adds in the controls
		//TODO: only do the swap over to controls if the login is actually successful
		login.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0){
				player.login(username.getText(), new String(password.getPassword()));
				panel.remove(username);
				panel.remove(password);
				panel.remove(login);
				panel.add(play);
				panel.add(pause);
				panel.add(search);
				panel.add(searchTarget);
				panel.add(currentlyPlaying);
				pack();
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
		//TODO: trigger the login on pressing ENTER/RETURN in the password box
		password = new JPasswordField(20);
		
		//plays let's dance by david bowie
		play = new JButton("Play");
		play.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0){
				player.play("spotify:track:0F0MA0ns8oXwGw66B2BSXm");
				updateCurrentlyPlaying();
			}
		});
		
		//pauses the track if it is playing. resumes it otherwise.
		pause = new JButton("Pause");
		pause.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0){
				player.pause();
			}
		});
		
		//searches for and plays the first result based on what is typed in the box
		search = new JButton("Search");
		search.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0){
				player.play(player.search(searchTarget.getText()));
				updateCurrentlyPlaying();
			}
		});
		
		//the search box
		searchTarget = new JTextField(50);
		
		//currently playing song label
		currentlyPlaying = new JLabel("No Song Loaded"); 
		
		panel.add(username);
		panel.add(password);
		panel.add(login);
		this.add(panel);
	}
	
	//displays the GUI
	public void display(){
		this.pack();
		this.setVisible(true);
	}
	
	private void updateCurrentlyPlaying(){
		currentlyPlaying.setText(player.getSongTitle() + " by " + player.getArtist() + " on " + player.getAlbum());
		pack();
	}
}
