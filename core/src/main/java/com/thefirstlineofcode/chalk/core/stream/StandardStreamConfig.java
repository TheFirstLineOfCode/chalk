package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.chalk.core.stream.keepalive.KeepAliveConfig;

public class StandardStreamConfig extends StreamConfig {
	private String resource;
	private boolean tlsPreferred;
	private KeepAliveConfig keepAliveConfig;
	
	public StandardStreamConfig(String host, int port) {
		this(host, port, false);
	}
	
	public StandardStreamConfig(String host, int port, boolean tlsPreferred) {
		this(host, port, tlsPreferred, createDefaultKeepAliveConfig());
	}
	
	private static KeepAliveConfig createDefaultKeepAliveConfig() {
		return new KeepAliveConfig();
	}

	public StandardStreamConfig(String host, int port, boolean tlsPreferred, KeepAliveConfig keepAliveConfig) {
		super(host, port);
		
		this.tlsPreferred = tlsPreferred;
		this.keepAliveConfig = keepAliveConfig;
		this.resource = null;
	}
	
	public boolean isTlsPreferred() {
		return tlsPreferred;
	}

	public void setTlsPreferred(boolean tlsPreferred) {
		this.tlsPreferred = tlsPreferred;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
	
	public KeepAliveConfig getKeepAliveConfig() {
		return keepAliveConfig;
	}

}
