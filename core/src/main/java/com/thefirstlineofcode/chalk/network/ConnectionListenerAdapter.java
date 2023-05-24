package com.thefirstlineofcode.chalk.network;

public abstract class ConnectionListenerAdapter implements IConnectionListener {
	@Override
	public void heartBeatsReceived(int length) {}
}
