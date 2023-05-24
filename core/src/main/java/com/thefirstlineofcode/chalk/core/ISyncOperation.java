package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;

interface ISyncOperation<K extends Stanza, V> {
	void trigger(IUnidirectionalStream<K> stream);
	boolean isErrorOccurred(StanzaError error);
	boolean isResultReceived(K stanza);
	V processResult(K stanza);
}
