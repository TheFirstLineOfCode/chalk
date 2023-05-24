package com.thefirstlineofcode.chalk.demo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.LangText;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Item;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.INegotiationListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.im.IInstantingMessager;
import com.thefirstlineofcode.chalk.im.roster.IRosterListener;
import com.thefirstlineofcode.chalk.im.roster.RosterError;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;
import com.thefirstlineofcode.chalk.im.subscription.ISubscriptionListener;
import com.thefirstlineofcode.chalk.im.subscription.SubscriptionError;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;
import com.thefirstlineofcode.chalk.xeps.muc.IMucService;
import com.thefirstlineofcode.chalk.xeps.muc.IRoom;
import com.thefirstlineofcode.chalk.xeps.muc.IRoomListener;
import com.thefirstlineofcode.chalk.xeps.muc.Occupant;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeAvailabilityStatus;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeAvailabilityStatusEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.ChangeNickEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.DiscussionHistoryEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Enter;
import com.thefirstlineofcode.chalk.xeps.muc.events.EnterEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Exit;
import com.thefirstlineofcode.chalk.xeps.muc.events.ExitEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.Invitation;
import com.thefirstlineofcode.chalk.xeps.muc.events.InvitationEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.KickEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.KickedEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.PrivateMessageEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomMessageEvent;
import com.thefirstlineofcode.chalk.xeps.muc.events.RoomSubjectEvent;

