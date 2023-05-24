package com.thefirstlineofcode.chalk.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerTimeout;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.stanza.IIqListener;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;

public class SyncOperationTemplate<K extends Stanza, V> {
	private static final String PROPERTY_NAME_DEFAULT_SYNC_OPERATION_TIMEOUT = "default.sync.operation.timeout";
	private static final int DEFAULT_DEFAULT_SYNC_OPERATION_TIMEOUT = 1000 * 30;
	
	private int defaultSyncOperationTimeout;
	
	private IChatServices chatServices;
	
	public SyncOperationTemplate(IChatServices chatServices) {
		this.chatServices = chatServices;
		defaultSyncOperationTimeout = chatServices.getStream().getStreamConfig().getProperty(
			PROPERTY_NAME_DEFAULT_SYNC_OPERATION_TIMEOUT, DEFAULT_DEFAULT_SYNC_OPERATION_TIMEOUT);
	}
	
	@SuppressWarnings("unchecked")
	public V execute(ISyncOperation<K, V> operation) throws ErrorException {
		if (operation instanceof ISyncIqOperation) {
			return new IqOperationExecutor((ISyncIqOperation<V>)operation).execute();
		} else if (operation instanceof ISyncPresenceOperation) {
			return new PresenceOperationExecutor((ISyncPresenceOperation<V>)operation).execute();
		} else if (operation instanceof ISyncMessageOperation) {
			return new MessageOperationExecutor((ISyncMessageOperation<V>)operation).execute();
		} else {
			throw new IllegalArgumentException(String.format("Unspported executor type. Only %s, %s and %s are supported.",
					ISyncIqOperation.class.getName(), ISyncPresenceOperation.class.getName(),
						ISyncMessageOperation.class.getName()));
		}
	}
	
	private abstract class OperationExecutor implements IErrorListener, IOrder {
		private ISyncOperation<K, V> operation;
		private StanzaError error;
		private boolean timeout;
		private V result;
		private Timer timer;
		private TimerTask timerTask;
		
		private Lock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		
		public OperationExecutor(ISyncOperation<K, V> operation) {
			this.operation = operation;
		}
		
		@Override
		public void occurred(IError error) {
			if (!(error instanceof StanzaError)) {
				return;
			}
			
			StanzaError stanzaError = (StanzaError)error;
			if (operation.isErrorOccurred(stanzaError)) {
				lock.lock();
				try {
					this.error = stanzaError;
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
		}
		
		public V execute() throws ErrorException {
			chatServices.getErrorService().addErrorListener(this);
			bindListener();
			IUnidirectionalStream<K> stream = createStream();
			operation.trigger(stream);
			
			V ret = processResult();
			if (ret != null) {
				clean();
				return ret;
			}
			
			lock.lock();
			try {
				condition.await(60 * 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException("sync operation interrupted", e);
			} finally {
				lock.unlock();
			}
			
			clean();
			return processResult();
		}

		private void clean() {
			timer.cancel();
			chatServices.getErrorService().removeErrorListener(this);
			unbindListener();
		}

		private V processResult() throws ErrorException {
			if (timeout) {
				throw new ErrorException(new RemoteServerTimeout());
			}
			
			if (error != null) {
				throw new ErrorException(error);
			}
			
			return result;
		}
		
		private IUnidirectionalStream<K> createStream() {
			return new TimerStream(this);
		}
		
		public void resetTimeout(long timeout) {
			lock.lock();
			try {
				if (timer == null) {
					timer = new Timer();
				}
			} finally {
				lock.unlock();
			}
			
			if (timerTask != null) {
				timerTask.cancel();
			}
			
			lock.lock();
			try {
				if (this.timeout) {
					return;
				}
			} finally {
				lock.unlock();
			}
				
			timerTask = new TimerTask() {
				@Override
				public void run() {
					lock.lock();
					try {
						OperationExecutor.this.timeout = true;
						condition.signal();
					} finally {
						lock.unlock();
					}
				}
			};
				
			timer.schedule(timerTask, timeout);
		}
		
		protected void stanzaReceived(K stanza) {
			if (!operation.isResultReceived(stanza)) {
				return;
			}
			
			result = operation.processResult(stanza);
			
			lock.lock();
			try {
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
		
		@Override
		public int getOrder() {
			return IOrder.ORDER_MIN;
		}
		
		protected abstract void bindListener();
		protected abstract void unbindListener();
	}
	
	private class TimerStream implements IUnidirectionalStream<K> {
		private OperationExecutor executor;
		
		public TimerStream(OperationExecutor executor) {
			this.executor = executor;
		}

		@Override
		public void send(K stanza) {
			send(stanza, defaultSyncOperationTimeout);
		}

		@Override
		public void send(K stanza, int timeout) {
			executor.resetTimeout(timeout);
			chatServices.getStream().send(stanza);
		}
		
	}
	
	private class IqOperationExecutor extends OperationExecutor implements IIqListener {
		@SuppressWarnings("unchecked")
		public IqOperationExecutor(ISyncIqOperation<V> operation) {
			super((ISyncOperation<K, V>)operation);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void received(Iq iq) {
			stanzaReceived((K)iq);
		}

		@Override
		protected void bindListener() {
			chatServices.getIqService().addListener(this);
		}
		
		@Override
		protected void unbindListener() {
			chatServices.getIqService().removeListener(this);
		}
	}
	
	private class PresenceOperationExecutor extends OperationExecutor implements IPresenceListener {
		@SuppressWarnings("unchecked")
		public PresenceOperationExecutor(ISyncPresenceOperation<V> operation) {
			super((ISyncOperation<K, V>)operation);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void received(Presence presence) {
			stanzaReceived((K)presence);
		}

		@Override
		protected void bindListener() {
			chatServices.getPresenceService().addListener(this);
		}
		
		@Override
		protected void unbindListener() {
			chatServices.getPresenceService().removeListener(this);
		}
	}
	
	private class MessageOperationExecutor extends OperationExecutor implements IMessageListener {
		@SuppressWarnings("unchecked")
		public MessageOperationExecutor(ISyncMessageOperation<V> operation) {
			super((ISyncOperation<K, V>)operation);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void received(Message message) {
			stanzaReceived((K)message);
		}

		@Override
		protected void bindListener() {
			chatServices.getMessageService().addListener(this);
		}
		
		@Override
		protected void unbindListener() {
			chatServices.getMessageService().removeListener(this);
		}
	}
}
