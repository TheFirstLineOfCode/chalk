package com.thefirstlineofcode.chalk.core;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;

public interface IMessageService {
	void send(Message message);
	void addListener(IMessageListener listener);
	void removeListener(IMessageListener listener);
	List<IMessageListener> getListeners();
}