public abstract class Client extends ConnectionListenerAdapter implements Runnable, INegotiationListener, IMessageListener,
		IPresenceListener, IRosterListener, ISubscriptionListener, IRoomListener {
	
	protected static final String host = getHost();
	protected static final int port = getPort();
	protected static final String messageFormat = getMessageFormat();
	
	protected static final JabberId JID_AGILEST_PAD = new JabberId("agilest", host, "pad");
	protected static final JabberId JID_AGILEST_MOBILE = new JabberId("agilest", host, "mobile");
	protected static final JabberId JID_DONGGER_OFFICE = new JabberId("dongger", host, "office");
	protected static final JabberId JID_DONGGER_HOME = new JabberId("dongger", host, "home");
	protected static final JabberId JID_SMARTSHEEP_MOBILE = new JabberId("smartsheep", host, "mobile");
	
	protected static final JabberId BARE_JID_AGILEST = new JabberId("agilest", host);
	protected static final JabberId BARE_JID_DONGGER = new JabberId("dongger", host);
	protected static final JabberId BARE_JID_SMARTSHEEP = new JabberId("smartsheep", host);
	protected static final JabberId BARE_JID_JELLY = new JabberId("jelly", host);
	
	protected String clientName;
	protected StandardChatClient chatClient;
	protected IInstantingMessager im;
	protected IMucService muc;
	protected JabberId mucHost;
	
	private static Map<Class<?>, AtomicInteger> runCounts = new HashMap<>();
	
	protected Demo demo;
		
	public Client(Demo demo, String clientName) {
		this.clientName = clientName;
		this.demo = demo;
	}
	
	private static String getHost() {
		String host = System.getProperty("chalk.stream.config.host");
		if (host == null) {
			host = "localhost";
		}
		
		return host;
	}
	
	private static int getPort() {
		String sPort = System.getProperty("chalk.stream.config.port");
		if (sPort == null) {
			return 5222;
		}
		
		return Integer.parseInt(sPort);
	}
	
	private static String getMessageFormat() {
		return System.getProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT);
	}
	
	protected AtomicInteger getRunCount() {
		AtomicInteger runCount = runCounts.get(getClass());
		if (runCount == null) {
			runCount = new AtomicInteger(0);
			runCounts.put(getClass(), runCount);
		}
		
		return runCount;
	}

	public void start() {
		new Thread(this).start();
	}
	
	public void exit() {
		chatClient.close();
		
		print("Exited.");
	}
	
	@Override
	public void run() {
		try {
			getRunCount().incrementAndGet();
			runChatClient();
		} catch (Exception e) {
			e.printStackTrace();
			chatClient.close();
			
			synchronized (this) {
				notify();
			}
		}
	}
	
	protected void runChatClient() throws Exception {
		createChatClient();
		
		beforeConnecting();
		
		chatClient.addConnectionListener(this);
		chatClient.addNegotiationListener(this);
		String[] userNameAndPassword = getUserNameAndPassword();
		try {
			chatClient.connect(userNameAndPassword[0], userNameAndPassword[1]);
		} catch (AuthFailureException e) {
			chatClient.close();
			print("Auth failed.");
			
			createChatClient();
			chatClient.addConnectionListener(this);
			chatClient.addNegotiationListener(this);
			
			print("Reconnect...");
			try {
				chatClient.connect(userNameAndPassword[0], userNameAndPassword[1]);
			} catch (AuthFailureException ae) {
				print("Auth failed.");
				print("Invoke auth failure callback.");
				processAuthFailure(ae);
			}
		} catch (ConnectionException e) {
			exceptionOccurred(e);
		}
		
		im = chatClient.createApi(IInstantingMessager.class);
		
		im.addMessageListener(this);
		im.addPresenceListener(this);
		im.getRosterService().addRosterListener(this);
		im.getSubscriptionService().addSubscriptionListener(this);
		
		muc = chatClient.createApi(IMucService.class);
		muc.addRoomListener(this);
		
		try {
			JabberId[] hosts = muc.getMucHosts();
			
			if (hosts == null || hosts.length == 0)
				throw new RuntimeException("No MUC host found.");
			
			mucHost = hosts[0];
			print(String.format("MUC host %s found.", mucHost));
		} catch (Exception e) {
			throw new RuntimeException("Can't get MUC hosts.", e);
		}
		
		try {
			int totalNumber = muc.getTotalNumberOfRooms(mucHost);
			if (totalNumber == 0) {
				print(String.format("No chat rooms found on %s.", mucHost));
			} else {
				print(String.format("%d chat rooms found on %s.", totalNumber, mucHost));
			}
		} catch (Exception e) {
			throw new RuntimeException("Can't get chat rooms.", e);
		}
		
		afterConnected();
		
		im.getRosterService().retrieve();
	}

	protected void createChatClient() {
		chatClient = new StandardChatClient(getStreamConfig());
		registerPlugins();
	}

	@Override
	public void received(Presence presence) {
		print(String.format("Presence(%s, %s, %s) received[from %s].", presence.getType() == null ?
				"AVAILABLE" : presence.getType(), presence.getShow(),
					getStatusText(presence.getStatuses()), presence.getFrom()));
	}

	private String getStatusText(List<LangText> statuses) {
		if (statuses == null || statuses.size() == 0)
			return null;
		
		StringBuilder sb = new StringBuilder();
		for (LangText status : statuses) {
			sb.append(status.getText()).append(",");
		}
		
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}

	@Override
	public void received(Message message) {
		print(String.format("Message received[from %s]: %s.", message.getFrom(), message.getText()));
	}
	
	@Override
	public void retrieved(Roster roster) {
		print(String.format("Roster retrieved(size: %d).", roster.getItems().length));
		for (Item item : roster.getItems()) {
			print(String.format("Item: jid(%s), name(%s), subscription(%s), ask(%s).",
				item.getJid(), item.getName(), item.getSubscription(), item.getAsk()));
			for (String group : item.getGroups()) {
				String.format("Item group: %s.", group);
			}
		}
		
		im.send(new Presence());
		
		synchronized (this) {
			notify();
		}
	}
	
	@Override
	public void occurred(RosterError error) {
		if (error.getReason() == RosterError.Reason.ROSTER_RETRIEVE_ERROR ||
				error.getReason() == RosterError.Reason.ROSTER_ADD_ERROR ||
				error.getReason() == RosterError.Reason.ROSTER_UPDATE_ERROR ||
				error.getReason() == RosterError.Reason.ROSTER_DELETE_ERROR) {
			StanzaError stanzaError = (StanzaError)error.getDetail();
			print(String.format("Roster error: %s, %s.", stanzaError.getDefinedCondition(),
				(stanzaError.getText() == null ? "null" : stanzaError.getText().getText())));
		} else {
			print(String.format("Roster timeout: %s.", error.getDetail()));
		}
	}
	
	@Override
	public void updated(Roster roster) {
		print("Roster updated.");
		for (Item item : roster.getItems()) {
			print(String.format("Item: jid(%s), name(%s), subscription(%s), ask(%s).",
				item.getJid(), item.getName(), item.getSubscription(), item.getAsk()));
			for (String group : item.getGroups()) {
				print(String.format("Item group: %s.", group));
			}
		}
	}
	
	@Override
	public void asked(JabberId user) {
		print(String.format("User %s requested your subscription.", user));
	}

	@Override
	public void approved(JabberId contact) {
		print(String.format("Contact %s approved your subscription request.", contact));
	}

	@Override
	public void refused(JabberId contact) {
		print(String.format("Contact %s refused your subscription request.", contact));
	}

	@Override
	public void occurred(SubscriptionError error) {
		print(String.format("Subscription error: %s, %s.", error.getReason(), error.getDetail()));
	}
	
	@Override
	public void revoked(JabberId user) {}
	
	protected void print(String message) {
		System.out.println(String.format("\t%s: %s", clientName, message));
	}
	
	protected void printOut(String message) {
		System.out.println(String.format("->%s: %s", clientName, message));
	}
	
	protected void printIn(String message) {
		System.out.println(String.format("<-%s: %s", clientName, message));
	}
	
	protected void processAuthFailure(AuthFailureException e) throws AuthFailureException, ConnectionException {
		throw e;
	}
	
	@Override
	public void before(IStreamNegotiant source) {
		print(String.format("%s is starting.", source.getClass().getSimpleName()));
	}

	@Override
	public void after(IStreamNegotiant source) {
		print(String.format("%s has done.", source.getClass().getSimpleName()));
	}
	
	@Override
	public void done(IStream stream) {
		// Do nothing.
	}
	
	@Override
	public void occurred(NegotiationException exception) {
		// Do nothing
	}
	
	@Override
	public void exceptionOccurred(ConnectionException exception) {
		OutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		exception.printStackTrace(ps);
		print(String.format("Connection error: %s, %s.", exception.getType(), os.toString()));
		chatClient.close();
	}
	
	@Override
	public void messageReceived(String message) {
		printIn(String.format("Received: %s.", message));
	}

	@Override
	public void messageSent(String message) {
		printOut(String.format("Sent: %s.", message));
	}
	
	@Override
	public void received(RoomEvent<?> event) {
		if (event instanceof InvitationEvent) {
			InvitationEvent invitationEvent = (InvitationEvent)event;
			JabberId roomJid = invitationEvent.getRoomJid();
			Invitation invitation = invitationEvent.getEventObject();
			print(String.format("'%s' invites you to join room '%s'.", invitation.getInvitor(), roomJid));
		} else if (event instanceof EnterEvent) {
			Enter enter = ((EnterEvent)event).getEventObject();
			Occupant occupant = muc.getRoom(event.getRoomJid()).getOccupant(enter.getNick());
			int sessions = occupant == null ? 0 : occupant.getSessions();
			print(String.format("'%s'[sessions:%d] has joined room '%s'.", enter.getNick(), sessions, event.getRoomJid()));
		} else if (event instanceof ExitEvent) {
			Exit exit = ((ExitEvent)event).getEventObject();
			Occupant occupant = muc.getRoom(event.getRoomJid()).getOccupant(exit.getNick());
			int sessions = occupant == null ? 0 : occupant.getSessions();
			print(String.format("'%s'[sessions:%d] has exited room '%s'.", exit.getNick(), sessions, event.getRoomJid()));
		} else if (event instanceof ChangeAvailabilityStatusEvent) {
			ChangeAvailabilityStatus changeAvailabilityStatus = ((ChangeAvailabilityStatusEvent)event).getEventObject();
			print(String.format("'%s' has changed it's availability status to: %s.", changeAvailabilityStatus.getNick(),
					getAvailabilityStatus(changeAvailabilityStatus)));
		} else if (event instanceof RoomMessageEvent) {
			RoomMessageEvent messageEvent = (RoomMessageEvent)event;
			print(String.format("Groupchat message received[from '%s' at room '%s']: %s.", messageEvent.getEventObject().getNick(),
					messageEvent.getRoomJid(), messageEvent.getEventObject().getMessage()));
		} else if (event instanceof PrivateMessageEvent) {
			PrivateMessageEvent privateMessageEvent = (PrivateMessageEvent)event;
			print(String.format("Groupchat private message received[from '%s' at room '%s']: %s.", privateMessageEvent.getEventObject().getNick(),
					privateMessageEvent.getRoomJid(), privateMessageEvent.getEventObject().getMessage()));
		} else if (event instanceof DiscussionHistoryEvent) {
			DiscussionHistoryEvent discussionHistoryEvent = (DiscussionHistoryEvent)event;
			print(String.format("Groupchat discussion history message received[from '%s' at room '%s']: %s.", discussionHistoryEvent.getEventObject().getNick(),
					discussionHistoryEvent.getRoomJid(), discussionHistoryEvent.getEventObject().getMessage()));
		} else if (event instanceof ChangeNickEvent) {
			ChangeNickEvent changeNickEvent = (ChangeNickEvent)event;
			print(String.format("User '%s'[sessions: %d] changed his nick[at room '%s']: %s.", changeNickEvent.getEventObject().getOldNick(),
					changeNickEvent.getEventObject().getOldNickSessions(), changeNickEvent.getRoomJid(),
					changeNickEvent.getEventObject().getNewNick()));
		} else if (event instanceof RoomSubjectEvent) {
			RoomSubjectEvent roomSubjectEvent = (RoomSubjectEvent)event;
			if ("".equals(roomSubjectEvent.getEventObject().getSubject())) {
				print(String.format("There are no room subject in room '%s'.", roomSubjectEvent.getRoomJid()));
			} else {
				print(String.format("Room subject received[from '%s' in room '%s']: %s.", roomSubjectEvent.getEventObject().getNick(),
						roomSubjectEvent.getRoomJid(),  roomSubjectEvent.getEventObject().getSubject()));
			}
		} else if (event instanceof KickedEvent) {
			KickedEvent kickedEvent = (KickedEvent)event;
			print(String.format("'%s' is kicked by '%s' from room '%s'. Reason: '%s'.", kickedEvent.getEventObject().getNick(),
						kickedEvent.getEventObject().getActor().getNick(), kickedEvent.getRoomJid(),
							kickedEvent.getEventObject().getReason()));
		} else if (event instanceof KickEvent) {
			KickEvent kickEvent = (KickEvent)event;
			print(String.format("'%s' is kicked from room '%s'.", kickEvent.getEventObject().getNick(), kickEvent.getRoomJid()));
		}
	}

	private String getAvailabilityStatus(ChangeAvailabilityStatus changeAvailabilityStatus) {
		Presence presence = changeAvailabilityStatus.getPresence();
		StringBuilder sb = new StringBuilder();
		sb.append(presence.getType() != null ? presence.getType() : "AVAILABLE");
		if (presence.getShow() != null) {
			sb.append(", ").append(presence.getShow());
		}
		
		return sb.toString();
	}
	
	protected void joinRoom(Invitation invitation) throws ErrorException {
		IRoom room = muc.getRoom(invitation.getRoom());
		if (invitation.getPassword() != null) {
			room.enter(chatClient.getStream().getJid().getNode(), invitation.getPassword());
		} else {
			room.enter(chatClient.getStream().getJid().getNode());
		}
		
	}
	
	public String getClientName() {
		return clientName;
	}
	
	protected StandardStreamConfig getStreamConfig() {
		StandardStreamConfig streamConfig = new StandardStreamConfig(getHost(), getPort());
		configureStreamConfig(streamConfig);
		
		streamConfig.setProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT, getMessageFormat());
		
		return streamConfig;
	}

	protected void beforeConnecting() {}
	protected void afterConnected() {}
	
	protected abstract void configureStreamConfig(StandardStreamConfig streamConfig);
	
	protected abstract String[] getUserNameAndPassword();
	
	protected abstract void registerPlugins();

}
