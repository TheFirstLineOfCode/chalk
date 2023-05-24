package com.thefirstlineofcode.chalk.demo.clients;

import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig.WhoIs;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.demo.Demo;
import com.thefirstlineofcode.chalk.xeps.muc.IRoom;
import com.thefirstlineofcode.chalk.xeps.muc.StandardRoomConfigurator;
import com.thefirstlineofcode.chalk.xeps.muc.events.EnterEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Invitation;
import com.thefirstlineofcode.chalk.xeps.muc.events.InvitationEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessageEvent;

public class AgilestPad extends StandardClient {

	public AgilestPad(Demo demo) {
		super(demo, "Agilest/pad");
	}

	@Override
	protected void configureStreamConfig(StandardStreamConfig streamConfig) {
		streamConfig.setResource("pad");
		streamConfig.setTlsPreferred(true);
	}

	@Override
	protected String[] getUserNameAndPassword() {
		return new String[] {"agilest", "a_good_guy"};
	}
	
	@Override
	public void approved(JabberId contact) {
		super.approved(contact);
		
		setPresenceAndSendMessageToDongger();
	}

	private void setPresenceAndSendMessageToDongger() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Presence presence = new Presence();
		presence.setShow(Presence.Show.CHAT);
		presence.setPriority(64);
		
		im.send(presence);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		presence = new Presence();
		presence.setShow(Presence.Show.AWAY);
		presence.setTo(BARE_JID_DONGGER);
		
		im.send(presence);
		
		Message message = new Message("Hello, Dongger!");
		
		message.setTo(JID_DONGGER_HOME);
		
		im.send(message);
	}
	
	@Override
	public void received(Message message) {
		super.received(message);
		
		demo.startClient(this.getClass(), SmartSheepMobile.class);
		demo.startClient(this.getClass(), JellyMobile.class);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			IRoom room = muc.createReservedRoom(new JabberId("first_room_of_agilest", mucHost.getDomain()), "agilest",
					new StandardRoomConfigurator() {
						
						@Override
						protected RoomConfig configure(RoomConfig roomConfig) {
							roomConfig.setRoomName("agilest's first room");
							roomConfig.setRoomDesc("Hope you have happy hours here!");
							roomConfig.setMembersOnly(true);
							roomConfig.setAllowInvites(true);
							roomConfig.setPasswordProtectedRoom(true);
							roomConfig.setRoomSecret("simple");
							roomConfig.getGetMemberList().setParticipant(false);
							roomConfig.getGetMemberList().setVisitor(false);
							roomConfig.setWhoIs(WhoIs.MODERATORS);
							roomConfig.setModeratedRoom(true);
							
							return roomConfig;
						}
					});
			
			room.invite(BARE_JID_SMARTSHEEP, "Let's discuss our plan.");
		} catch (ErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void received(RoomEvent<?> event) {
		super.received(event);
		
		if (event instanceof InvitationEvent) {
			Invitation invitation = (Invitation)event.getEventObject();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				joinRoom(invitation);
			} catch (ErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (event instanceof EnterEvent) {
			EnterEvent enterEvent = (EnterEvent)event;
			if ("first_room_of_agilest".equals(enterEvent.getRoomJid().getNode()) &&
					enterEvent.getEventObject().getNick().equals("smartsheep")) {
				IRoom room = muc.getRoom(new JabberId("first_room_of_agilest", mucHost.getDomain()));
				room.send(new Message("Hello, Smartsheep!"));
			}
		} else if (event instanceof RoomMessageEvent) {
			RoomMessageEvent messageEvent = ((RoomMessageEvent)event);
			if ("first_room_of_agilest".equals(messageEvent.getRoomJid().getNode()) &&
					"smartsheep".equals(messageEvent.getEventObject().getNick()) &&
					"Hello, Agilest!".equals(messageEvent.getEventObject().getMessage())) {
				IRoom room = muc.getRoom(messageEvent.getRoomJid());
				room.setSubject("Let's discuss our plan.");
				room.send(new Message("Waiting for a minute. I will invite your brother to join the room."));
				
				try {
					room.invite(BARE_JID_DONGGER, "Let's discuss our plan.");
				} catch (ErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} if ("room0605".equals(messageEvent.getRoomJid().getNode()) &&
					"smartsheep".equals(messageEvent.getEventObject().getNick()) &&
						"Hi, guys!".equals(messageEvent.getEventObject().getMessage())) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				IRoom room = muc.getRoom(messageEvent.getRoomJid());
				room.exit();
				
				im.send(JID_DONGGER_HOME, new Message("I'll leave for a while."));
				
			} else if ("first_room_of_agilest".equals(messageEvent.getRoomJid().getNode()) &&
					"dongger".equals(messageEvent.getEventObject().getNick())) {
				IRoom room = muc.getRoom(messageEvent.getRoomJid());
				room.send(new Message("Hello, Dongger!"));
			}
		}
	}

}
