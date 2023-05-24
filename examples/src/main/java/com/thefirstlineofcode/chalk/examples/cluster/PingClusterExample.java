package com.thefirstlineofcode.chalk.examples.cluster;

import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.xeps.ping.IPing;
import com.thefirstlineofcode.chalk.xeps.ping.PingPlugin;

public class PingClusterExample extends AbstractClusterExample {

	@Override
	protected String[][] getUserNameAndPasswords() {
		return new String[][] {new String[] {"dongger", "a_stupid_man"}};
	}

	@Override
	public void run() throws Exception {
		IChatClient chatClient = new StandardChatClient(createStreamConfig());
		chatClient.register(PingPlugin.class);
		
		chatClient.getStream().addConnectionListener(this);
		try {
			chatClient.connect(new UsernamePasswordToken("dongger", "a_stupid_man"));
		} catch (ConnectionException e) {
			e.printStackTrace();
		} catch (AuthFailureException e) {
			e.printStackTrace();
		}
		
		IPing ping = chatClient.createApi(IPing.class);
		ping.setTimeout(5 * 60 * 1000);
		
		IPing.Result result = ping.ping();
		if (result == IPing.Result.PONG) {
			System.out.println("Ping Result: Pong.");
		} else if (result == IPing.Result.SERVICE_UNAVAILABLE) {
			System.out.println("Ping Result: Service Unavailable.");
		} else {
			System.out.println("Ping Result: Timeout.");
		}
		
		chatClient.close();
	}

	@Override
	protected void doInit() {}

	@Override
	protected void cleanData() {
		// No example data.
	}
	
}
