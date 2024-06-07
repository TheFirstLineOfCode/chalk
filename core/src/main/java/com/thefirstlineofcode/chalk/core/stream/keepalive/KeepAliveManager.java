package com.thefirstlineofcode.chalk.core.stream.keepalive;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.Stream;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public class KeepAliveManager implements IKeepAliveManager, IConnectionListener {
	private static final Logger logger = LoggerFactory.getLogger(KeepAliveManager.class);
	
	public static final byte[] BYTES_OF_HEART_BEAT_CHAR =  getBytesOfHeartBeatChar();
	
	protected KeepAliveConfig config;
	protected IStream stream;
	protected boolean started;
	protected boolean useBinaryFormat;
	protected KeepAliveThread keepAliveThread;
	
	protected IKeepAliveCallback callback;
	protected long lastMessageReceivedTime;
	protected long lastMessageSentTime;
	
	public KeepAliveManager(IStream stream, KeepAliveConfig config) {
		if (stream == null)
			throw new IllegalArgumentException("Null stream.");
		
		useBinaryFormat = false;
		String messageFormat = System.getProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT);
		if (Constants.MESSAGE_FORMAT_BINARY.equals(messageFormat)) {
			useBinaryFormat = true;
		}
		
		if (config == null)
			throw new IllegalArgumentException("Null keep-alive config.");
		
		this.config = config;
		this.stream = stream;
		
		this.callback = createDefaultCallback();
		started = false;
	}
	
	private static byte[] getBytesOfHeartBeatChar() {
		try {
			return String.valueOf(CHAR_HEART_BEAT).getBytes(Constants.DEFAULT_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("%s not supported!", Constants.DEFAULT_CHARSET), e);
		}
	}
	
	protected IKeepAliveCallback createDefaultCallback() {
		return new DefaultKeepAliveCallback();
	}
	
	private class DefaultKeepAliveCallback implements IKeepAliveCallback {

		@Override
		public void received(Date currentTime, boolean isHeartbeats) {
			lastMessageReceivedTime = currentTime.getTime();
			
			if (logger.isTraceEnabled())
				logger.trace("Keep-alive thread({}) has received a message at {}.",
						stream.getJid(), currentTime.toString());
		}
		
		@Override
		public void sent(Date currentTime, boolean isHeartbeats) {
			lastMessageSentTime = currentTime.getTime();
			
			if (logger.isTraceEnabled())
				logger.trace("Keep-alive thread({}) has sent a heartbeat at {}.",
						stream.getJid(), currentTime.toString());
		}

		@Override
		public void timeout(final IStream stream) {
			new Thread() {
				@Override
				public void run() {
					stream.close(false);
					((Stream)stream).exceptionOccurred(new ConnectionException(ConnectionException.Type.CONNECTION_CLOSED));
				}
			}.start();
		}
		
	}
	
	@Override
	public KeepAliveConfig getConfig() {
		return config;
	}

	@Override
	public void changeConfig(KeepAliveConfig config) {
		if (!isStarted()) {
			this.config = config;
			return;
		}
		
		stop();
		this.config = config;
		start();
	}

	@Override
	public void start() {
		if (isStarted())
			return;
		
		doStart();
	}
	
	protected void doStart() {
		if (keepAliveThread == null) {
			keepAliveThread = new KeepAliveThread(stream.getJid());
		}
		
		keepAliveThread.start();
		if (logger.isInfoEnabled()) {
			logger.info("Keep-alive thread({}) has started.", stream.getJid());
		}
		
		stream.getConnection().addListener(this);
	}
	
	protected class KeepAliveThread extends Thread {
		private boolean stop;
		private JabberId jid;
		
		KeepAliveThread(JabberId jid) {
			super("Client Keep-alive Thread");
			this.jid = jid;
		}
		
		@Override
		public void run() {
			if (jid == null)
				return;
			
			started = true;
			
			lastMessageSentTime = currentTime().getTime();
			lastMessageReceivedTime = currentTime().getTime();
			while (!stop) {
				if (stream.isClosed()) {
					if (logger.isWarnEnabled()) {
						logger.warn("Keep-alive thread({}) can't work. The stream has closed.", jid);
					}
					return;
				}
				
				if (getClientInactiveTime() > config.getClientKeepAliveInterval() ||
						getServerInactiveTime() > config.getServerKeepAliveInterval()) {
					if (useBinaryFormat) {
						stream.getConnection().write(new byte[] {BYTE_HEART_BEAT});
					} else {
						stream.getConnection().write(BYTES_OF_HEART_BEAT_CHAR);
					}
					
					if (logger.isTraceEnabled()) {
						logger.trace("Keep-alive thread({}) sent a heart beat.", jid);
					}
					
					callback.sent(currentTime(), true);
				}
				
				try {
					Thread.sleep(config.getCheckingInterval());
				} catch (InterruptedException e) {
					throw new RuntimeException(String.format("Keep-alive thread(%s) throws an exception.", jid), e);
				}

				if (getServerInactiveTime() > config.getTimeout()) {
					if (logger.isWarnEnabled())
						logger.warn("Keeping-alive thread({}) has timeouted. Keep-alive callback's timeout() will be called.", jid);
					
					callback.timeout(stream);
					
					stop = true;
				}
			}
			
			if (logger.isInfoEnabled()) {
				logger.info("Keep-alive thread({}) has stopped.", jid);
			}
			started = false;
		}

		protected long getClientInactiveTime() {
			return currentTime().getTime() - lastMessageSentTime;
		}

		protected long getServerInactiveTime() {
			return currentTime().getTime() - lastMessageReceivedTime;
		}
		
		public void exit() {
			stop = true;
		}
	}
	
	protected Date currentTime() {
		return Calendar.getInstance().getTime();
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public void exceptionOccurred(ConnectionException exception) {
		// Do nothing.
	}

	@Override
	public void messageReceived(String message) {
		callback.received(currentTime(), false);
	}

	@Override
	public void messageSent(String message) {
		callback.sent(currentTime(), false);
	}

	@Override
	public void stop() {
		if (!isStarted())
			return;
		
		stream.getConnection().removeListener(this);
		
		if (keepAliveThread == null)
			throw new IllegalStateException("Null keep alive thread.");
		
		keepAliveThread.exit();
		try {
			keepAliveThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Is thread interrupted???", e);
		}
		
		keepAliveThread = null;
	}

	@Override
	public void heartBeatsReceived(int length) {
		callback.received(currentTime(), true);
	}
	
	@Override
	public void setCallback(IKeepAliveCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public IKeepAliveCallback getCallback() {
		return callback;
	}

}
