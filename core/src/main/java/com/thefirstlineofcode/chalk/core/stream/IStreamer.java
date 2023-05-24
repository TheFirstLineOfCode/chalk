package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public interface IStreamer {
	void setConnectTimeout(int connectTimeout);
	int getConnectTimeout();
	void negotiate(IAuthenticationToken authToken);
	void setConnectionListener(IConnectionListener connectionListener);
	IConnectionListener getConnectionListener();
	void setNegotiationListener(INegotiationListener negotiantListener);
	INegotiationListener getNegotiationListener();
	void setAuthenticationCallback(IAuthenticationCallback authenticationCallback);
	IAuthenticationCallback getAuthenticationCallback();
	IConnection getConnection();
}
