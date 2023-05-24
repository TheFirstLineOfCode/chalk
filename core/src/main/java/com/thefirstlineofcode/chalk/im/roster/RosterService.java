package com.thefirstlineofcode.chalk.im.roster;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Item;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ITask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.core.stanza.IIqListener;
import com.thefirstlineofcode.chalk.im.roster.RosterError.Reason;

public class RosterService implements IRosterService, IIqListener {
	private static final Logger logger = LoggerFactory.getLogger(RosterService.class);
	
	private IChatServices chatServices;
	private List<IRosterListener> listeners;
	private volatile Roster local;
	
	public RosterService(IChatServices chatServices) {
		this.chatServices = chatServices;
		listeners = new ArrayList<>();
		
		chatServices.getIqService().addListener(Roster.PROTOCOL, this);
		local = new Roster();
	}

	@Override
	public void retrieve() {
		retrieve(15 * 1000);
	}

	@Override
	public void retrieve(final int timeout) {
		chatServices.getTaskService().execute(new RosterTask(new Roster(),
				Reason.ROSTER_RETRIEVE_ERROR,
					Reason.ROSTER_RETRIEVE_TIMEOUT) {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq();
				iq.setObject(new Roster());
				
				stream.send(iq, timeout);
			}
			
			@Override
			public void processResponse(IUnidirectionalStream<Iq> stream, Iq iq) {
				processRetrived(iq);
			}
		});
	}

	@Override
	public void add(final Roster roster) {
		chatServices.getTaskService().execute(new RosterTask(roster,
			Reason.ROSTER_ADD_ERROR, Reason.ROSTER_ADD_TIMEOUT));
	}

	@Override
	public void update(Roster roster) {
		chatServices.getTaskService().execute(new RosterTask(roster,
			Reason.ROSTER_UPDATE_ERROR, Reason.ROSTER_UPDATE_TIMEOUT));
	}

	@Override
	public void delete(Roster roster) {
		chatServices.getTaskService().execute(new RosterTask(roster,
			Reason.ROSTER_DELETE_ERROR, Reason.ROSTER_DELETE_TIMEOUT));
	}

	@Override
	public synchronized void addRosterListener(IRosterListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public synchronized void removeRosterListener(IRosterListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void received(Iq iq) {
		if (iq.getType() == Iq.Type.SET) {
			Roster roster = iq.getObject();
			
			updateLocal(roster);
			
			for (IRosterListener listener : listeners) {
				listener.updated(roster);
			}
			
			return;
		}
	}
	
	private void processRetrived(Iq iq) {
		Roster roster = iq.getObject();
		
		updateLocal(roster);
		
		for (IRosterListener listener : listeners) {
			listener.retrieved(roster);
		}
	}

	private void updateLocal(Roster roster) {
		if (logger.isTraceEnabled()) {
			logger.trace("Update roster local. Items size is {}.", roster.getItems().length);
		}
		
		for (Item item : roster.getItems()) {
			local.addOrUpdate(item);
		}
	}

	private class RosterTask implements ITask<Iq> {
		private Roster roster;
		private Reason errorReason;
		private Reason timeoutReason;
		
		public RosterTask(Roster roster, Reason errorReason, Reason timeoutReason) {
			this.roster = roster;
			this.errorReason = errorReason;
			this.timeoutReason = timeoutReason;
		}

		@Override
		public void trigger(IUnidirectionalStream<Iq> stream) {
			Iq iq = new Iq();
			iq.setType(Iq.Type.SET);
			iq.setObject(roster);
			
			if (logger.isTraceEnabled()) {
				logger.trace("Ready to send roster get.");
			}
			
			stream.send(iq);
		}

		@Override
		public void processResponse(IUnidirectionalStream<Iq> stream, Iq iq) {}

		@Override
		public boolean processError(IUnidirectionalStream<Iq> stream, StanzaError error) {
			for (IRosterListener listener : listeners) {
				listener.occurred(new RosterError(errorReason, error));
			}
			
			return true;
		}

		@Override
		public boolean processTimeout(IUnidirectionalStream<Iq> stream, Iq stanza) {
			for (IRosterListener listener : listeners) {
				listener.occurred(new RosterError(timeoutReason, stanza));
			}
			
			return true;
		}

		@Override
		public void interrupted() {}
		
	}

	@Override
	public Roster getLocal() {
		return local;
	}

	@Override
	public synchronized IRosterListener[] getRosterListeners() {
		return listeners.toArray(new IRosterListener[listeners.size()]);
	}

}
