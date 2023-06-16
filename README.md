# Chalk开发指南

## Chalk简介

Chalk是一个Java开发的XMPP客户端通讯库，可以用于开发Java桌面和Android的XMPP客户端。Chalk基于插件体系设计，这使得它易于使用及易于扩展。

## 如何使用Chalk

### 依赖配置(Maven)

Chalk是一个纯粹的Java库，在使用它时，我们需要将它配置为工程的依赖库。以Maven工程为例，我们需要做以下的配置。

#### 1.添加FirstLineCode的Maven仓库

在pom.xml中添加FirstLineCode的Maven仓库。

```
<repository>
	<id>com.thefirstlineofcode.release</id>
	<name>TheFirstLineOfCode Repository - Releases</name>
	<url>http://repo.thefirstlineofcode.com/content/repositories/releases</url>
</repository>
```

#### 2.添加Chalk依赖

Chalk库被设计成插件架构，以保证良好的扩展性，不同的功能被封装在不同的插件依赖库里，开发者可以根据自己需要，选择要使用的依赖。

##### 基础依赖

我们最少需要配置以下的基础依赖。

```
<dependency>
	<groupId>com.thefirstlineofcode.chalk</groupId>
	<artifactId>com.thefirstlineofcode.chalk</artifactId>
	<version>0.2.0-RELEASE</version>
</dependency>
```

##### 可选插件依赖

可以根据需要添加可选的插件依赖，例如添加在线注册(In Band Registration)插件依赖。

```
<dependency>
	<groupId>com.thefirstlineofcode.chalk.xeps</groupId>
	<artifactId>com.thefirstlineofcode.chalk.xeps.ibr</artifactId>
	<version>0.2.0-RELEASE</version>
</dependency>
```

下表列出了Chalk提供了的常用插件。

