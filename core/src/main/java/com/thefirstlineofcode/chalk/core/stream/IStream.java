package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.chalk.core.IErrorListener;
import com.thefirstlineofcode.chalk.core.stanza.IStanzaListener;
import com.thefirstlineofcode.chalk.core.stream.keepalive.IKeepAliveManager;
import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public interface IStream {
	JabberId getJid();
	StreamConfig getStreamConfig();
	
	void send(Stanza stanza);
	void send(IError error);
	
	void close();
	void close(boolean graceful);
	boolean isClosed();
	
	void addStanzaListener(IStanzaListener stanzaListener);
	void removeStanzaListener(IStanzaListener stanzaListener);
	IStanzaListener[] getStanzaListeners();
	
	void addErrorListener(IErrorListener errorListener);
	void removeErrorListener(IErrorListener errorListener);
	IErrorListener[] getErrorListeners();
	
	void addConnectionListener(IConnectionListener connectionListener);
	void removeConnectionListener(IConnectionListener connectionListener);
	IConnectionListener[] getConnectionListeners();
	
	void addStanzaWatcher(IStanzaWatcher stanzaWatcher);
	void removeStanzaWatcher(IStanzaWatcher stanzaWatcher);
	IStanzaWatcher[] getStanzaWatchers();
	
	IOxmFactory getOxmFactory();
	void setOxmFactory(IOxmFactory oxmFactory);
	
	IConnection getConnection();
	
	IKeepAliveManager getKeepAliveManager();
}
