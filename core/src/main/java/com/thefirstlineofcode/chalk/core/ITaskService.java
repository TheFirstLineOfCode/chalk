package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface ITaskService {
	void execute(ITask<?> task);
	void setDefaultTimeout(long timeout);
	long getDefaultTimeout();
	void setDefaultTimeoutHandler(ITimeoutHandler timeoutHandler);
	<K extends Stanza, V> V execute(ISyncTask<K, V> task) throws ErrorException;
}
