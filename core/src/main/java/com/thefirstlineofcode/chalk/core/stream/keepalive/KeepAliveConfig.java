package com.thefirstlineofcode.chalk.core.stream.keepalive;

public class KeepAliveConfig {
	private static int DEFAULT_CHECK_INTERVAL = 1000;
	private static int DEFAULT_CLIENT_KEEP_ALIVE_INTERVAL = 60 * 1000;
	private static int DEFAULT_SERVER_KEEP_ALIVE_INTERVAL = 120 * 1000;
	private static int DEFAULT_TIMEOUT = 240 * 1000;
	
	private int checkingInterval;
	private int clientKeepAliveInterval;
	private int serverKeepAliveInterval;
	private int timeout;
	
	public KeepAliveConfig() {
		this(DEFAULT_CHECK_INTERVAL, DEFAULT_CLIENT_KEEP_ALIVE_INTERVAL,
				DEFAULT_SERVER_KEEP_ALIVE_INTERVAL, DEFAULT_TIMEOUT);
	}
	
	public KeepAliveConfig(int clientKeepAliveInterval, int serverKeepAliveInterval, int timeout) {
		this(DEFAULT_CHECK_INTERVAL, clientKeepAliveInterval, serverKeepAliveInterval, timeout);
	}
	
	public KeepAliveConfig(int checkingInterval, int clientKeepAliveInterval,
			int serverKeepAliveInterval, int timeout) {
		if (checkingInterval > clientKeepAliveInterval)
			throw new IllegalArgumentException("Client keep-alive interval shouldn't be less than checking interval");
		
		if (checkingInterval > serverKeepAliveInterval)
			throw new IllegalArgumentException("Server keep-alive interval shouldn't be less than checking interval");
		
		if (clientKeepAliveInterval > timeout)
			throw new IllegalArgumentException("Timeout shouldn't be less than client keep-alive interval.");
		
		if (serverKeepAliveInterval > timeout)
			throw new IllegalArgumentException("Timeout shouldn't be less than server-alive-interval interval.");
		
		this.checkingInterval = checkingInterval;
		this.clientKeepAliveInterval = clientKeepAliveInterval;
		this.serverKeepAliveInterval = serverKeepAliveInterval;
		this.timeout = timeout;
	}
	
	public int getCheckingInterval() {
		return checkingInterval;
	}

	public int getClientKeepAliveInterval() {
		return clientKeepAliveInterval;
	}
	
	public int getServerKeepAliveInterval() {
		return serverKeepAliveInterval;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KeepAliveConfig) {
			KeepAliveConfig other = (KeepAliveConfig)obj;
			return other.clientKeepAliveInterval == this.clientKeepAliveInterval &&
					other.serverKeepAliveInterval == this.serverKeepAliveInterval &&
					other.timeout == this.timeout;
		}
		
		return false;
	}
}
