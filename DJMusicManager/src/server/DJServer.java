package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import player.MusicPlayer;

public class DJServer {
	
	private ServerSocket server;
	private Socket clientSocket;
	private MusicPlayer player;
	private ObjectOutputStream outToClient;
	private ObjectInputStream inFromClient;
	private boolean done = false;
	private String command;
	
	protected Executor pool = Executors.newFixedThreadPool(5);
	
	public DJServer() throws IOException{
		server = new ServerSocket(1729);
		clientSocket = server.accept();
		player = new MusicPlayer(this);
		outToClient = new ObjectOutputStream(clientSocket.getOutputStream());
		inFromClient = new ObjectInputStream(clientSocket.getInputStream());
		Runnable r = new Runnable(){
			@Override
			public void run(){
				try {
					listener();
				} 
				catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		};
		pool.execute(r);
	}

	public void play(){
		player.play();
	}
	
	public void pause(){
		player.pause();
	}
	
	public void skip(){
		player.skip();
	}
	
	public void login(String username, String password){
		player.login(username, password);
	}
	
	public void close() throws IOException{
		server.close();
		clientSocket.close();
		System.exit(0);
	}
	
	protected void listener() throws IOException, ClassNotFoundException {
		System.out.println("NOW RUNNING");
		while(!done){
			System.out.println("waiting for input...");
			command = (String) inFromClient.readObject();
			System.out.println("received " + command); 
			if (command.equals("play")){
				this.play();
			}
			if (command.equals("pause")){
				this.pause();
			}
			if (command.equals("skip")){
				this.skip();
			}
			if (command.equals("login")){
				String username = (String) inFromClient.readObject();
				String password = (String) inFromClient.readObject();
				outToClient.writeObject("yes");
				this.login(username, password);				
			}
			if (command.equals("search")){
				String target = (String) inFromClient.readObject();
				String[] results = player.search(target);
				outToClient.writeObject(results);
			}
			if (command.equals("add")){
				int index = ((Integer) inFromClient.readObject()).intValue();
				player.addSong(index);
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException{
			new DJServer();
	}
}
