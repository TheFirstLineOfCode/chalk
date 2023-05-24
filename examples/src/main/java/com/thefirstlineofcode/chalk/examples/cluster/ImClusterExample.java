package com.thefirstlineofcode.chalk.examples.cluster;

import java.util.concurrent.CountDownLatch;

import org.bson.Document;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.examples.AbstractClientThread;
import com.thefirstlineofcode.chalk.im.IInstantingMessager;
import com.thefirstlineofcode.chalk.im.InstantingMessagerPlugin;
import com.thefirstlineofcode.chalk.im.roster.IRosterListener;
import com.thefirstlineofcode.chalk.im.roster.RosterError;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.subscription.ISubscriptionListener;
import com.thefirstlineofcode.chalk.im.subscription.SubscriptionError;

public class ImClusterExample extends MultiClientsClusterExample {
	private static final String[][] USER_AND_PASSWORDS = new String[][] {
		new String[] {"dongger", "a_stupid_man"},
		new String[] {"agilest", "a_good_guy"}
	};
	
	private CountDownLatch waitAllClientsToReady = new CountDownLatch(3);
	
	private class DonggerOfficeThread extends AbstractClientThread {
		public DonggerOfficeThread(StandardChatClient chatClient, MultiClientsClusterExample example) {
			super(chatClient, example);
		}

		@Override
		public void doRun() throws Exception {
			chatClient.register(InstantingMessagerPlugin.class);
			final IInstantingMessager im = chatClient.createApi(IInstantingMessager.class);
			im.getSubscriptionService().addSubscriptionListener(new ISubscriptionListener() {
				
				@Override
				public void revoked(JabberId user) {}
				
				@Override
				public void refused(JabberId contact) {}
				
				@Override
				public void occurred(SubscriptionError error) {
					chatClient.close();
					throw new RuntimeException("Subscription error. Reason: " + error.getReason());
				}
				
				@Override
				public void asked(JabberId user) {
					im.getSubscriptionService().approve(user);
				}
				
				@Override
				public void approved(JabberId contact) {
					im.send(getJabberId("agilest"), new Message("Hello, Agilest!"));
				}
			});
			
			im.addMessageListener(new IMessageListener() {
				
				@Override
				public void received(Message message) {
					if (message.getBodies().get(0).getText().equals("Hello, Dongger!")) {
						im.send(message.getFrom(), new Message("Are you still in ShangHai?"));
					} else {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						
						chatClient.close();
					}
				}
			});
			
			im.getRosterService().addRosterListener(new IRosterListener() {
				
				@Override
				public void updated(Roster roster) {}
				
				@Override
				public void retrieved(Roster roster) {
					waitAllClientsToReady.countDown();
					try {
						waitAllClientsToReady.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						chatClient.close();
						return;
					}
					
					im.getSubscriptionService().subscribe(getJabberId("agilest"));
				}
				
				@Override
				public void occurred(RosterError error) {
					chatClient.close();
				}
			});

			im.getRosterService().retrieve();
			im.send(new Presence());
			
			Thread.sleep(500);
		}

		@Override
		protected String[] getUserNameAndPassword() {
			return USER_AND_PASSWORDS[0];
		}

		@Override
		protected String getResourceName() {
			return "office";
		}
		
	}

	private class AgilestPadThread extends AbstractClientThread {
		public AgilestPadThread(StandardChatClient chatClient, MultiClientsClusterExample example) {
			super(chatClient, example);
		}

		@Override
		public void doRun() throws Exception {
			chatClient.register(InstantingMessagerPlugin.class);
			final IInstantingMessager im = chatClient.createApi(IInstantingMessager.class);
			
			im.getSubscriptionService().addSubscriptionListener(new ISubscriptionListener() {

				@Override
				public void asked(JabberId user) {
					im.getSubscriptionService().approve(user);
				}

				@Override
				public void approved(JabberId contact) {}

				@Override
				public void refused(JabberId contact) {}

				@Override
				public void revoked(JabberId user) {}

				@Override
				public void occurred(SubscriptionError error) {
					chatClient.close();
					throw new RuntimeException("Subscription error. Reason: " + error.getReason());
				}
				
			});
			
			im.addMessageListener(new IMessageListener() {
				
				@Override
				public void received(Message message) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					chatClient.close();
				}
			});
			
			im.getRosterService().addRosterListener(new IRosterListener() {
				
				@Override
				public void updated(Roster roster) {}
				
				@Override
				public void retrieved(Roster roster) {
					waitAllClientsToReady.countDown();
					try {
						waitAllClientsToReady.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						chatClient.close();
						return;
					}
				}
				
				@Override
				public void occurred(RosterError error) {
					chatClient.close();
				}
			});

			im.getRosterService().retrieve();
			im.send(new Presence());
			
			Thread.sleep(500);
		}

		@Override
		protected String[] getUserNameAndPassword() {
			return USER_AND_PASSWORDS[1];
		}
		
		@Override
		protected String getResourceName() {
			return "pad";
		}
		
	}
	
	private class AgilestMobileThread extends AbstractClientThread {
		public AgilestMobileThread(StandardChatClient chatClient, MultiClientsClusterExample example) {
			super(chatClient, example);
		}

		@Override
		public void doRun() throws Exception {
			chatClient.register(InstantingMessagerPlugin.class);
			final IInstantingMessager im = chatClient.createApi(IInstantingMessager.class);
			
			im.addMessageListener(new IMessageListener() {

				@Override
				public void received(Message message) {
					if (message.getText().equals("Hello, Agilest!")) {
						im.send(message.getFrom(), new Message("Hello, Dongger!"));
					} else {
						im.send(message.getFrom(), new Message("Yes, I'm still in Shanghai."));
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						chatClient.close();
					}
				}
				
			});
			
			im.getRosterService().addRosterListener(new IRosterListener() {
				
				@Override
				public void updated(Roster roster) {}
				
				@Override
				public void retrieved(Roster roster) {
					waitAllClientsToReady.countDown();
					try {
						waitAllClientsToReady.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						chatClient.close();
						return;
					}
				}
				
				@Override
				public void occurred(RosterError error) {
					chatClient.close();
				}
			});

			im.getRosterService().retrieve();
			im.send(new Presence());
			
			Thread.sleep(500);
		}

		@Override
		protected String[] getUserNameAndPassword() {
			return USER_AND_PASSWORDS[1];
		}
		
		@Override
		protected String getResourceName() {
			return "mobile";
		}
		
	}
	
	@Override
	protected String[][] getUserNameAndPasswords() {
		return USER_AND_PASSWORDS;
	}

	@Override
	protected AbstractClientThread[] createClients() {
		return new AbstractClientThread[] {
				new DonggerOfficeThread(new ChatClient(createStreamConfig("office")), this),
				new AgilestMobileThread(new ChatClient(createStreamConfig("mobile")), this),
				new AgilestPadThread(new ChatClient(createStreamConfig("pad")), this)
		};
	}

	@Override
	public void run() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void cleanData() {
		database.getCollection("subscription_notifications").deleteMany(new Document());
		database.getCollection("subscriptions").deleteMany(new Document());
	}

}
