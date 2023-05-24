package com.thefirstlineofcode.chalk.xeps.ping;

public interface IPingService {
	void enable(boolean enabled);
	boolean isEnabled();
	void supportClientPing(boolean clientPingSupported);
	boolean isClientPingSupported();
}
