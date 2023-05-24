package com.thefirstlineofcode.chalk.im.subscription;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface ISubscriptionService {
	void subscribe(JabberId contact);
	void unsubscribe(JabberId contact);
	void approve(JabberId user);
	void refuse(JabberId user);
	void addSubscriptionListener(ISubscriptionListener listener);
	void removeSubscriptionListener(ISubscriptionListener listener);
	ISubscriptionListener[] getSubscriptionListeners();
}
