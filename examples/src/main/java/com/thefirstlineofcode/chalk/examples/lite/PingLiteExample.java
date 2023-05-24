package com.thefirstlineofcode.chalk.examples.lite;

import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.core.stream.keepalive.KeepAliveConfig;
import com.thefirstlineofcode.chalk.examples.AbstractLiteExample;
import com.thefirstlineofcode.chalk.xeps.ping.IPing;
import com.thefirstlineofcode.chalk.xeps.ping.PingPlugin;

public class PingLiteExample extends AbstractLiteExample {

	@Override
	protected String[][] getUserNameAndPasswords() {
		return new String[][] {new String[] {"dongger", "a_stupid_man"}};
	}

	@Override
	public void run() throws Exception {
		IChatClient chatClient = new StandardChatClient(createStreamConfig());
		chatClient.register(PingPlugin.class);
		chatClient.addConnectionListener(this);
		
		chatClient.connect(new UsernamePasswordToken("dongger", "a_stupid_man"));
		
		chatClient.removeConnectionListener(this);
		chatClient.getStream().addConnectionListener(this);
		chatClient.getStream().addErrorListener(this);
		
		KeepAliveConfig config = chatClient.getStream().getKeepAliveManager().getConfig();
		KeepAliveConfig newConfig = new KeepAliveConfig(10 * 1000, 20 * 1000, config.getTimeout());
		chatClient.getStream().getKeepAliveManager().changeConfig(newConfig);
		
		for (int i = 0; i < 5; i++) {
			if (stop) {
				chatClient.close();
				return;
			}
			
			Thread.sleep(30 * 1000);
						
			IPing ping = chatClient.createApi(IPing.class);
			ping.setTimeout(60 * 1000);
			
			IPing.Result result = ping.ping();
			if (result == IPing.Result.PONG) {
				System.out.println("Ping Result: Pong.");
			} else if (result == IPing.Result.SERVICE_UNAVAILABLE) {
				System.out.println("Ping Result: Service Unavailable.");
			} else {
				System.out.println("Ping Result: Timeout.");
			}
			
		}
		
		chatClient.close();
		
	}
}
