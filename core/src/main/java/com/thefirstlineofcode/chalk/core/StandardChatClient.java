package com.thefirstlineofcode.chalk.core;

import java.security.cert.Certificate;

import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationCallback;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationFailure;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;
import com.thefirstlineofcode.chalk.core.stream.IStandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.IStreamer;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.core.stream.negotiants.sasl.SaslError;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.IPeerCertificateTruster;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.SocketConnection;

public class StandardChatClient extends AbstractChatClient implements IAuthenticationCallback {
	protected IPeerCertificateTruster peerCertificateTruster;
	private IAuthenticationFailure authFailure;
	
	public StandardChatClient(StandardStreamConfig streamConfig) {
		super(streamConfig, null);
	}
	
	public StandardChatClient(StandardStreamConfig streamConfig, IConnection connection) {
		super(streamConfig, connection);
	}
	
	public void setPeerCertificateTruster(IPeerCertificateTruster peerCertificateTruster) {
		this.peerCertificateTruster = peerCertificateTruster;
	}

	public IPeerCertificateTruster getPeerCertificateTruster() {
		return peerCertificateTruster;
	}
	
	public void connect(String userName, String password) throws ConnectionException,
			AuthFailureException, NegotiationException {
		connect(new UsernamePasswordToken(userName, password));
	}
	
	@Override
	public void connect(IAuthenticationToken authToken) throws ConnectionException, AuthFailureException {
		if (!(authToken instanceof UsernamePasswordToken)) {
			throw new IllegalArgumentException(String.format("Auth token type must be %s.", UsernamePasswordToken.class.getName()));
		}
		
		if (authFailure != null && authFailure.isRetriable()) {
			authFailure.retry(authToken);
			
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Unexpected exception.", e);
				}
				
			}
		} else {
			close();
			
			try {
				super.connect(authToken);
				
				if (authFailure != null) {
					throw new AuthFailureException(authFailure.getErrorCondition());
				}
			} catch (RuntimeException e) {
				if ((e.getCause() instanceof NegotiationException) &&
						isMaxFailureCountExcceed((NegotiationException)e.getCause())) {
					throw new AuthFailureException();
				}
				
				throw e;
			}
		}		
	}
	
	protected IStreamer createStreamer(StreamConfig streamConfig, IConnection connection) {
		IStandardStreamer standardStreamer = new StandardStreamer((StandardStreamConfig)streamConfig, connection);
		standardStreamer.setNegotiationListener(this);
		standardStreamer.setAuthenticationCallback(this);
		
		if (peerCertificateTruster != null) {
			standardStreamer.setPeerCertificateTruster(peerCertificateTruster);
		} else {
			// always trust peer certificate
			standardStreamer.setPeerCertificateTruster(new IPeerCertificateTruster() {				
				@Override
				public boolean accept(Certificate[] certificates) {
					return true;
				}
			});
		}
		
		return standardStreamer;
	}
	
	@Override
	public synchronized void close() {
		if (state == State.CLOSED)
			return;
		
		if (authFailure != null) {
			authFailure.abort();
			authFailure = null;
		}
		
		super.close();			
	}
	
	@Override
	public void failed(IAuthenticationFailure authFailure) {
		this.authFailure = authFailure;
		
		synchronized (this) {
			notify();
		}
	}
	
	@Override
	protected void doConnect(IAuthenticationToken authToken) {
		if (authFailure != null && authFailure.isRetriable()) {
			authFailure.retry(authToken);
			authFailure = null;
		} else {
			super.doConnect(authToken);
		}
	}
	
	protected boolean isMaxFailureCountExcceed(NegotiationException ne) {
		Object additionalErrorInfo =  ne.getAdditionalErrorInfo();
		if ((additionalErrorInfo instanceof SaslError) &&
				(additionalErrorInfo == SaslError.MAX_FAILURE_COUNT_EXCCEED)) {
			return true;
		}
		
		return false;
	}

	@Override
	protected IConnection createConnection(StreamConfig streamConfig) {
		String messageFormat = streamConfig.getProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT,
				Constants.MESSAGE_FORMAT_XML);
		return new SocketConnection(messageFormat);
	}
}
