package com.thefirstlineofcode.chalk.core.stream.keepalive;

public interface IKeepAliveManager {
	public static final char CHAR_HEART_BEAT = ' ';
	public static final byte BYTE_HEART_BEAT = (byte)CHAR_HEART_BEAT;
	
	KeepAliveConfig getConfig();
	void changeConfig(KeepAliveConfig config);
	boolean isStarted();
	void setCallback(IKeepAliveCallback callback);
	IKeepAliveCallback getCallback();
	void start();
	void stop();
}
