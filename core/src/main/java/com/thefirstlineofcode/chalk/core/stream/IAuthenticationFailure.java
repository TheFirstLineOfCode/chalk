package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Failure;

public interface IAuthenticationFailure {
	boolean isRetriable();
	Failure.ErrorCondition getErrorCondition();
	int getFailureCount();
	void retry(IAuthenticationToken authToken);
	void abort();
}