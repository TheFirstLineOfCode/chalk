package com.thefirstlineofcode.chalk.demo;

import java.util.HashMap;
import java.util.Map;

public class Demo implements IClientController {
	private Map<Class<? extends Client>, Client> runningClients = new HashMap<>();
	
	@Override
	public void startClient(Class<? extends Client> askerClass, Class<? extends Client> clientClass) {
		try {
			doStart(askerClass, clientClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void doStart(Class<? extends Client> askerClass, Class<? extends Client> clientClass)
			throws Exception {
		if (askerClass == null) {
			System.out.println(String.format("%s starts %s.",
					"Main program", clientClass.getSimpleName()));
		} else {
			System.out.println(String.format("%s starts %s.",
					askerClass.getSimpleName(), clientClass.getSimpleName()));
		}
		
		Client client = clientClass.getConstructor(Demo.class).newInstance(this);
		client.start();
		
		synchronized (client) {
			client.wait();
		}
		
		runningClients.put(clientClass, client);
	}
	
	@Override
	public synchronized void stopClient(Class<? extends Client> askerClass, Class<? extends Client> clientClass) {
		doStop(askerClass, clientClass);
	}

	private void doStop(Class<? extends Client> askerClass, Class<? extends Client> clientClass) {
		if (askerClass == null) {
			System.out.println(String.format("%s stops %s.",
					"Main program", clientClass.getSimpleName()));
		} else {
			System.out.println(String.format("%s stops %s",
					askerClass.getSimpleName(), clientClass.getSimpleName()));
		}
		
		Client client = runningClients.remove(clientClass);
		if (client != null) {
			client.exit();
		}
	}

	public void run() {
		startClient(null, com.thefirstlineofcode.chalk.demo.clients.DonggerHome.class);
	}
}
