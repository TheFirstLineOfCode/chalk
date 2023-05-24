package com.thefirstlineofcode.chalk.im.subscription;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface ISubscriptionListener {
	void asked(JabberId user);
	void approved(JabberId contact);
	void refused(JabberId contact);
	void revoked(JabberId user);
	void occurred(SubscriptionError error);
}
