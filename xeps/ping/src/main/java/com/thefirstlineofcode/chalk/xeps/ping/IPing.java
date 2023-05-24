package com.thefirstlineofcode.chalk.xeps.ping;

public interface IPing {
	public enum Result {
		PONG,
		SERVICE_UNAVAILABLE,
		TIME_OUT
	}
	
	Result ping();
	void setTimeout(int timeout);
	int getTimeout();
}
