package com.thefirstlineofcode.chalk.xeps.muc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xeps.disco.Feature;
import com.thefirstlineofcode.basalt.xeps.disco.Identity;
import com.thefirstlineofcode.basalt.xeps.muc.ExtendedRoomInfo;
import com.thefirstlineofcode.basalt.xeps.muc.Muc;
import com.thefirstlineofcode.basalt.xeps.muc.RoomInfo;
import com.thefirstlineofcode.basalt.xeps.muc.admin.MucAdmin;
import com.thefirstlineofcode.basalt.xeps.muc.owner.MucOwner;
import com.thefirstlineofcode.basalt.xeps.muc.user.Invite;
import com.thefirstlineofcode.basalt.xeps.muc.user.Item;
import com.thefirstlineofcode.basalt.xeps.muc.user.MucUser;
import com.thefirstlineofcode.basalt.xeps.muc.user.Status;
import com.thefirstlineofcode.basalt.xeps.muc.xconference.XConference;
import com.thefirstlineofcode.basalt.xeps.xdata.Field;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq.Type;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ISyncPresenceOperation;
import com.thefirstlineofcode.chalk.core.ISyncTask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.core.SyncOperationTemplate;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeNick;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeNickEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Enter;
import com.thefirstlineofcode.chalk.xeps.muc.events.EnterEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Exit;
import com.thefirstlineofcode.chalk.xeps.muc.events.ExitEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;

class StandardRoom implements IRoom, IRoomListener {
	private IChatServices chatServices;
	private JabberId roomJid;
	private String nick;
	
	private Map<String, Occupant> occupants;
	
	public StandardRoom(IChatServices chatServices, JabberId roomJid) {
		this.chatServices = chatServices;
		this.roomJid = roomJid;
		occupants = new HashMap<>();
	}
	
	@Override
	public JabberId getRoomJid() {
		return roomJid;
	}
	
	@Override
	public String create(String nick) throws ErrorException {
		JabberId owner = createRoom(roomJid, nick);
		configureInstantRoom(roomJid);
		this.nick = owner.getResource();
		
		return this.nick;
	}
	
