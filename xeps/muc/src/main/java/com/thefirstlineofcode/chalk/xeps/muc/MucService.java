package com.thefirstlineofcode.chalk.xeps.muc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.thefirstlineofcode.basalt.xeps.delay.Delay;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoItems;
import com.thefirstlineofcode.basalt.xeps.disco.Feature;
import com.thefirstlineofcode.basalt.xeps.disco.Item;
import com.thefirstlineofcode.basalt.xeps.muc.user.Continue;
import com.thefirstlineofcode.basalt.xeps.muc.user.Invite;
import com.thefirstlineofcode.basalt.xeps.muc.user.MucUser;
import com.thefirstlineofcode.basalt.xeps.muc.user.Status;
import com.thefirstlineofcode.basalt.xeps.muc.xconference.XConference;
import com.thefirstlineofcode.basalt.xeps.rsm.Set;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ISyncTask;
import com.thefirstlineofcode.chalk.core.ITask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeAvailabilityStatus;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeAvailabilityStatusEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeNick;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeNickEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.DiscussionHistoryEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Enter;
import com.thefirstlineofcode.chalk.xeps.muc.events.EnterEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Exit;
import com.thefirstlineofcode.chalk.xeps.muc.events.ExitEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Invitation;
import com.thefirstlineofcode.chalk.xeps.muc.events.InvitationEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Kick;
import com.thefirstlineofcode.chalk.xeps.muc.events.KickEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Kicked;
import com.thefirstlineofcode.chalk.xeps.muc.events.KickedEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.PrivateMessageEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessage;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessageEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomSubject;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomSubjectEvent;

public class MucService implements IMucService, IMessageListener, IPresenceListener {
	private IChatServices chatServices;
	private Map<JabberId, IRoom> rooms;
	private List<IRoomListener> roomListeners;
	
	public MucService(IChatServices chatServices) {
		rooms = new HashMap<>();
		roomListeners = new CopyOnWriteArrayList<>();
		this.chatServices = chatServices;
		
		listenMucEvents();
	}
	
	private void listenMucEvents() {
		chatServices.getMessageService().addListener(this);
		chatServices.getPresenceService().addListener(this);
	}

	@Override
	public JabberId[] getMucHosts() throws ErrorException {
		JabberId[] candidates = getMucHostCandidates();
		
		return new MucHostsFinder(candidates).findHosts();
	}
	
	private class MucHostsFinder {
		private JabberId[] candidates;
		
		public MucHostsFinder(JabberId[] candidates) {
			this.candidates = candidates;
		}

		public JabberId[] findHosts() {
			if (candidates.length == 0)
				return new JabberId[0];
			
			MucHostsFinderTask task = new MucHostsFinderTask(candidates);
			chatServices.getTaskService().execute(task);
			
			return task.getMucHosts();
		}
	}
	
	private class MucHostsFinderTask implements ITask<Iq> {
		private JabberId[] candidates;
		private int count;
		private List<JabberId> hosts;
		
		public MucHostsFinderTask(JabberId[] candidates) {
			this.candidates = candidates;
			count = candidates.length;
			hosts = new ArrayList<>(1);
		}

		@Override
		public void trigger(IUnidirectionalStream<Iq> stream) {
			for (JabberId candidate : candidates) {
				Iq iq = new Iq();
				iq.setTo(candidate);
				iq.setObject(new DiscoInfo());
				
				stream.send(iq);
			}
		}

		@Override
		public void processResponse(IUnidirectionalStream<Iq> stream, Iq iq) {
			DiscoInfo discoInfo = iq.getObject();
			if (isMucHost(discoInfo)) {
				hosts.add(iq.getFrom());
			}
			
			synchronized (this) {
				count--;
				
				if (count == 0) {
					notify();
				}
			}
		}
		
		private boolean isMucHost(DiscoInfo discoInfo) {
			for (Feature feature : discoInfo.getFeatures()) {
				if ("http://jabber.org/protocol/muc".equals(feature.getVar()))
					return true;
			}
			
			return false;
		}

