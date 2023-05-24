package com.thefirstlineofcode.chalk.examples.cluster;

import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.examples.AbstractClientThread;

public abstract class MultiClientsClusterExample extends AbstractClusterExample {
	private int activatedClients;
	
	@Override
	public void run() throws Exception {
		Runnable[] clients = createClients();
		activatedClients = clients.length;
		
		for (Runnable client : clients) {
			Thread thread = new Thread(client);
			thread.start();
		}
		
		synchronized (this) {
			wait();
		}
	}
	
	protected class ChatClient extends StandardChatClient {
		public ChatClient(StandardStreamConfig streamConfig) {
			super(streamConfig);
		}
		
		@Override
		public synchronized void close() {
			super.close();
			
			synchronized (MultiClientsClusterExample.this) {
				activatedClients--;
				if (activatedClients == 0) {
					MultiClientsClusterExample.this.notify();
				}
			}
		}
		
		@Override
		protected synchronized void close(boolean graceful) {
			super.close(graceful);
			
			synchronized (MultiClientsClusterExample.this) {
				activatedClients--;
				if (activatedClients == 0) {
					MultiClientsClusterExample.this.notify();
				}
			}
		}
	}
	
	protected abstract AbstractClientThread[] createClients();

}
