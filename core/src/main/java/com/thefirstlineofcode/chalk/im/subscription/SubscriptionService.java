package com.thefirstlineofcode.chalk.im.subscription;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Item;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ITask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.im.roster.IRosterService;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;
import com.thefirstlineofcode.chalk.im.subscription.SubscriptionError.Reason;

public class SubscriptionService implements ISubscriptionService, IPresenceListener {
	private IChatServices chatServices;
	private List<ISubscriptionListener> listeners;
	private IRosterService rosterService;
	
	public SubscriptionService(IChatServices chatServices, IRosterService rosterService) {
		this.chatServices = chatServices;
		this.rosterService = rosterService;
		this.listeners = new ArrayList<>();
		
		chatServices.getPresenceService().addListener(this);
	}

	@Override
	public void subscribe(final JabberId contact) {
		if (rosterService.getLocal().exists(contact)) {
			ask(contact);
		} else {
			rosterSetAndAsk(contact);
		}
	}
	
	private void rosterSetAndAsk(final JabberId contact) {
		chatServices.getTaskService().execute(new ITask<Iq>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Roster roster = new Roster();
				Item item = new Item();
				item.setJid(contact);
				roster.addOrUpdate(item);
				
				Iq iq = new Iq();
				iq.setType(Iq.Type.SET);
				iq.setObject(roster);
				
				stream.send(iq);
			}

			@Override
			public void processResponse(IUnidirectionalStream<Iq> stream, Iq iq) {
				ask(contact);
			}

			@Override
			public boolean processError(IUnidirectionalStream<Iq> stream, StanzaError error) {
				for (ISubscriptionListener listener : listeners) {
					listener.occurred(new SubscriptionError(Reason.ROSTER_SET_ERROR, error));
				}
				
				return true;
			}

			@Override
			public boolean processTimeout(IUnidirectionalStream<Iq> stream, Iq iq) {
				for (ISubscriptionListener listener : listeners) {
					listener.occurred(new SubscriptionError(Reason.ROSTER_SET_TIMEOUT, iq));
				}
				
				return true;
			}

			@Override
			public void interrupted() {}
			
		});		
	}

	private void ask(JabberId contact) {
		Presence subscribe = new Presence();
		subscribe.setTo(contact);
		subscribe.setType(Presence.Type.SUBSCRIBE);
		
		chatServices.getPresenceService().send(subscribe);
	}

	@Override
	public void unsubscribe(JabberId contact) {
		Presence unsubscribe = new Presence();
		unsubscribe.setTo(contact);
		unsubscribe.setType(Presence.Type.UNSUBSCRIBE);
		
		chatServices.getPresenceService().send(unsubscribe);
	}

	@Override
	public void approve(JabberId user) {
		Presence subscribed = new Presence();
		subscribed.setTo(user);
		subscribed.setType(Presence.Type.SUBSCRIBED);
		
		chatServices.getPresenceService().send(subscribed);
	}

	@Override
	public void refuse(JabberId user) {
		Presence unsubscribed = new Presence();
		unsubscribed.setTo(user);
		unsubscribed.setType(Presence.Type.UNSUBSCRIBED);
		
		chatServices.getPresenceService().send(unsubscribed);
	}

	@Override
	public synchronized void addSubscriptionListener(ISubscriptionListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public synchronized void removeSubscriptionListener(ISubscriptionListener listener) {
		listeners.remove(listener);
	}
	
	public synchronized void caught(SubscriptionError error) {
		for (ISubscriptionListener listener : listeners) {
			listener.occurred(error);
		}
	}

	@Override
	public void received(Presence presence) {
		Presence.Type type = presence.getType();
		if (type == null ||
				type == Presence.Type.UNAVAILABLE ||
					type == Presence.Type.PROBE)
			return;
		
		if (type == Presence.Type.SUBSCRIBE) {
			synchronized (this) {
				for (ISubscriptionListener listener : listeners) {
					listener.asked(presence.getFrom());
				}
			}
		} else if (type == Presence.Type.UNSUBSCRIBE) {
			Presence affirm = new Presence();
			affirm.setTo(presence.getFrom().getBareId());
			affirm.setType(Presence.Type.UNSUBSCRIBED);
			
			chatServices.getPresenceService().send(affirm);
			
			synchronized (this) {
				for (ISubscriptionListener listener : listeners) {
					listener.revoked(presence.getFrom());
				}
			}
		} else if (type == Presence.Type.SUBSCRIBED) {
			Presence affirm = new Presence();
			affirm.setTo(presence.getFrom());
			affirm.setType(Presence.Type.SUBSCRIBE);
			
			chatServices.getPresenceService().send(affirm);
			
			synchronized (this) {
				for (ISubscriptionListener listener : listeners) {
					listener.approved(presence.getFrom());
				}
			}
		} else { // type == Presence.Type.UNSUBSCRIBED
			Presence affirm = new Presence();
			affirm.setTo(JabberId.parse(presence.getFrom().getBareIdString()));
			affirm.setType(Presence.Type.UNSUBSCRIBE);
			
			chatServices.getPresenceService().send(affirm);
			
			synchronized (this) {
				for (ISubscriptionListener listener : listeners) {
					listener.refused(presence.getFrom());
				}
			}
		}
	}

	@Override
	public synchronized ISubscriptionListener[] getSubscriptionListeners() {
		return listeners.toArray(new ISubscriptionListener[listeners.size()]);
	}

	
}
