package com.thefirstlineofcode.chalk.core.stream;

import java.util.HashMap;
import java.util.Map;

public class StreamConfig {
	public static final String PROPERTY_NAME_CHALK_MESSAGE_FORMAT = "chalk.message.format";
	
	protected String host;
	protected int port;
	protected String lang;
	protected Map<String, Object> properties;
	
	public StreamConfig(String host, int port) {
		this.host = host;
		this.port = port;
		
		properties = new HashMap<>();
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	
	public <T> T getProperty(String name, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)properties.get(name);
		
		return value == null ? defaultValue : value;
	}
	
}
