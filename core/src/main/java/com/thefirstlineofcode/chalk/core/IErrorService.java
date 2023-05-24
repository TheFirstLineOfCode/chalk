package com.thefirstlineofcode.chalk.core;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.IError;

public interface IErrorService {
	void addErrorListener(IErrorListener listener);
	void removeErrorListener(IErrorListener listener);
	List<IErrorListener> getErrorListeners();
	void send(IError error);
}