		@Override
		public boolean processError(IUnidirectionalStream<Iq> stream, StanzaError error) {
			synchronized (this) {
				count--;
				if (count == 0) {
					notify();
				}
			}
			
			System.out.println(String.format("Disco error: %s.", error.getDefinedCondition()));
			return true;
		}

		@Override
		public boolean processTimeout(IUnidirectionalStream<Iq> stream, Iq iq) {
			synchronized (this) {
				count--;
				if (count == 0) {
					notify();
				}
			}
			
			System.out.println(String.format("Disco timeout. Stanza id: %s.", iq.getId()));
			return true;
		}

		@Override
		public void interrupted() {}
		
		JabberId[] getMucHosts() {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Unexpected thread error.", e);
				}
			}
			
			return hosts.toArray(new JabberId[hosts.size()]);
		}
		
	}

	private JabberId[] getMucHostCandidates() throws ErrorException {
		return chatServices.getTaskService().execute(new ISyncTask<Iq, JabberId[]>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				JabberId host = JabberId.parse(chatServices.getStream().getStreamConfig().getHost());
				
				Iq iq = new Iq();
				iq.setTo(host);
				iq.setObject(new DiscoItems());
				
				stream.send(iq);
			}

			@Override
			public JabberId[] processResult(Iq iq) {
				DiscoItems discoItems = iq.getObject();
				
				if (discoItems.getItems() == null || discoItems.getItems().isEmpty())
					return new JabberId[0];
				
				List<JabberId> candidates = new ArrayList<>();
				for (Item item : discoItems.getItems()) {
					candidates.add(item.getJid());
				}
				
				return candidates.toArray(new JabberId[candidates.size()]);
			}
		});
	}

	@Override
	public int getTotalNumberOfRooms(final JabberId hostJid) throws ErrorException {
		if (hostJid.getNode() != null || hostJid.getResource() != null) {
			throw new IllegalArgumentException("Need a domain only jid.");
		}
		
		return chatServices.getTaskService().execute(new ISyncTask<Iq, Integer>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq();
				iq.setTo(hostJid);
				DiscoItems discoItems = new DiscoItems();
				discoItems.setSet(Set.limit(0));
				iq.setObject(discoItems);
				
				stream.send(iq);
			}

			@Override
			public Integer processResult(Iq iq) {
				DiscoItems discoItems = iq.getObject();
				if (discoItems.getSet() == null)
					return 0;
				
				return discoItems.getSet().getCount();
			}
		});
	}

	@Override
	public IRoom createInstantRoom(JabberId roomJid, String nick) throws ErrorException {
		if (roomJid.getNode() == null || roomJid.getResource() != null) {
			throw new IllegalArgumentException("Room jid must be a bare jid.");
		}
		
		IRoom room = getRoom(roomJid);
		room.create(nick);
		
		return room;
	}

	@Override
	public <T> IRoom createReservedRoom(JabberId roomJid, String nick, IRoomConfigurator configurator)
				throws ErrorException {
		if (roomJid.getNode() == null || roomJid.getResource() != null) {
			throw new IllegalArgumentException("Room jid must be a bare jid.");
		}
		
		IRoom room = getRoom(roomJid);
		room.create(nick, configurator);
		
		return room;
	}

	@Override
	public IRoom getRoom(JabberId roomJid) {
		synchronized (rooms) {
			if (!rooms.containsKey(roomJid)) {
				rooms.put(roomJid, createRoomImpl(roomJid));
			}
		}
		
		return rooms.get(roomJid);
	}
	
	protected IRoom createRoomImpl(JabberId roomJid) {
		return new StandardRoom(chatServices, roomJid);
	}

	@Override
	public void addRoomListener(IRoomListener listener) {
		roomListeners.add(listener);
	}

	@Override
	public void removeRoomListener(IRoomListener listener) {
		roomListeners.remove(listener);
	}

	@Override
	public void received(Message message) {
		if (message.getFrom() == null)
			return;
		
		if (message.getType() == Message.Type.GROUPCHAT) {
			if (!message.getSubjects().isEmpty() && message.getBodies().isEmpty()) {
				processGroupChatSubject(message);
			} else {
				processGroupChatMessage(message);
			}
			
			return;
		}
		
		JabberId contact = message.getFrom();
		if (message.getObject() == null && rooms.containsKey(contact.getBareId())) {
			processGroupChatPrivateMessage(message);
			return;
		}
		
		if (message.getObject() == null)
			return;
		
		if (message.getObject() instanceof MucUser) {
			processMucUserMessage(message);
			
			return;
		}
		
		if (message.getObject() instanceof XConference) {
			processXConferenceMessage(message);
			
			return;
		}
	}

	private void processGroupChatSubject(Message message) {
		RoomSubjectEvent event = new RoomSubjectEvent(message, message.getFrom().getBareId(),
				new RoomSubject(message.getFrom().getResource(), message.getSubject()));
		for (IRoomListener listener : roomListeners) {
			listener.received(event);
		}
	}

	private void processXConferenceMessage(Message message) {
		XConference xConference = message.getObject();
		JabberId roomJid = xConference.getJid();
		JabberId invitor = message.getFrom().getBareId();
		String reason = xConference.getReason();
		String password = xConference.getPassword();
		
		Invitation invitation = new Invitation(invitor, roomJid, reason);
		if (password != null) {
			invitation.setPassword(password);
		}
		
		if (xConference.isContinue()) {
			invitation.setContinue(true);
			invitation.setThread(xConference.getThread());
		}
		
		for (IRoomListener listener : roomListeners) {
			listener.received(new InvitationEvent(message, roomJid, invitation));
		}
	}

	private void processMucUserMessage(Message message) {
		MucUser mucUser = message.getObject();
		
		if (!mucUser.getInvites().isEmpty()) {
			JabberId roomJid = message.getFrom();
			Invite invite = mucUser.getInvites().get(0);
			JabberId invitor = invite.getFrom();
			String reason = invite.getReason();
			String password = mucUser.getPassword();
			Continue continuee = invite.getContinue();
			
			Invitation invitation = new Invitation(invitor, roomJid, reason);
			if (password != null) {
				invitation.setPassword(password);
			}
			
			if (continuee != null) {
				invitation.setContinue(true);
				invitation.setThread(continuee.getThread());
			}
			
			for (IRoomListener listener : roomListeners) {
				listener.received(new InvitationEvent(message, roomJid, invitation));
			}
		}
	}

	private void processGroupChatPrivateMessage(Message message) {
		for (IRoomListener listener : roomListeners) {
			listener.received(new PrivateMessageEvent(message, message.getFrom().getBareId(),
					new RoomMessage(message.getFrom().getResource(), message.getText())));
		}
	}

	private void processGroupChatMessage(Message message) {
		if (message.getObjectProtocol(Delay.class) != null) {
			for (IRoomListener listener : roomListeners) {
				listener.received(new DiscussionHistoryEvent(message, message.getFrom().getBareId(),
						new RoomMessage(message.getFrom().getResource(), message.getText())));
			}
		} else {
			for (IRoomListener listener : roomListeners) {
				listener.received(new RoomMessageEvent(message, message.getFrom().getBareId(),
						new RoomMessage(message.getFrom().getResource(), message.getText())));
			}
		}
	}

	@Override
	public PublicRoom[] getPublicRooms(JabberId hostJid) {
		try {
			return doGetPublicRooms(hostJid);
		} catch (ErrorException e) {
			throw new RuntimeException("Unexpected error.", e);
		}
	}

	private PublicRoom[] doGetPublicRooms(final JabberId hostJid) throws ErrorException {
		return chatServices.getTaskService().execute(new ISyncTask<Iq, PublicRoom[]>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				DiscoItems discoItems = new DiscoItems();
				
				Iq iq = new Iq();				
				iq.setTo(hostJid);
				iq.setObject(discoItems);
				
				stream.send(iq);
			}

			@Override
			public PublicRoom[] processResult(Iq iq) {
				DiscoItems discoItems = iq.getObject();
				if (discoItems.getItems().isEmpty())
					return new PublicRoom[0];
				
				PublicRoom[] publicRooms = new PublicRoom[discoItems.getItems().size()];
				for (int i = 0; i< discoItems.getItems().size(); i++) {
					Item item = discoItems.getItems().get(i);
					PublicRoom publicRoom = new PublicRoom(item.getJid(), item.getName());
					publicRooms[i] = publicRoom;
				}
				
				return publicRooms;
			}
		});
	}

	@Override
	public void received(Presence presence) {
		if (presence.getObject() instanceof MucUser) {
			MucUser mucUser = presence.getObject();
			if (mucUser.getItems().isEmpty())
				return;
			
			RoomEvent<?> event;
			com.thefirstlineofcode.basalt.xeps.muc.user.Item item = mucUser.getItems().get(0);
			if (presence.getType() == null) {
				JabberId roomJid = presence.getFrom().getBareId();
				
				String nick = presence.getFrom().getResource();
				if (!mucUser.getStatuses().contains(new Status(130))) {
					Enter enter = new Enter(nick, item.getAffiliation(), item.getRole());
					if (item.getJid() != null) {
						enter.setJid(item.getJid());
					}
					
					if (mucUser.getStatuses().contains(new Status(110))) {
						enter.setSelf(true);
					}
					
					event = new EnterEvent(presence, presence.getFrom().getBareId(), enter);
				} else {
					ChangeAvailabilityStatus changeAvailabilityStatus = new ChangeAvailabilityStatus(nick, presence);
					if (mucUser.getStatuses().contains(new Status(110))) {
						changeAvailabilityStatus.setSelf(true);
					}
					
					event = new ChangeAvailabilityStatusEvent(presence, roomJid, changeAvailabilityStatus);
				}
			} else if (presence.getType() == Presence.Type.UNAVAILABLE) {
				if (mucUser.getStatuses().contains(new Status(307))) {
					String nick = presence.getFrom().getResource();
					
					if (mucUser.getStatuses().contains(new Status(110))) {
						Kicked kicked = new Kicked(nick, item.getAffiliation(), item.getRole());
						kicked.setActor(item.getActor());
						kicked.setReason(item.getReason());
						event = new KickedEvent(presence, presence.getFrom().getBareId(), kicked);
					} else {
						Kick kick = new Kick(nick, item.getAffiliation(), item.getRole());
						kick.setActor(item.getActor());
						kick.setReason(item.getReason());
						event = new KickEvent(presence, presence.getFrom().getBareId(), kick);
					}
				} else if (item.getNick() != null) {
					ChangeNick changeNick = new ChangeNick(presence.getFrom().getResource(), item.getNick(),
							item.getAffiliation(), item.getRole());
					if (item.getJid() != null) {
						changeNick.setJid(item.getJid());
					}
					
					event = new ChangeNickEvent(presence, presence.getFrom().getBareId(), changeNick);
				} else {
					Exit exit = new Exit(presence.getFrom().getResource(), item.getAffiliation());
					if (item.getJid() != null) {
						exit.setJid(item.getJid());
					}
					
					if (mucUser.getStatuses().contains(new Status(110))) {
						exit.setSelf(true);
					}
					
					event = new ExitEvent(presence, presence.getFrom().getBareId(), exit);					
				}

			} else {
				return;
			}

			for (IRoom room : rooms.values()) {
				if ((room instanceof IRoomListener) && room.getRoomJid().equals(
						presence.getFrom().getBareId())) {
					((IRoomListener)room).received(event);
				}
			}
			
			for (IRoomListener listener : roomListeners) {
				listener.received(event);
			}
		}
	}

}
