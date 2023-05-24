package com.thefirstlineofcode.chalk.network;

public interface IConnectionListener {
	void exceptionOccurred(ConnectionException exception);
	void messageReceived(String message);
	void heartBeatsReceived(int length);
	void messageSent(String message);
}
