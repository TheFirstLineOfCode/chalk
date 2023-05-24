package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.IError;

public interface IErrorHandler {
	void process(IError error);
}
