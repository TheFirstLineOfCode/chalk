package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface ISyncTask<K extends Stanza, V> {
	void trigger(IUnidirectionalStream<K> stream);
	V processResult(K stanza);
}
