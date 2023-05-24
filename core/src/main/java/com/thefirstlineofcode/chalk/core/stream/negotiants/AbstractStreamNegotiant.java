package com.thefirstlineofcode.chalk.core.stream.negotiants;

import java.util.EmptyStackException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.error.StreamError;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;

public abstract class AbstractStreamNegotiant extends ConnectionListenerAdapter implements IStreamNegotiant {
	protected static final long DEFAULT_READ_RESPONSE_TIMEOUT = 1000 * 15;
	protected static final long DEFAULT_READ_RESPONSE_INTERVAL = 200;
	
	protected Queue<String> responses = new LinkedBlockingQueue<>();
	protected ConnectionException exception;
	protected Object lock = new Object();
	
	protected long readResponseTimeout = DEFAULT_READ_RESPONSE_TIMEOUT;
	
	@Override
	public synchronized void negotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		try {
			doNegotiate(context);
		} catch (RuntimeException e) {
			throw e;
		}
	}
	
	@Override
	public void exceptionOccurred(ConnectionException exception) {
		this.exception = exception;
		synchronized (lock) {
			lock.notify();
		}
	}
	
	@Override
	public void messageReceived(String message) {
		synchronized (lock) {
			responses.offer(message);
			lock.notify();
		}
	}
	
	@Override
	public void messageSent(String message) {
		// Do nothing.
	}
	
	private void waitResponse(long timeout) throws ConnectionException {
		try {
			synchronized (lock) {
				lock.wait(timeout);
			}
		} catch (InterruptedException e) {
			// ignore
		}
		
		if (exception != null) {
			ConnectionException thrown = exception;
			exception = null;
			
			throw thrown;
		}
	}
	
	protected String readResponse() throws ConnectionException {
		return readResponse(getReadResponseTimeout());
	}
	
	protected synchronized String readResponse(long timeout) throws ConnectionException {
		long waitedTime = 0;
		while (Long.compare(waitedTime, timeout) < 0) {
			if (responses.size() == 0 && exception == null) {
				long waitingTime = Math.min(timeout - waitedTime, DEFAULT_READ_RESPONSE_INTERVAL);
				waitResponse(waitingTime);
				waitedTime += waitingTime;
			}
			
			if (responses.size() != 0) {
				try {
					return responses.poll();
				} catch (EmptyStackException e) {
					throw new RuntimeException("null response???");
				}
			}
		}
		
		throw new ConnectionException(ConnectionException.Type.READ_RESPONSE_TIMEOUT);
	}
	
	protected void processError(IError error, INegotiationContext context, IOxmFactory oxmFactory)
				throws ConnectionException, NegotiationException {
		if (error instanceof StreamError) {
			Stream closeStream = (Stream)oxmFactory.parse(readResponse());
			
			if (closeStream != null && closeStream.isClose()) {
				context.close();
			}
		}
		
		throw new NegotiationException(this, error);
		
	}
	
	public void setReadResponseTimeout(long timeout) {
		this.readResponseTimeout = timeout;
	}
	
	public long getReadResponseTimeout() {
		return readResponseTimeout;
	}
	
	protected abstract void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException;
}
