package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;

public interface ITask<T extends Stanza> {
	void trigger(IUnidirectionalStream<T> stream);
	void processResponse(IUnidirectionalStream<T> stream, T stanza);
	boolean processError(IUnidirectionalStream<T> stream, StanzaError error);
	boolean processTimeout(IUnidirectionalStream<T> stream, T stanza);
	void interrupted();
}