	private void configureInstantRoom(final JabberId roomJid) throws ErrorException {
		chatServices.getTaskService().execute(new ISyncTask<Iq, Void>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq(Type.SET);
				iq.setTo(roomJid);
				
				MucOwner mucOwner = new MucOwner();
				XData xData = new XData(XData.Type.SUBMIT);
				mucOwner.setXData(xData);
				
				iq.setObject(mucOwner);
				
				stream.send(iq);
			}

			@Override
			public Void processResult(Iq result) {
				return null;
			}
		});
	}
	
	private JabberId createRoom(final JabberId roomJid, final String nick) throws ErrorException {
		SyncOperationTemplate<Presence, JabberId> template = new SyncOperationTemplate<>(chatServices);
		return template.execute(new ISyncPresenceOperation<JabberId>() {
			@Override
			public void trigger(IUnidirectionalStream<Presence> stream) {
				Presence presence = new Presence();
				presence.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
				presence.setObject(new Muc());
				
				stream.send(presence);
			}

			@Override
			public boolean isErrorOccurred(StanzaError error) {
				if (roomJid.equals(error.getFrom()))
					return true;
				
				return false;
			}

			@Override
			public boolean isResultReceived(Presence presence) {
				if (presence.getFrom() == null)
					return false;
				
				if (roomJid.getBareId().equals(presence.getFrom().getBareId()) &&
						(presence.getObject() instanceof MucUser)) {
					MucUser mucUser = presence.getObject();
					
					if (mucUser.getStatuses().contains(new Status(201)) &&
							mucUser.getStatuses().contains(new Status(110))) {
						return true;
					}
					
					// throw new RuntimeException(String.format("Error status code. Has room %s created already?", roomJid));
					return false;
				}
				
				return false;
			}

			@Override
			public JabberId processResult(Presence presence) {
				return presence.getFrom();
			}
			
		});
	}
	
	@Override
	public String create(String nick, IRoomConfigurator configurator) throws ErrorException {
		JabberId owner = createRoom(roomJid, nick);
		this.nick = owner.getResource();
		configure(configurator);
		
		return this.nick;
	}

	@Override
	public String enter(String nick) throws ErrorException {
		return enter(nick, null);
	}

	@Override
	public String enter(final String nick, final String password) throws ErrorException {
		final JabberId newOccupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), nick);
		SyncOperationTemplate<Presence, String> template = new SyncOperationTemplate<>(chatServices);
		this.nick = template.execute(new ISyncPresenceOperation<String>() {

			@Override
			public void trigger(IUnidirectionalStream<Presence> stream) {
				Muc muc = new Muc();
				if (password != null) {
					muc.setPassword(password);
				}
				
				Presence presence = new Presence();
				presence.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
				presence.setObject(muc);
				
				stream.send(presence);
			}

			@Override
			public boolean isErrorOccurred(StanzaError error) {
				return newOccupantJid.equals(error.getFrom());
			}

			@Override
			public boolean isResultReceived(Presence presence) {
				if (presence.getFrom() != null &&
						presence.getFrom().getBareId().equals(roomJid) &&
							presence.getType() == null) {
					if (presence.getObject() instanceof MucUser) {
						MucUser mucUser = presence.getObject();
						if (mucUser.getStatuses().contains(new Status(110)))
							return true;
					}
				}
				
				return false;
			}

			@Override
			public String processResult(Presence presence) {
				return presence.getFrom().getResource();
			}
		});
		
		return this.nick;
	}
	
	@Override
	public void exit() {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Not in the room.");
		}
		
		try {
			doExit();
		} catch (ErrorException e) {
			throw new RuntimeException("Unknown error.", e);
		}
	}

	private void doExit() throws ErrorException {
		SyncOperationTemplate<Presence, Void> template = new SyncOperationTemplate<>(chatServices);
		template.execute(new ISyncPresenceOperation<Void>() {

			@Override
			public void trigger(IUnidirectionalStream<Presence> stream) {
				Presence presence = new Presence(Presence.Type.UNAVAILABLE);
				presence.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
				stream.send(presence);
			}

			@Override
			public boolean isErrorOccurred(StanzaError error) {
				return false;
			}

			@Override
			public boolean isResultReceived(Presence presence) {
				if (presence.getType() != Presence.Type.UNAVAILABLE ||
						presence.getFrom() == null ||
						!presence.getFrom().equals(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick))) {
					return false;
				}
				
				if (presence.getObject() instanceof MucUser) {
					MucUser mucUser = presence.getObject();
					return mucUser.getStatuses().contains(new Status(110));
				}
				
				return false;
			}

			@Override
			public Void processResult(Presence stanza) {
				return null;
			}
		});
	}

	@Override
	public <T> void configure(IRoomConfigurator configurator) throws ErrorException {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before configuring it.");
		}
		
		XData form = chatServices.getTaskService().execute(new ISyncTask<Iq, XData>() {
			
			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq(Iq.Type.GET);
				iq.setTo(roomJid);
				iq.setObject(new MucOwner());
				
				stream.send(iq);
			}

			@Override
			public XData processResult(Iq iq) {
				MucOwner mucOwner = iq.getObject();				
				return mucOwner.getXData();
			}
			
		});
		
		XData submit = configurator.configure(form);
		chatServices.getTaskService().execute(new ConfigureRoomTask(submit));
	}
	
	private class ConfigureRoomTask implements ISyncTask<Iq, Void> {
		private XData submit;
		
		public ConfigureRoomTask(XData submit) {
			this.submit = submit;
		}

		@Override
		public void trigger(IUnidirectionalStream<Iq> stream) {
			Iq iq = new Iq(Iq.Type.SET);
			iq.setTo(roomJid);
			
			MucOwner mucOwner = new MucOwner();
			if (submit.getType() != XData.Type.SUBMIT) {
				submit.setType(XData.Type.SUBMIT);
			}
			mucOwner.setXData(submit);
			
			iq.setObject(mucOwner);
			
			stream.send(iq);
		}

		@Override
		public Void processResult(Iq iq) {
			return null;
		}
		
	}

	@Override
	public RoomInfo getRoomInfo() throws ErrorException {
		return chatServices.getTaskService().execute(new ISyncTask<Iq, RoomInfo>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq();
				iq.setTo(roomJid);
				iq.setObject(new DiscoInfo());
				
				stream.send(iq);
			}

			@Override
			public RoomInfo processResult(Iq iq) {
				DiscoInfo discoInfo = iq.getObject();
				
				boolean extendedRoomInfo = isExtendedRoomInfo(discoInfo);
				RoomInfo roomInfo;
				if (extendedRoomInfo) {
					roomInfo = new ExtendedRoomInfo();
				} else {
					roomInfo = new RoomInfo();
				}
				
				extractRoomInfo(discoInfo, roomInfo);
				
				if (extendedRoomInfo) {
					extractExtendedRoomInfo(discoInfo, (ExtendedRoomInfo)roomInfo);
				}
				
				return roomInfo;
			}

			private void extractExtendedRoomInfo(DiscoInfo discoInfo, ExtendedRoomInfo roomInfo) {
				for (Field field : discoInfo.getXData().getFields()) {
					if ("muc#maxhistoryfetch".equals(field.getVar())) {
						String maxHistoryFetch = getFieldValue(field);
						if (maxHistoryFetch == null)
							continue;
						
						roomInfo.setMaxHistoryFetch(Integer.parseInt(maxHistoryFetch));
					} else if ("muc#roominfo_contactjid".equals(field.getVar())) {
						roomInfo.setContactJid(getContactJid(field));
					} else if ("muc#roominfo_description".equals(field.getVar())) {
						roomInfo.setRoomDesc(getFieldValue(field));
					} else if ("muc#roominfo_lang".equals(field.getVar())) {
						roomInfo.setLang(getFieldValue(field));
					} else if ("muc#roominfo_ldapgroup".equals(field.getVar())) {
						roomInfo.setLdapGroup(getFieldValue(field));
					} else if ("muc#roominfo_logs".equals(field.getVar())) {
						roomInfo.setLogs(getFieldValue(field));
					} else if ("muc#roominfo_occupants".equals(field.getVar())) {
						String occupants = getFieldValue(field);
						if (occupants == null)
							continue;
						
						roomInfo.setOccupants(Integer.parseInt(occupants));
					} else if ("muc#roominfo_subject".equals(field.getVar())) {
						roomInfo.setSubject(getFieldValue(field));
					} else if ("muc#roominfo_subjectmod".equals(field.getVar())) {
						String subjectMod = getFieldValue(field);
						if (subjectMod == null)
							continue;
						
						roomInfo.setSubjectMod(Boolean.parseBoolean(subjectMod));
					} else {
						continue;
					}
				}
			}

			private JabberId[] getContactJid(Field field) {
				if (field.getValues().isEmpty())
					return new JabberId[0];
				
				List<JabberId> contactJids = new ArrayList<>();
				for (String contactJid : field.getValues()) {
					contactJids.add(JabberId.parse(contactJid));
				}
				
				return contactJids.toArray(new JabberId[contactJids.size()]);
			}

			private String getFieldValue(Field field) {
				if (field.getValues().isEmpty())
					return null;
				
				return field.getValues().get(0);
			}

			private void extractRoomInfo(DiscoInfo discoInfo, RoomInfo roomInfo) {
				Identity identity = null;
				for (int i = 0; i < discoInfo.getIdentities().size(); i++) {
					identity = discoInfo.getIdentities().get(i);
					if (identity.getCategory().equals("conference") && identity.getType().equals("text")) {
						break;
					}
				}
				
				roomInfo.setRoomName(identity.getName());
				for (Feature feature : discoInfo.getFeatures()) {
					if ("muc_hidden".equals(feature.getVar())) {
						roomInfo.setPublic(false);
					} else if ("muc_membersonly".equals(feature.getVar())) {
						roomInfo.setMembersOnly(true);
					} else if ("muc_moderated".equals(feature.getVar())) {
						roomInfo.setModerated(true);
					} else if ("muc_nonanonymous".equals(feature.getVar())) {
						roomInfo.setNonAnonymous(true);
					} else if ("muc_open".equals(feature.getVar())) {
						roomInfo.setMembersOnly(false);
					} else if ("muc_passwordprotected".equals(feature.getVar())) {
						roomInfo.setPasswordProtected(true);
					} else if ("muc_persistent".equals(feature.getVar())) {
						roomInfo.setPersistent(true);
					} else if ("muc_public".equals(feature.getVar())) {
						roomInfo.setPublic(true);
					} else if ("muc_semianonymous".equals(feature.getVar())) {
						roomInfo.setSemiAnonymous(true);
					} else if ("muc_temporary".equals(feature.getVar())) {
						roomInfo.setPersistent(false);
					} else if ("muc_unmoderated".equals(feature.getVar())) {
						roomInfo.setModerated(false);
					} else if ("muc_unsecured".equals(feature.getVar())) {
						roomInfo.setPasswordProtected(false);
					}
				}
			}

			private boolean isExtendedRoomInfo(DiscoInfo discoInfo) {
				if (discoInfo.getXData() == null)
					return false;
				
				for (Field field : discoInfo.getXData().getFields()) {
					if (Field.Type.HIDDEN.equals(field.getType()) &&
							"FORM_TYPE".equals(field.getVar())) {
						if (!field.getValues().isEmpty()) {
							if ("http://jabber.org/protocol/muc#roominfo".equals(field.getValues().get(0))) {
								return true;
							}
						}
					}
				}
				
				return false;
			}
			
		});
	}

	@Override
	public String discoReservedNick() {
		try {
			return doGetReservedNick();
		} catch (ErrorException e) {
			throw new RuntimeException("unexpected error", e);
		}
	}

	private String doGetReservedNick() throws ErrorException {
		return chatServices.getTaskService().execute(new ISyncTask<Iq, String>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				DiscoInfo discoInfo = new DiscoInfo();
				discoInfo.setNode("x-roomuser-item");
				
				Iq iq = new Iq();
				iq.setTo(roomJid);
				iq.setObject(discoInfo);
				
				stream.send(iq);
			}

			@Override
			public String processResult(Iq iq) {
				DiscoInfo discoInfo = iq.getObject();
				if (discoInfo.getIdentities().isEmpty())
					return null;
				
				return discoInfo.getIdentities().get(0).getName();
			}
		});
	}

	@Override
	public void invite(JabberId invitee, String reason) throws ErrorException {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before inviting another user to a room.");
		}
		
		if (getRoomInfo().isMembersOnly()) {
			sendMediatedInvitation(invitee, reason);
		} else {
			sendDirectInvitation(invitee, reason);
		}
	}

	public void sendDirectInvitation(JabberId invitee, String reason) throws ErrorException {
		XConference xConference = new XConference();
		xConference.setJid(roomJid);
		xConference.setReason(reason);
		
		Message message = new Message();
		message.setFrom(chatServices.getStream().getJid());
		message.setTo(invitee);
		message.setObject(xConference);
		
		chatServices.getMessageService().send(message);
	}

	public void sendMediatedInvitation(JabberId invitee, String reason) {
		Invite invite = new Invite();
		invite.setTo(invitee);
		invite.setReason(reason);
		
		MucUser mucUser = new MucUser();
		mucUser.getInvites().add(invite);
		
		Message message = new Message();
		message.setTo(roomJid);
		message.setObject(mucUser);
		
		chatServices.getMessageService().send(message);		
	}

	@Override
	public boolean isEntered() {
		return nick != null;
	}

	private boolean waitToEnterRoom() {
		if (nick == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		return nick != null;
	}

	@Override
	public String getNick() {
		return nick;
	}

	@Override
	public void received(RoomEvent<?> event) {
		if (event instanceof EnterEvent) {
			synchronized (this) {
				Enter enter = ((EnterEvent)event).getEventObject();
				Occupant occupant = occupants.get(enter.getNick());
				
				if (occupant == null) {
					occupant = new Occupant(enter.getNick(), enter.getAffiliation(), enter.getRole());
					
					if (enter.getJid() != null) {
						occupant.setJid(enter.getJid());
					}
				}
				
				occupant.addSession();
				occupants.put(occupant.getNick(), occupant);	
				
				enter.setSessions(occupant.getSessions());
			}

		} else if (event instanceof ExitEvent){
			synchronized (this) {
				Exit exit = ((ExitEvent)event).getEventObject();
				Occupant occupant = occupants.get(exit.getNick());
				
				if (occupant == null) {
					return;
				}
				
				occupant.removeSession();
				
				if (occupant.getSessions() == 0) {
					occupants.remove(exit.getNick());
				}
				
				exit.setSessions(occupant.getSessions());
			}
		} else if (event instanceof ChangeNickEvent) {
			synchronized (this) {
				ChangeNick changeNick = ((ChangeNickEvent)event).getEventObject();
				Occupant occupant = occupants.get(changeNick.getOldNick());
				
				if (occupant == null) {
					return;
				}
				
				occupant.removeSession();
				
				if (occupant.getSessions() == 0) {
					occupants.remove(changeNick.getOldNick());
				}
				
				changeNick.setOldNickSessions(occupant.getSessions());
			}
		}
	}

	@Override
	public synchronized Occupant[] getOccupants() {
		Collection<Occupant> cOccupants = occupants.values();
		return cOccupants.toArray(new Occupant[cOccupants.size()]);
	}

	@Override
	public void setSubject(String subject) {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before setting room subject.");
		}
		
		Message message = new Message();
		message.setType(Message.Type.GROUPCHAT);
		message.setTo(roomJid);
		message.setSubject(subject);
		
		chatServices.getMessageService().send(message);
	}

	@Override
	public void send(Message message) {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before sending groupchat message.");
		}
		
		if (message.getType() != Message.Type.GROUPCHAT) {
			message.setType(Message.Type.GROUPCHAT);
		}
		message.setTo(roomJid);
		
		chatServices.getMessageService().send(message);
	}

	@Override
	public void send(String nick, Message message) {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before sending groupchat private message.");
		}
		
		message.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
		
		chatServices.getMessageService().send(message);
	}

	@Override
	public String changeNick(final String newNick) throws ErrorException {
		if (!waitToEnterRoom()) {
			throw new IllegalStateException("Enter the room before changing nick.");
		}
		
		final JabberId newOccupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), newNick);
		SyncOperationTemplate<Presence, String> template = new SyncOperationTemplate<>(chatServices);
		this.nick = template.execute(new ISyncPresenceOperation<String>() {

			@Override
			public void trigger(IUnidirectionalStream<Presence> stream) {
				Presence presence = new Presence();
				presence.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), newNick));
				
				stream.send(presence);
			}

			@Override
			public boolean isErrorOccurred(StanzaError error) {
				return newOccupantJid.equals(error.getFrom());
			}

			@Override
			public boolean isResultReceived(Presence presence) {
				if (presence.getFrom() != null &&
						presence.getFrom().getBareId().equals(roomJid) &&
							presence.getType() == Presence.Type.UNAVAILABLE) {
					if (presence.getObject() instanceof MucUser) {
						MucUser mucUser = presence.getObject();
						if (mucUser.getStatuses().contains(new Status(110)) &&
								mucUser.getStatuses().contains(new Status(303)))
							return true;
					}
				}
				
				return false;
			}

			@Override
			public String processResult(Presence presence) {
				return presence.getFrom().getResource();
			}
		});
		
		return this.nick;
	}

	@Override
	public synchronized Occupant getOccupant(String nick) {
		return occupants.get(nick);
	}

	@Override
	public void send(Presence presence) {
		presence.setFrom(chatServices.getStream().getJid());
		presence.setTo(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
		
		chatServices.getPresenceService().send(presence);
	}
	
	@Override
	public void kick(String nick) throws ErrorException {
		kick(nick, null);
	}

	@Override
	public void kick(final String nick, final String reason) throws ErrorException {
		chatServices.getTaskService().execute(new ISyncTask<Iq, Void>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq(Iq.Type.SET);
				iq.setTo(roomJid);
				
				MucAdmin mucAdmin = new MucAdmin();
				Item item = new Item();
				item.setNick(nick);
				if (reason != null) {
					item.setReason(reason);
				}
				mucAdmin.getItems().add(item);
				
				iq.setObject(mucAdmin);
				
				stream.send(iq);
			}

			@Override
			public Void processResult(Iq iq) {
				return null;
			}
			
		});
	}

}
