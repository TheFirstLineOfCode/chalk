package com.thefirstlineofcode.chalk.demo.clients;

import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.demo.Demo;
import com.thefirstlineofcode.chalk.xeps.muc.IRoom;
import com.thefirstlineofcode.chalk.xeps.muc.StandardRoomConfigurator;
import com.thefirstlineofcode.chalk.xeps.muc.events.Enter;
import com.thefirstlineofcode.chalk.xeps.muc.events.EnterEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Invitation;
import com.thefirstlineofcode.chalk.xeps.muc.events.InvitationEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.PrivateMessageEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessage;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessageEvent;

public class SmartSheepMobile extends StandardClient {

	public SmartSheepMobile(Demo demo) {
		super(demo, "SmartSheep/mobile");
	}

	@Override
	protected void configureStreamConfig(StandardStreamConfig streamConfig) {
		streamConfig.setResource("mobile");
		streamConfig.setTlsPreferred(true);
	}

	@Override
	protected String[] getUserNameAndPassword() {
		return new String[] {"smartsheep", "a_pretty_girl"};
	}
	
	@Override
	public void received(RoomEvent<?> event) {
		super.received(event);
		
		if (event instanceof InvitationEvent) {
			
			try {
				Invitation invitation = ((InvitationEvent)event).getEventObject();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				joinRoom(invitation);
			} catch (ErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		} else if (event instanceof RoomMessageEvent) {
			RoomMessageEvent messageEvent = ((RoomMessageEvent)event);
			if ("first_room_of_agilest".equals(messageEvent.getRoomJid().getNode()) &&
					"Hello, Smartsheep!".equals(messageEvent.getEventObject().getMessage())) {
				IRoom room = muc.getRoom(messageEvent.getRoomJid());
				room.send(new Message("Hello, Agilest!"));
			}
		} else if (event instanceof EnterEvent) {
			Enter enter = ((EnterEvent)event).getEventObject();
			if ("jelly".equals(enter.getNick())) {
				IRoom room = muc.getRoom(event.getRoomJid());
				try {
					room.kick("jelly", "I'm sorry. You needn't attend the meeting.");
				} catch (ErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (event instanceof PrivateMessageEvent) {
			IRoom room = muc.getRoom(event.getRoomJid());
			
			RoomMessage roomMessage = ((PrivateMessageEvent)event).getEventObject();
			if ("dongger".equals(roomMessage.getNick())) {
				room.send("dongger", new Message("He is fine!"));
			}
			
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			IRoom room0605 = null;
			try {
				room0605 = createRoom0605();
				inviteOtherUsers(room0605);
			} catch (ErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (room0605 != null) {
				room0605.send(new Message("Hi, guys!"));
			}
		}
	}
	
	private void inviteOtherUsers(IRoom room) throws ErrorException {
		room.invite(BARE_JID_AGILEST, "Welcome to my chat room.");
		room.invite(BARE_JID_DONGGER, "Welcome to my chat room.");
		room.invite(BARE_JID_JELLY, "Welcome to my chat room.");
	}

	private IRoom createRoom0605() throws ErrorException {
		IRoom room = muc.createInstantRoom(new JabberId("room0605", mucHost.getDomain()), "smartsheep");
		room.configure(new StandardRoomConfigurator() {
			
			@Override
			protected RoomConfig configure(RoomConfig roomConfig) {
				roomConfig.setPasswordProtectedRoom(true);
				roomConfig.setRoomSecret("0605");
				
				return roomConfig;
			}
		});
		
		return room;
	}

}
