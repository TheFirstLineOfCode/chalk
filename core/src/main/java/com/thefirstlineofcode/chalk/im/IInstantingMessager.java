package com.thefirstlineofcode.chalk.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.im.roster.IRosterService;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;
import com.thefirstlineofcode.chalk.im.subscription.ISubscriptionService;

public interface IInstantingMessager {
	IRosterService getRosterService();
	ISubscriptionService getSubscriptionService();
	
	void send(Message message);
	void send(JabberId contact, Message message);
	void send(Presence presence);
	void send(JabberId contact, Presence presence);
	
	void addMessageListener(IMessageListener messageListener);
	void removeMessageListener(IMessageListener messageListener);
	void addPresenceListener(IPresenceListener presenceListener);
	void removePresenceListener(IPresenceListener presenceListener);
}