插件名 | groupId | artifactId | 插件描述 | 备注 |
----- | ------- | ---------- | ------- | --- |
IM     | com.thefirstlineofcode.chalk | com.thefirstlineofcode.chalk | 实现[RFC3921(Instant Messaging and Presence)](https://xmpp.org/rfcs/rfc3921.html) | IM插件内置在基础依赖包中 |
Ping   | com.thefirstlineofcode.chalk.xeps | com.thefirstlineofcode.chalk.xeps.ping | 实现[XEP-0199(XMPP Ping)](https://xmpp.org/extensions/xep-0199.html) | |
IBR    | com.thefirstlineofcode.chalk.xeps | com.thefirstlineofcode.chalk.xeps.ibr | 实现[XEP-0077(In-Band Registration)](https://xmpp.org/extensions/xep-0077.html) | |
MUC    | com.thefirstlineofcode.chalk.xeps | com.thefirstlineofcode.chalk.xeps.muc | 实现[XEP-0045(Multi-User Chat)](https://xmpp.org/extensions/xep-0045.html) | |
LEP IM | com.thefirstlineofcode.chalk.leps | com.thefirstlineofcode.chalk.leps.im | 实现LEP-0011(Traceable Message) | 非标准协议，解决XMPP IM的一些缺陷，如：双向订阅、可信赖消息；消息状态跟踪等 |

### IChatClient

使用Chalk库最重要的接口就是IChatClient，IChatClient提供了两个核心的功能：

* 建立客户端到服务器端的连接(Stream)。
* 创建插件的API，应用开发者通过插件API来使用库提供的各种功能。

#### 建立连接(Stream)

根据XMPP协议，在客户端-服务器之间，我们需要：（1）先建立一个信息通道(Stream) 。（2）在建立好的Stream上，交换信息(Stanza)。

使用以下的代码，可以建立Stream。

```
StreamConfig config = new StreamConfig("im.thefirstlineofcode.com", 5222);
config.setResource("my_android_mobile");
IChatClient chatClient = new StandardChatClient(config);
try {
	chatClient.connect("my_user_name", "my_password");
} catch (ConnectionException e) {
	throw new RuntimeException("can't connect to host", e);
} catch (AuthFailureException e) {
	throw new RuntimeException("auth failed", e);
}
```

XMPP规范定义了Stream建立的几个协商阶段：

* Initial Stream
* TLS
* SASL
* Resource Binding
* Session Establishment

如果需要监控Stream建立的细节，可以使用INegotiationListener。

```
chatClient.addNegotiationListener(new INegotiationListsener() {
	public void before(IStreamNegotiant source) {
		if (source instanceof TlsNegotiation) {
			System.out.println("Ready to negotiate TLS");
		}
	}

	public void after(IStreamNegotiant source) {
		if (source instanceof TlsNegotiation) {
			System.out.println("TLS negotiation has done");
		}
	}
	
	public void occurred(NegotiationException exception) {
		IStreamNegotiant source = exception.getSource();
		if (source instnaceof SaslNegotiant) {
			SaslError saslError = (SaslError)exception.getAdditionalErrorInfo();
			System.out.println("Error occured in SASL negotiation. Additional error info: " + saslError);
		}
	}
	
	public void done(IStream stream) {
		System.out.println("Stream has created");
	}
});

chatClient.connect("my_user_name", "my_password");
```

在TLS协商过程中，如果客户端需要检查服务器证书有效性，可以使用IPeerCertificateTruster。

```
((StandardChatClient)chatClient).setPeerCertificateTruster(new IPeerCertificateTruster() {
		public boolean accept(X509Certificate[] certificates) {
			// check certificates
		}
	}
);
```

#### 使用插件

XMPP是一组协议族，包括RFC协议(例如：RFC3920、RFC3921)和XEPs(例如：XEP-0045 Multi-User Chat、XEP-0077 In-Band Registration)协议。在很多情况下，用户甚至可能会基于XMPP协议标准，定义自己的私有协议。

Chalk基于插件架构设计，以对应XMPP的扩展性和灵活性。除了少数基础功能(例如：建立Stream)之外，Chalk的所有功能都由插件来提供。

Chalk基于一个简单、灵活的插件框架，使用插件功能，一般使用插件需要以下的步骤：

* 注册插件
* 获取插件API
* 使用插件功能

我们用一个简单的插件PingPlugin(实现[XEP-0199 XMPP Ping](https://xmpp.org/extensions/xep-0199.html))来演示插件的使用方法。

##### 注册插件

使用插件前，需要先注册插件，IChatClient提供了注册插件的接口。

注册PingPlugin的代码如下。

```
chatClient.register(PingPlugin.class);
```

##### 获取插件API

注册插件后，就可以获取插件的API。每个插件根据自己协议的细节，设计并提供API，应用开发者需要通过对应的文档了解相关细节。

Ping插件提供了一个名为IPing的接口，定义如下。

```
public interface IPing {
	public enum Result {
		PONG,
		SERVICE_UNAVAILABLE,
		TIME_OUT
	}
	
	Result ping();
	void setTimeout(int timeout);
	int getTimeout();
}
```

获取IPing接口的代码如下。

```
IPing ping = chatClient.createApi(IPing.class);
```

##### 使用插件功能

```
ping.setTimeout(4 * 1000); // Set timeout to the ping operation. Default is 2 * 1000ms.
Result result = ping.ping(); // Ping the server and waiting for result.

if (result == Ping.Result.PONG) {
	System.out.println("Pong.");
} else if (result == SERVICE_UNAVAILABLE) {
	System.out.println("Server doesn't support the protocol.");
} else {
	System.out.println("Ping timed out.");
}

```

### 插件(IPlugin)

Chalk的一个重要设计目的，是为了充分体现XMPP协议的灵活性及扩展性，以便于我们可以根据自己需要，灵活的定义和开发IM通讯协议。

XMPP协议为一组松散的协议族，除了RFC3920 XMPP Core，RFC3921 XMPP IM之外，其它的协议(主要是XEPs)被视为可选的协议。不同的IM产品会选择实现其中一些协议，
Chalk也实现了部分XMPP标准协议，并提供一些改善标准XMPP的非标准协议扩展。

以下为Chalk已经实现的插件(协议)。

#### IbrPlugin

IbrPlugin实现了[XEP-0077(In-Band Registration)](https://xmpp.org/extensions/xep-0077.html)，为客户端提供在线注册功能。

##### 注册插件

```
chaClient.register(IbrPlugin.class);
```

##### 使用IRegistration接口注册用户

```
try {
	IRegistration registration = chatClient.createApi(IRegistration.class);
	registration.register(new IRegistrationCallback() {
		public Object fillOut(IqRegister iqRegister) {
			if (iqRegister.getRegister() instanceof RegistrationForm) {
				RegistrationForm form = new RegistrationForm();
				form.getFields().add(new RegistrationField("username", "my_user_name"));
				form.getFields().add(new RegistrationField("password", "my_password"));
				
				return form;
			} else {
				throw new RuntimeException("Can't get registration form");
			}
		}
	});
} catch(RegistrationException e) {
	IbrError error = e.getError();
	if (error instanceof IbrError.Conflict) {
		System.out.println("User has existed. Please change your name.");
	} else if (error instanceof IbrError.NOT_ACCEPTABLE) {
		System.out.println("Illegal user name. Please change your name.");
	} else {
		System.out.println("Registration failed.");
	}
}

```

#### InstantingMessengerPlugin

InstantingMessengerPlugin实现了[RFC3921(Instant Messaging and Presence)](https://xmpp.org/rfcs/rfc3921.html)，提供Roster管理、Subscription管理，及发送接收Presence和Message的功能。

>为简化文档，我们在后续说明中，可能会使用IM插件作为InstantingMessengerPlugin的同义词，当提及IM插件时，意味着是InstantingMessengerPlugin。

##### 注册插件

```
chaClient.register(InstantingMessengerPlugin.class);
```

##### 发送和接收Presence

###### Initial Presence

在成功建立Stream后，根据RFC3921要求，客户端应该要立即发送一个Initial Presence，服务器端在收到Initial Presence后，才会将客户端的状态置为Available。

```
IInstantingMessenger im = chatClient.createApi(IInstantingMessenger.class);

im.send(new Presence()); // Send initial presence
```

###### 发送Presence

在此后任何时刻，都可以通过发送Presence，更改自己的当前状态。

```
Presence presence = new Presence(Show.DND);
presence.getStatuses().add(new LangText("I'm being in a meeting."));

im.send(presence);
```

###### 监听Presence

可以通过IPresenceListener监听联系人的Presence变化。

```
im.addPresenceListener(new IPresenceListener() {
	public void received(Presence presence) {
		System.out.println("Contact " + presence.getFrom() + " changed it's presence to " + presence.toString());
	}
});
```

##### Roster管理

XMPP IM协议中，使用Roster来管理联系人列表。IM插件提供IRosterService和IRosterListener来管理Roster。

###### 获取Roster

使用IRosterService的retrieve()方法来从服务器端获取Roster列表。

```
IRosterService rosterService = im.getRosterService();
rosterService.addRosterListener(new IRosterListener() {
	void retrieved(Roster roster) {
		// Process the roster that is retrieved from server.
	}

	void occurred(RosterError error) {
		// An error occurred
	}
	...
});
rosterService.retrieve();
```

> 注意：retrieve()是一个异步方法，并不会直接返回结果，所以我们需要注册一个IRosterListener来监听获取的结果。

###### 监听Roster变化

IRosterListener还提供了updated()和deleted()方法，可以用于监听Roster的变更。

```
rosterService.addRosterListener(new IRosterListener() {
	...
	public void updated(Roster roster) {
		// Process the updated roster
	}
	
	public void deleted(Roster roster) {
		// process the deleted roster
	}
	...
});
```

###### 变更Roster

> 大部分时候，我们并不需要直接变更Roster，Roster管理往往和Subscription管理相关，当Subscription状态变更时，会自动导致Roster变更。

在某些情况下，我们需要直接变更Roster，例如修改用户的分组，IRosterService提供了以下的方法。

```
public interface IRosterService {
	...
	void add(Roster roster);
	void update(Roster roster);
	void delete(Roster roster);
	...
}
```

##### Subscription管理

XMPP IM协议使用Subscription来管理联系人之间的关联关系。IM插件提供了ISubscriptionService和ISubscriptionListener来管理Subscription。

###### 发起订阅

```
JabberId contact = JabberId.parse("smartsheep@im.thefirstlineofcode.com");
ISubscriptionService subscriptionService = im.getSubscriptionService();
subscriptionService.subscribe(contact);
```

###### 接收订阅消息

注册ISubscriptionListener，可以接收订阅消息。

```
subscriptionService.addSubscriptionListener(new ISubscriptionListener() {
	...
	public void asked(JabberId user) {
		System.out.println("User " + user + " wants to subscribe you.");
	}
	...
});
```

###### 接受订阅

如果用户决定通过对方的订阅，可以使用以下代码。

```
subscriptionService.approve(contact);
```

###### 拒绝订阅

如果拒绝对方订阅，可以使用以下代码。

```
subscriptionService.refuse(contact);
```

###### 接收反馈消息

使用ISubscriptionListener监听订阅反馈信息。

```
subscriptionService.addSubscriptionListener(new ISubscriptionListener() {
	...
	public void approved(JabberId contact) {
		System.out.println("User " + contact + " approved your subscription.");
	}

	public void refused(JabberId contact) {
		System.out.println("User " + user + " refused your subscription.");
	}
	...
});
```

##### 发送和接收Message

###### 发送Message

```
JabberId contact = JabberId.parse("agilest@im.thefirstlineofcode.com");

Message message = new Message("Hello, Agilest!");
message.setTo(contact);

im.send(message);
```

或者，采用更简洁的方式。

```
JabberId contact = JabberId.parse("agilest@im.thefirstlineofcode.com");
im.send(contact, new Message("Hello, Agilest!"))
```

###### 监听Message

```
im.addMessageListener(new IMessageListener() {
	public void received(Message message) {
		System.out.println("Received a message from user " + message.getFrom());
	}
});
```

#### InstantingMessengerPlugin2

InstantingMessengerPlugin2实现了LEP-0011(Traceable Message)。主要是提供可靠消息服务，以及可以跟踪消息状态。

> LEP-0011是非标准协议，需要支持LEP协议的服务器配合，例如：Granite XMPP Server。

##### 注册插件

```
chaClient.register(InstantingMessengerPlugin2.class);
```

##### 跟踪消息状态

InstantingMessengerPlugin2插件，使用IMessageListener2的接口来替代标准的IMessageListener。IMessageListeners接口提供了一个traced方法来跟踪消息状态。

```
IInstantingMessenger2 im2 = chatClient.createApi(IInstantingMessenger2.class);
im2.addMessageListener(new IMessageListener2() {
	public void received(Message message) {
		System.out.println("Received a message from user " + message.getFrom());
	}

	public void traced(Trace trace) {
		for (MsgStatus status : trace.getMsgStatuses()) {
			if (status.getStatus() == MsgStatus.Status.SERVER_REACHED) {
				System.out.println("Message which id is " + status.getId() + " has reached server at time " + status.getStamp());
			} else if (status.getStatus() == MsgStatus.Status.PEER_REACHED) {
				System.out.println("Message which id is " + status.getId() + " has reached peer at time " + status.getStamp());
			} else { // status.getStatus() == MsgStatus.Status.MESSAGE_READ
				System.out.println("Message which id is " + status.getId() + " has read by contact at time " + status.getStamp());
			}
		}
	}
});

```

#### MucPlugin

MucPlugin实现了[XEP-0045(Multi-User Chat)](https://xmpp.org/extensions/xep-0045.html)，提供聊天室多人聊天功能。

##### 注册插件

```
chaClient.register(MucPlugin.class);
```

##### 查询Muc主机

```
IMucService muc = chatClient.createApi(IMucService.class);
JabberId[] hosts = muc.getMucHosts();
```

##### 查询某台主机上的聊天室

```
JabberId[] rooms = muc.getMucRooms();
```

##### 获取一个聊天室实例

```
IRoom room = muc.getRoom(roomJid);
```

##### 获取聊天室信息

```
RoomInfo roomInfo = room.getRoomInfo();
```

##### 进入聊天室

```
room.enter("my_nick_name");
```

##### 退出聊天室

```
room.exit();
```

##### 获取聊天室人员列表

```
Occupant[] occupants = room.getOccupants();
```

##### 发送消息到聊天室

可以给聊天室发送消息，聊天室里的所有用户都能收到该消息。

```
room.send(new Message("Hello, everyone!"));
```

##### 私聊

可以发送私聊消息给聊天室中某个用户。

```
room.send("user_nick_name", new Message("Hello, everyone!"));
```

##### 创建聊天室

###### 用默认配置创建一个聊天室

```
JabberId roomJid = JabberId.parse("my_chat_room_name@im.thefirstlineofcode.com");
muc.createInstantRoom(roomJid, "my_nick_name");
```

###### 用自定义配置创建聊天室

```
JabberId roomJid = JabberId.parse("my_chat_room_name@im.thefirstlineofcode.com");
muc.createReservedRoom(roomJid, "my_nick_name", new StandardRoomConfigurator() {
	protected RoomConfig configure(RoomConfig roomConfig) {
		roomConfig.setRoomName("my first room");
		roomConfig.setRoomDesc("Hope you have happy hours here!");
		roomConfig.setMembersOnly(true);
		roomConfig.setAllowInvites(true);
		roomConfig.setPasswordProtectedRoom(true);
		roomConfig.setRoomSecret("simple_password");
		roomConfig.getGetMemberList().setParticipant(false);
		roomConfig.getGetMemberList().setVisitor(false);
		roomConfig.setWhoIs(WhoIs.MODERATORS);
		roomConfig.setModeratedRoom(true);
		
		return roomConfig;
	}
}
);
```

##### 邀请其它用户加入聊天室

```
JabberId myColleague = JabberId.parse("my_colleague_name@im.thefirstlineofcode.com");
room.invite(myColleague, "Let's discuss our plan")
```

##### 监听聊天室事件

通过IRoomListener可以监听多种Room相关的事件。当Room产生事件时，IRoomListener接收到RoomEvent类型的对象。

RoomEvent对象有两个关键属性，roomJid和eventObject，roomJid表示Event来自哪个Rooom，而eventObject则根据RoomEvent类型的不同，可以是不同类型的对象，表示Event的具体细节。

处理RoomEvent的代码大概如下。

```
muc.addRoomListener(new IRoomListener() {
	public void received(RoomEvent<?> event) {
		if (event instanceof InvitationEvent) {
			InvitationEvent invitationEvent = (InvitationEvent)event;
			JabberId roomJid = invitationEvent.getRoomJid();
			Invitation invitation = invitationEvent.getEventObject();
			System.out.println(String.format("'%s' invites you to join room '%s'", invitation.getInvitor(), roomJid));
		} else if (event instanceof EnterEvent) {
			Enter enter = ((EnterEvent)event).getEventObject();
			Occupant occupant = muc.getRoom(event.getRoomJid()).getOccupant(enter.getNick());
			int sessions = occupant == null ? 0 : occupant.getSessions();
			System.out.println(String.format("'%s'[sessions:%d] has joined room '%s'", enter.getNick(), sessions, event.getRoomJid()));
		} else if (event instanceof ExitEvent) {
			Exit exit = ((ExitEvent)event).getEventObject();
			Occupant occupant = muc.getRoom(event.getRoomJid()).getOccupant(exit.getNick());
			int sessions = occupant == null ? 0 : occupant.getSessions();
			System.out.println(String.format("'%s'[sessions:%d] has exited room '%s'", exit.getNick(), sessions, event.getRoomJid()));
		} else if (event instanceof ChangeAvailabilityStatusEvent) {
			ChangeAvailabilityStatus changeAvailabilityStatus = ((ChangeAvailabilityStatusEvent)event).getEventObject();
			System.out.println(String.format("'%s' has changed it's availability status to: %s", changeAvailabilityStatus.getNick(),
					getAvailabilityStatus(changeAvailabilityStatus)));
		} else if (event instanceof RoomMessageEvent) {
			RoomMessageEvent messageEvent = (RoomMessageEvent)event;
			System.out.println(String.format("groupchat message received[from '%s' at room '%s']: %s", messageEvent.getEventObject().getNick(),
					messageEvent.getRoomJid(), messageEvent.getEventObject().getMessage()));
		} else if (event instanceof PrivateMessageEvent) {
			PrivateMessageEvent privateMessageEvent = (PrivateMessageEvent)event;
			System.out.println(String.format("groupchat private message received[from '%s' at room '%s']: %s", privateMessageEvent.getEventObject().getNick(),
					privateMessageEvent.getRoomJid(), privateMessageEvent.getEventObject().getMessage()));
		} else if (event instanceof DiscussionHistoryEvent) {
			DiscussionHistoryEvent discussionHistoryEvent = (DiscussionHistoryEvent)event;
			System.out.println(String.format("groupchat discussion history message received[from '%s' at room '%s']: %s", discussionHistoryEvent.getEventObject().getNick(),
					discussionHistoryEvent.getRoomJid(), discussionHistoryEvent.getEventObject().getMessage()));
		} else if (event instanceof ChangeNickEvent) {
			ChangeNickEvent changeNickEvent = (ChangeNickEvent)event;
			System.out.println(String.format("user '%s'[sessions: %d] changed his nick[at room '%s']: %s", changeNickEvent.getEventObject().getOldNick(),
					changeNickEvent.getEventObject().getOldNickSessions(), changeNickEvent.getRoomJid(),
					changeNickEvent.getEventObject().getNewNick()));
		} else if (event instanceof RoomSubjectEvent) {
			RoomSubjectEvent roomSubjectEvent = (RoomSubjectEvent)event;
			if ("".equals(roomSubjectEvent.getEventObject().getSubject())) {
				System.out.println(String.format("there are no room subject in room '%s'", roomSubjectEvent.getRoomJid()));
			} else {
				System.out.println(String.format("room subject received[from '%s' in room '%s']: %s", roomSubjectEvent.getEventObject().getNick(),
						roomSubjectEvent.getRoomJid(),  roomSubjectEvent.getEventObject().getSubject()));
			}
		} else if (event instanceof KickedEvent) {
			KickedEvent kickedEvent = (KickedEvent)event;
			System.out.println(String.format("you are kicked by '%s' from room '%s'. reason is '%s'", kickedEvent.getEventObject().getNick(),
						kickedEvent.getEventObject().getActor().getNick(), kickedEvent.getRoomJid(),
							kickedEvent.getEventObject().getReason()));
		} else if (event instanceof KickEvent) {
			KickEvent kickEvent = (KickEvent)event;
			System.out.println(String.format("'%s' is kicked from room '%s'", kickEvent.getEventObject().getNick(), kickEvent.getRoomJid()));
		}
	}
});

```

以下是RoomEvent对象列表。

对象类型 | eventObject类型 | 描述 |
------- | -------------- | ---- |
ChangeAvailabilityStatusEvent | ChangeAvailabilityStatus | 有聊天室用户修改了他的Presence状态 |
ChangeNickEvent | ChangeNick | 有聊天室用户修改了他的昵称 |
DiscussionHistoryEvent | RoomMessage | 进入聊天室时，会收到聊天室最近的聊天历史消息 |
EnterEvent | Enter | 有新用户进入聊天室 |
ExitEvent | Exit | 有用户退出了聊天室 |
InvitationEvent | Invitation | 加入聊天室的邀请 |
KickedEvent | Kicked | 用户自己被踢出了聊天室 |
KickEvent | Kick | 有用户被踢出了聊天室 |
PrivateMessageEvent | RoomMessage | 私聊消息 |
RoomMessageEvent | RoomMessage | 有用户在聊天室发了消息 |
RoomSubjectEvent | RoomSubject | 聊天室主题变更 |

## 开发Chalk插件
### Chalk架构设计

![](https://cdn.jsdelivr.net/gh/XDongger/dongger_s_img_repo/images/chalk_architecture.png)

Chalk的架构设计将系统分为两部分。

* 主程序框架 
* 插件

在主程序框架中，灰色的框是系统的骨架，是系统中比较稳定的部分，虚线框的部分是系统中灵活及充满变化的地方。

这种灵活性和变化从何而来？因为XMPP被设计成一个内核稳定，但高度可扩展的协议，通过XML的namespace语义，我们可以在stanza(iq, message, presence)中添加任意的新协议元素，从而扩展XMPP协议。通过这样的方式，XMPP被扩展成一个庞大的协议族，仅公开的[XEPs(XMPP Extension Protocols)](https://xmpp.org/extensions/)就有近200个。

对应XMPP协议的设计原则，Chalk也将系统设计成稳定的框架+可扩展的插件子系统。框架部分封装了通讯细节和XMPP基础概念，插件子系统允许通过插件任意扩展系统的能力。

值得注意的是，系统的扩展和变化主要出现这几个地方。

* 协议定义
* 协议-协议对象的转化
* 协议逻辑处理

>大部分情况下，我们希望插件和协议有清楚的映射关系。这意味着，我们希望尽量能够在一个插件中，封装一个或一组相关的XMPP协议。应该尽量避免将一个独立协议的逻辑，拆分在多个插件中。

### 插件子系统

为简化Plugin的开发，Chalk提供了IChatSystem和IChatServices，希望能够把Plugin和系统之间的联系，简化限制在这两个接口内。

#### IChatSystem

IChatSystem允许Plugin将特定的扩展注册到系统当中，主要包括：

* 协议对象(Protocol Object)
* 协议-协议对象转换器(Parser & Translator)
* 插件的Api(Api)
* 插件Api的实现(Api Impl)

#### IChatServices

IChatServices封装了下层通讯细节及XMPP基础概念，插件的Api实现可以调用IChatServices提供的服务，处理协议的细节逻辑。

### 开发一个最简单的插件

我们通过一个简单的案例，说明如何开发一个Chalk插件。

#### 协议定义

我们选择实现一个简单的协议[XEP-0199(XMPP Ping)](https://xmpp.org/extensions/xep-0199.html)。

XMPP Ping是一个基于iq的协议，简单来说，我们需要处理以下的逻辑。

客户端向服务器端发送一个ping请求。

```
<iq from='juliet@capulet.lit/balcony' to='capulet.lit' id='c2s1' type='get'>
	<ping xmlns='urn:xmpp:ping'/>
</iq>
```

如果服务器支持XMPP Ping协议，服务器返回pong响应。

```
<iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='c2s1' type='result'/>
```

如果服务器不支持XMPP Ping协议，则返回<service-unavailable/>错误。

```
<iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='c2s1' type='error'>
	<ping xmlns='urn:xmpp:ping'/>
	<error type='cancel'>
		<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
	</error>
</iq>
```

#### 协议对象（Protocol Object）

为了便于逻辑处理，我们一般会对应网络上传输协议XML文档，设计一个Java业务对象，这样便于用业务类来进行处理。这个Java业务对象，我们称之为协议对象(Protocol Object)。

在本案例中，协议对象结构非常简单，类定义如下。

```
public class Ping {
	public static final Protocol PROTOCOL = new Protocol("urn:xmpp:ping", "ping");
}
```

Ping类不包含任何信息，只是用类型来表示ping协议。

#### 解析器（IParser）和转换器（ITranslator）

有了Protocol Object，引发了另外的问题，就是如何处理XML文档-Protocol Object之间的转换。

当我们收到网络上的XML协议文档，需要将其内容转换成一个Protocol Object实例。对应的，当我们发送一个Protocol Object时，需要将Protocol Object包含的协议信息，转换成对应的XML文档，再发送到网络上去。

Chalk用IParser和ITranslator来处理Protocol Object-XML文档之间的转换。IParser负责将一个XML文档转换成Protocol Object实例。ITranslator负责将一个Protocol Object实例翻译成对应的XML文档。

为了简化这些转换逻辑，Chalk使用Basalt项目提供的一个XMPP OXM(Protocol Object-XML Document Mapping)框架。在大部分情况下，我们并不需要为对象和XML文档之间的转换，编写逻辑代码。我们只需要选择系统内置的IParser和ITranslator实现。

因为Ping对象结构非常简单，我们可以选择使用SimpleObjectParser和SimpleObjectTranslator来处理对象和XML文档之间的转换。

>Basalt是一个XMPP协议库，定义了基础的XMPP协议对象，并提供一个简单易用的OXM(Protocol Object-XML Document Mapping)框架。

>最常用的IParser和ITranslator，是NamingConventionParser和NamingConventionTranslator，它们采用命名约定的方法，将Protocol Object的Field和XML文档的Element做对应的拷贝。

>关于Basalt OXM(Protocol Object-XML Document Mapping)框架的更多信息，请参考Basalt项目相关文档。

#### Api
在理想的情况下，我们将Chalk的开发者分为两类：

* 插件开发者
* 应用开发者

插件开发者理解XMPP协议细节，以及Chalk的插件架构体系。他们将XEPs或者自定义的非标准协议，开发成对应的Plugin。

应用开发者不需要理解XMPP协议的细节，他们只是使用Chalk基础框架和选用插件，在此之上开发具体XMPP应用，例如IM，物联网应用等。

在现实中，开发者可能需要兼插件开发者和应用开发者。即使是这样的情况，在设计插件时，也应该遵循一些原则：

* 提供易于使用的Api，使得应用开发更简单、直观。
* 屏蔽XMPP协议的底层细节。
* 避免绑定具体的应用逻辑，提升插件的复用性。

插件开发者一个重要的任务，是设计良好的Api，给应用开发者调用。

在本案例中，PingPlugin提供以下的Api给应用开发者使用。

```
public interface IPing {
	public enum Result {
		PONG, // Server returned a pong.
		SERVICE_UNAVAILABLE, // Server doesn't support the protocol.
		TIME_OUT // Ping timed out.
	}
	
	/**
	 * Send a ping request to server and waiting for result. 
	 */
	Result ping();
	
	/**
	 * Set timeout for ping operation.
	 */
	void setTimeout(int timeout);
	
	/**
	 * Get the ping timeout.
	 */
	int getTimeout();
}
```

#### Api实现

基于IChatServices接口，有多种办法可以实现协议逻辑。

> 只要正确的注册Api和Api Impl，框架会在在Api Impl里自动注入IChatServices对象，所以我们总是可以获取到IChatServices来使用。

##### Legacy模式

最常规的想法，我们向服务器端发送一个带id的ping消息，然后监控所有收到的信息，如果有一条相同id的消息返回，我们检查是pong还是server-unavailable，如果超过一定的时间还没收到响应消息，我们就返回超时。

如果是这样的思路，我们可以：

* 使用IIqService发送一个ping消息到服务器。

```
IIqService iqService = chatServices.getIqService();
Iq ping = new Iq(Iq.Type.GET);
ping.setObject(new Ping());

String pingId = ping.getId();

iqService.send(ping);
```

> 注意，我们需要记录下发送的iq的id，便于后面找到服务器端对应的响应消息。

* 我们当然需要增加一个IIqListener，监听从服务器端来的消息，检查是否pong的响应。

```
iqService.addListener(new IIqListener() {
	public void received(Iq iq) {
		if (pingId.equals(iq.getId()) {
			// Received response from server
		}
	}
});
```

* 我们还需要监听错误消息，检查服务器可能返回service-unavailable错误。

```
IErrorService errorService = chatServices.getErrorService();
errorService.addListener(new IErrorListener() {
	public void occurred(IError error) {
		if (pingId.equals(error.getId()) {
			if (error instanceof ServerUnavailable) {
				// Received server-unavailable error from server			
			}
		}
	}
});
```

* 我们当然还需要一个Timer定时器，来处理超时的情况。

```
Timer timer = new Timer();
timer.schedule(pingTimeoutTask, timeout);
```

这些处理看上去非常繁琐，Chalk提供了稍微简便一些的方法，我们可以使用SyncOperationTemplate类来简化一些代码逻辑。

以下是使用SyncOperationTemplate大概的代码逻辑。

```
public class PingImpl implements IPing {
	private IChatServices chatServices;
	private String id;
	private int timeout;

	public Result ping() {
		SyncOperationTemplate<Iq, IPing.Result> template = new SyncOperationTemplate<Iq, IPing.Result>(chatServices);
		
		try {
			return template.execute(new ISyncIqOperation<IPing.Result>() {

				public void trigger(IUnidirectionalStream<Iq> stream) {
					Iq iq = new Iq(Iq.Type.SET);
					iq.setObject(new Ping());
					id = iq.getId();
					
					stream.send(iq, timeout);
				}

				public boolean isErrorOccurred(StanzaError error) {
					if (id.equals(error.getId()))
						return true;
					
					return false;
				}

				public boolean isResultReceived(Iq iq) {
					if (id.equals(iq.getId()))
						return true;
					
					return false;
				}

				public Result processResult(Iq iq) {
					return IPing.Result.PONG;
				}
			});
		} catch (ErrorException e) {
			if (e.getError().getDefinedCondition().equals(RemoteServerTimeout.DEFINED_CONDITION)) {
				return IPing.Result.TIME_OUT;
			} else {
				return IPing.Result.SERVICE_UNAVAILABLE;
			}
		}
	}
	}

	...
}
```

这里简化之处在于，我们可以在一个ISyncIqOperation内部类中处理所有逻辑，而不需要去访问IIqService，IIqListener，IErrorService，IErrorListener及Timer等诸多细节。

##### Task模式

Legacy模式还是比较复杂，特别我们会注意到一个问题，我们总是需要在代码中跟踪相同id的消息，这似乎意味着跟踪id的处理，应该移交给框架去进行处理。

Chalk提供了Task模式的处理框架，可以避免我们琐碎的去跟踪相同id的消息，更加简化协议逻辑的处理。

###### Sync Task

注意，我们在IPing接口里，采用同步阻塞等待结果的方法，调用ping()方法后，程序会阻塞直到获得pong响应，或者接收到server-unavailable错误，或者等待超时返回。

在同步的情况下，最方便是使用ITaskService和ISyncTask接口。

```
public class PingImpl implements IPing {
	private IChatServices chatServices;
	private int timeout;
	
	...

	public Result ping() {
		ITaskService taskService = chatServices.getTaskService();
		try {
			return taskService.execute(new ISyncTask<Iq, IPing.Result>() {

				public void trigger(IUnidirectionalStream<Iq> stream) {
					Iq iq = new Iq(Iq.Type.SET);
					iq.setObject(new Ping());

					stream.send(iq, timeout);
				}

				public Result processResult(Iq iq) {
					return IPing.Result.PONG;
				}

			});
		} catch (ErrorException e) {
			if (e.getError().getDefinedCondition().equals(RemoteServerTimeout.DEFINED_CONDITION)) {
				return IPing.Result.TIME_OUT;
			} else {
				return IPing.Result.SERVICE_UNAVAILABLE;
			}
		}
	}

	...
	
}
```

可以看到，在Task模式下，框架默认监控相同id的消息，如果是接收到相同id的iq result，则回调processResult()方法进行处理。

如果服务器端返回相同id的错误，以及超时错误，都会统一封装成ErrorException，可以catch例外根据具体情况进行处理。

###### Async Task

在实时消息系统中，Sync的场景会比较少，大多数场景下，等待来自服务器或联系人的消息，但是消息何时会来到，我们并不能预期。

在大多数情况下，我们应该使用Async Task而不是Sync Task，等消息到达的时候，触发回调方法。

ITaskService提供了执行Async Task的方法：

```
public interface ITaskService {
	...

	void execute(ITask<?> task);

	...
}
```

> 关于Sync Task和Async Task的区别，还有一个值得注意的地方。所有的Sync Task的回调处理，都是在主消息接收线程里执行的，这意味着，如果有一个回调方法执行时，占用太多时间，会导致其它的Task被阻塞，有可能导致的一个结果是，应用程序一些业务被阻塞变慢。

> 当然我们可以在一些耗时的Sync Task回调方法里，启动新的线程，避免阻塞主消息接收线程。这是一个解决办法，但是我们有时候可能会容易忘记需要启动新线程。

> Async Task采用了不同的处理方法，框架提供了一个线程池，每当接收到一个需要处理的Async Task回调时，系统会从线程池中启动一个线程，将回调逻辑放在新线程中去处理。这样，Async Task可以更好的避免系统阻塞变慢问题。

> 如果可能，应该尽可能的使用Async Task模式来处理协议的逻辑。

#### 打包所有

现在，我们已经处理好了所有的协议细节和业务逻辑，需要将所有的代码和逻辑，通过插件注册到系统中去。我们在上面已经提到过了，我们使用IChatSystem来帮助完成这项工作。

我们需要编写一个Plugin类，在本案例中，这个类是PingPlugin。

在本案例中PingPlugin中，我们需要：
* 注册协议的协议对象Ping，以及对应的Parser和Translator。
* 注册提供的Api接口IPing，以及IPing的具体实现PingImpl。

我们在Plugin类的init()方法里，注册插件给系统提供的扩展。在destroy()方法里，我们移除这些扩展。

PingPlugin的代码如下。

```
public class PingPlugin implements IPlugin {
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerParser(
				ProtocolChain.first(Iq.PROTOCOL).next(Ping.PROTOCOL),
				new SimpleObjectParserFactory<Ping>(Ping.PROTOCOL, Ping.class));
		chatSystem.registerTranslator(
				Ping.class,
				new SimpleObjectTranslatorFactory<Ping>(Ping.class, Ping.PROTOCOL));
		
		chatSystem.registerApi(IPing.class, PingImpl.class, properties);			
	}

	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IPing.class);
		chatSystem.unregisterTranslator(Ping.class);
		chatSystem.unregisterParser(ProtocolChain.first(Iq.PROTOCOL).next(Ping.PROTOCOL));
	}

}
```

#### 最后

插件已经开发完成，现在我们可以注册插件：

```
chatClient.register(PingPlugin.class);
```

创建Api：

```
IPing ping = chatClient.createApi(IPing.class);
```

并执行协议逻辑：

```
IPing.Result result = ping.ping();
```

PingPlugin是一个非常简单的插件，虽然它很简单，但是开发这样一个插件，依然需要完成一个完整的插件开发的过程。

这样一个简单而又完整的插件案例，是我们开发更复杂协议的起点。

如果要开发更复杂的协议，最好的办法就是阅读XMPP文档，以及阅读Chalk的代码。如果你对XMPP和开源充满热情，那现在就开始吧。

## 其它
### 流协商（Stream Negotiation）

TBD

### Android平台

TBD
