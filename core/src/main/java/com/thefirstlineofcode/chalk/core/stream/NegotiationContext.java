package com.thefirstlineofcode.chalk.core.stream;

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.IConnectionListener;


public class NegotiationContext implements INegotiationContext {
	private IConnection connection;
	private Map<Object, Object> attributes;
	
	public NegotiationContext(IConnection connection) {
		this.connection = connection;
		attributes = new HashMap<>();
	}

	@Override
	public void connect(String host, int port) throws ConnectionException {
		connection.connect(host, port);
	}
	
	@Override
	public void connect(String host, int port, int timeout) throws ConnectionException {
		connection.connect(host, port, timeout);
	}

	@Override
	public void close() {
		connection.close();
	}

	@Override
	public void write(String message) {
		connection.write(message);
	}
	
	@Override
	public void write(byte[] bytes) {
		connection.write(bytes);
	}
	
	@Override
	public void addListener(IConnectionListener listener) {
		connection.addListener(listener);
	}

	@Override
	public boolean removeListener(IConnectionListener listener) {
		return connection.removeListener(listener);
	}
	
	@Override
	public IConnectionListener[] getListeners() {
		return connection.getListeners();
	}

	@Override
	public void setAttribute(Object key, Object value) {
		attributes.put(key, value);
	}

	@Override
	public Object getAttribute(Object key) {
		return attributes.get(key);
	}
	
	@Override
	public boolean removeAttribute(Object key) {
		return attributes.remove(key) != null;
	}

	@Override
	public Set<Object> getAttributeKeys() {
		return attributes.keySet();
	}

	@Override
	public IConnection getConnection() {
		return connection;
	}

	@Override
	public boolean isTlsSupported() {
		return connection.isTlsSupported();
	}

	@Override
	public boolean isTlsStarted() {
		return connection.isTlsStarted();
	}

	@Override
	public Certificate[] startTls() throws ConnectionException {
		return connection.startTls();
	}

	@Override
	public boolean isClosed() {
		return connection.isClosed();
	}

	@Override
	public boolean isConnected() {
		return connection.isConnected();
	}
}
