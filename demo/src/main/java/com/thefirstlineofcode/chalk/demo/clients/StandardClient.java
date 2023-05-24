package com.thefirstlineofcode.chalk.demo.clients;

import com.thefirstlineofcode.chalk.demo.Client;
import com.thefirstlineofcode.chalk.demo.Demo;
import com.thefirstlineofcode.chalk.im.InstantingMessagerPlugin;
import com.thefirstlineofcode.chalk.xeps.ibr.IbrPlugin;
import com.thefirstlineofcode.chalk.xeps.muc.MucPlugin;

public abstract class StandardClient extends Client {

	public StandardClient(Demo demo, String clientName) {
		super(demo, clientName);
	}

	@Override
	protected void registerPlugins() {
		chatClient.register(IbrPlugin.class);
		
		chatClient.register(InstantingMessagerPlugin.class);
		chatClient.register(MucPlugin.class);
	}

}
