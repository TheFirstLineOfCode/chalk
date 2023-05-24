package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.IError;

public interface IErrorListener {
	void occurred(IError error);
}
