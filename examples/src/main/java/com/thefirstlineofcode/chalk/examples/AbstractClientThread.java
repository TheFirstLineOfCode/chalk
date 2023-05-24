package com.thefirstlineofcode.chalk.examples;

import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.examples.cluster.MultiClientsClusterExample;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;

public abstract class AbstractClientThread extends ConnectionListenerAdapter implements Runnable {
	protected MultiClientsClusterExample example;
	protected StandardChatClient chatClient;
	
	public AbstractClientThread(StandardChatClient chatClient, MultiClientsClusterExample example) {
		this.chatClient = chatClient;
		this.example = example;
	}
	
	@Override
	public void run() {
		chatClient.getStream().addConnectionListener(this);
		
		try {
			String[] userNameAndPassword = getUserNameAndPassword();
			chatClient.connect(userNameAndPassword[0], userNameAndPassword[1]);
			
			doRun();
		} catch (Exception e) {
			example.printException(e);
		}
	}
	
	@Override
	public void messageSent(String message) {
		example.printString(getClass().getSimpleName() + " -> " + message);
	}
	
	@Override
	public void messageReceived(String message) {
		example.printString(getClass().getSimpleName() + " <- " + message);
	}
	
	@Override
	public void exceptionOccurred(ConnectionException exception) {}
	
	protected abstract String[] getUserNameAndPassword();
	protected abstract void doRun() throws Exception;
	protected abstract String getResourceName();
	
}