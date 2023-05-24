package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public interface IStreamNegotiant extends IConnectionListener {
	void negotiate(INegotiationContext context) throws ConnectionException, NegotiationException;
}
