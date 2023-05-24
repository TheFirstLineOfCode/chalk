package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;

public abstract class TaskAdapter<T extends Stanza> implements ITask<T> {

	@Override
	public boolean processError(IUnidirectionalStream<T> stream, StanzaError error) {
		return false;
	}

	@Override
	public boolean processTimeout(IUnidirectionalStream<T> stream, T stanza) {
		return false;
	}

	@Override
	public void interrupted() {}

}
