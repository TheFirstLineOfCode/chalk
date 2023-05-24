package com.thefirstlineofcode.chalk.network;

import java.security.cert.Certificate;

public interface IConnection {
	void connect(String host, int port) throws ConnectionException;
	void connect(String host, int port, int timeout) throws ConnectionException;
	void close();
	void write(String message);
	void write(byte[] bytes);
	
	boolean isConnected();
	boolean isClosed();
	
	void addListener(IConnectionListener listener);
	boolean removeListener(IConnectionListener listener);
	IConnectionListener[] getListeners();
	
	boolean isTlsSupported();
	boolean isTlsStarted();
	Certificate[] startTls() throws ConnectionException;
}
