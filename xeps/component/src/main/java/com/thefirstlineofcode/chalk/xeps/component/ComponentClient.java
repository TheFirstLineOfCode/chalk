package com.thefirstlineofcode.chalk.xeps.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.chalk.core.AbstractChatClient;
import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.IStreamer;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.SocketConnection;
import com.thefirstlineofcode.chalk.xeps.ping.IPing;
import com.thefirstlineofcode.chalk.xeps.ping.PingPlugin;

public class ComponentClient extends AbstractChatClient {
	private static final Logger logger = LoggerFactory.getLogger(ComponentClient.class);
	
	private static final int DEFAULT_MAX_PING_FAILURES = 5;
	private static final int DEFAULT_PING_INTERVAL = 5 * 1000;
	private static final int DEFAULT_RECONNECT_INTERVAL = 5 * 1000;
	
	private PingThread pingThread;
	private int maxPingFailures;
	private int pingInterval;
	private int reconnectInterval;
	
	private String secret;
	
	public ComponentClient(ComponentStreamConfig streamConfig) {
		this(streamConfig, new SocketConnection());
	}
	
	public ComponentClient(ComponentStreamConfig streamConfig, IConnection connection) {
		super(streamConfig, connection);
		
		maxPingFailures = DEFAULT_MAX_PING_FAILURES;
		pingInterval = DEFAULT_PING_INTERVAL;
		reconnectInterval = DEFAULT_RECONNECT_INTERVAL;
		
		register(PingPlugin.class);
	}
	
	@Override
	protected IStreamer createStreamer(StreamConfig streamConfig, IConnection connection) {
		ComponentStreamer componentStreamer = new ComponentStreamer((ComponentStreamConfig)streamConfig);
		componentStreamer.setNegotiationListener(this);
		
		return componentStreamer;
	}
	
	public void connect(String secret) throws ConnectionException,
			AuthFailureException, NegotiationException {
		this.secret = secret;
		connect(new SecretToken(secret));
	}
	
	@Override
	public void connect(IAuthenticationToken authToken) throws ConnectionException,
			AuthFailureException, NegotiationException {
		if (!(authToken instanceof SecretToken)) {
			throw new IllegalArgumentException(String.format("Auth token type must be %s.", SecretToken.class.getName()));
		}
		
		super.connect(authToken);
	}
	
	@Override
	public void done(IStream stream) {
		super.done(stream);
		
		pingThread = new PingThread();
		new Thread(pingThread).run();
	}
	
	@Override
	protected void close(boolean graceful) {
		if (pingThread != null) {
			pingThread.stop();
			pingThread = null;
		}
		
		super.close(graceful);
	}
	
	public int getMaxPingFailures() {
		return maxPingFailures;
	}

	public void setMaxPingFailures(int maxPingFailures) {
		this.maxPingFailures = maxPingFailures;
	}

	public int getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}

	public int getReconnectInterval() {
		return reconnectInterval;
	}

	public void setReconnectInterval(int reconnectInterval) {
		this.reconnectInterval = reconnectInterval;
	}

	private class PingThread implements Runnable {
		private volatile boolean stop;
		private int pingFailures;
		
		@Override
		public void run() {
			stop = false;
			pingFailures = 0;
			
			IPing ping = createApi(IPing.class);
			ping.setTimeout(pingInterval);
			while (!stop) {
				IPing.Result result = ping.ping();
				if (result != IPing.Result.TIME_OUT) {
					if (pingFailures != 0)
						pingFailures = 0;
				
					try {
						Thread.sleep(pingInterval);
					} catch (InterruptedException e) {
						logger.error("Thread interrupted.", e);
						reconnect();
					}
					
					continue;
				}
				
				pingFailures++;
				if (pingFailures + 1 > maxPingFailures) {
					reconnect();
				}
			}
		}
		
		private void reconnect() {
			stop();
			doReconnect();
		}

		private void doReconnect() {
			close(true);
			
			try {
				connect(new SecretToken(secret));
			} catch (Exception e) {
				ComponentStreamConfig config = (ComponentStreamConfig)streamConfig;
				logger.error(String.format("%s can't reconnect to host %s.",
					config.getComponentName(), config.getHost()), e);
				
				try {
					Thread.sleep(reconnectInterval);
				} catch (InterruptedException e1) {
					logger.error("Thread interrupted.", e);
				}
				
				doReconnect();
			}
		}

		public void stop() {
			pingFailures = 0;
			stop = true;
		}
		
	}
}
