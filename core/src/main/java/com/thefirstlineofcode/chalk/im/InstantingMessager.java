package com.thefirstlineofcode.chalk.im;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.im.roster.IRosterService;
import com.thefirstlineofcode.chalk.im.roster.RosterService;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;
import com.thefirstlineofcode.chalk.im.subscription.ISubscriptionService;
import com.thefirstlineofcode.chalk.im.subscription.SubscriptionService;

public class InstantingMessager implements IInstantingMessager,
		IPresenceListener, IMessageListener {
	private static final String CLASS_NAME_BASALT_XEPS_DELAY = "com.thefirstlineofcode.basalt.xeps.delay.Delay";
	
	private IChatServices chatServices;
	private ISubscriptionService subscriptionService;
	private IRosterService rosterService;
	private List<IPresenceListener> presenceListeners;
	private List<IMessageListener> messageListeners;
	
	public InstantingMessager(IChatServices chatServices) {
		this.chatServices = chatServices;
		rosterService = new RosterService(chatServices);
		subscriptionService = new SubscriptionService(chatServices, rosterService);
		presenceListeners = new CopyOnWriteArrayList<>();
		messageListeners = new CopyOnWriteArrayList<>();
		
		chatServices.getPresenceService().addListener(this);
		chatServices.getMessageService().addListener(this);
	}

	@Override
	public IRosterService getRosterService() {
		return rosterService;
	}

	@Override
	public ISubscriptionService getSubscriptionService() {
		return subscriptionService;
	}

	@Override
	public void send(Message message) {
		chatServices.getMessageService().send(message);
	}

	@Override
	public void send(Presence presence) {
		chatServices.getPresenceService().send(presence);
	}

	@Override
	public void addMessageListener(IMessageListener messageListener) {
		messageListeners.add(messageListener);
	}

	@Override
	public void addPresenceListener(IPresenceListener presenceListener) {
		presenceListeners.add(presenceListener);
	}

	@Override
	public void received(Presence presence) {
		if (presence.getObject() != null)
			return;
		
		if (presence.getType() == null ||
			presence.getType() == Presence.Type.UNAVAILABLE ||
				presence.getType() == Presence.Type.PROBE) {
			for (IPresenceListener listener : presenceListeners) {
				listener.received(presence);
			}
		}
	}

	@Override
	public void removeMessageListener(IMessageListener messageListener) {
		messageListeners.remove(messageListener);
	}

	@Override
	public void removePresenceListener(IPresenceListener presenceListener) {
		presenceListeners.remove(presenceListener);
	}

	@Override
	public void received(Message message) {
		for (Object object : message.getObjects()) {
			if (CLASS_NAME_BASALT_XEPS_DELAY.equals(object.getClass().getName())) {
				continue;
			}
			
			return;
		}
		
		if (message.getType() == Message.Type.GROUPCHAT)
			return;
		
		if (message.getFrom() != null && !rosterService.getLocal().exists(
				message.getFrom().getBareId()))
			return;
		
		for (IMessageListener listener : messageListeners) {
			listener.received(message);
		}
	}

	@Override
	public void send(JabberId contact, Message message) {
		message.setTo(contact);
		send(message);
	}

	@Override
	public void send(JabberId contact, Presence presence) {
		presence.setTo(contact);
		send(presence);
	}

}
