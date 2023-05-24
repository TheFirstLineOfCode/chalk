package com.thefirstlineofcode.chalk.core.stream;

public interface INegotiationListener {
	void before(IStreamNegotiant source);
	void after(IStreamNegotiant source);
	void occurred(NegotiationException exception);
	void done(IStream stream);
}
