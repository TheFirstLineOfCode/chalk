package com.thefirstlineofcode.chalk.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerTimeout;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.IChatClient.State;
import com.thefirstlineofcode.chalk.core.stanza.IIqListener;
import com.thefirstlineofcode.chalk.core.stanza.IStanzaListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.im.stanza.IMessageListener;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;

public class ChatServices implements IChatServices, IErrorListener, IStanzaListener {
	private static final Logger logger = LoggerFactory.getLogger(ChatServices.class);
	
	private static final String PROPERTY_NAME_DEFAULT_TASK_TIMEOUT = "default.task.timeout";
	
	private static final int DEFAULT_DEFAULT_TASK_TIMEOUT = 1000 * 30;
	
	private IPresenceService presenceService;
	private IMessageService messageService;
	private IIqService iqService;
	private IErrorService errorService;
	private TaskService taskService;
	
	private List<IPresenceListener> presenceListeners;
	private List<IMessageListener> messageListeners;
	private List<IIqListener> iqListeners;
	private List<IErrorListener> errorListeners;
	
	private ConcurrentMap<String, TimeoutTask<?>> tasksMonitor;
	
	private IChatClient chatClient;
	
	private int defaultTaskTimeout;
	
	public ChatServices(IChatClient chatClient) {
		this.chatClient = chatClient;
		
		presenceListeners = new OrderedList<>(new CopyOnWriteArrayList<IPresenceListener>());
		messageListeners = new OrderedList<>(new CopyOnWriteArrayList<IMessageListener>());
		iqListeners = new OrderedList<>(new CopyOnWriteArrayList<IIqListener>());
		errorListeners = new OrderedList<>(new CopyOnWriteArrayList<IErrorListener>());
		
		presenceService = new PresenceService();
		messageService = new MessageService();
		iqService = new IqService();
		errorService = new ErrorService();
		taskService = new TaskService();
		
		tasksMonitor = new ConcurrentHashMap<>();
		
		defaultTaskTimeout = chatClient.getStreamConfig().getProperty(PROPERTY_NAME_DEFAULT_TASK_TIMEOUT,
				DEFAULT_DEFAULT_TASK_TIMEOUT);
	}
	
	private class OrderedList<T> implements List<T> {
		private volatile List<T> original;
		
		public OrderedList(List<T> original) {
			this.original = original;
		}

		@Override
		public synchronized boolean add(T e) {
			List<T> tmp = new ArrayList<>();
			
			tmp.addAll(original);
			boolean result = tmp.add(e);
			Collections.sort(tmp, new OrderComparator<>());
			
			original = new CopyOnWriteArrayList<>(tmp);
			
			return result;
		}

		@Override
		public void add(int index, T element) {
			original.add(index, element);
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			return original.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends T> c) {
			return original.addAll(index, c);
		}

		@Override
		public void clear() {
			original.clear();
		}

		@Override
		public boolean contains(Object o) {
			return original.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return original.containsAll(c);
		}

		@Override
		public T get(int index) {
			return original.get(index);
		}

		@Override
		public int indexOf(Object o) {
			return original.indexOf(o);
		}

		@Override
		public boolean isEmpty() {
			return original.isEmpty();
		}

		@Override
		public Iterator<T> iterator() {
			return original.iterator();
		}

		@Override
		public int lastIndexOf(Object o) {
			return original.lastIndexOf(o);
		}

		@Override
		public ListIterator<T> listIterator() {
			return original.listIterator();
		}

		@Override
		public ListIterator<T> listIterator(int index) {
			return original.listIterator(index);
		}

		@Override
		public boolean remove(Object o) {
			return original.remove(o);
		}

		@Override
		public T remove(int index) {
			return original.remove(index);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return original.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return original.retainAll(c);
		}

		@Override
		public T set(int index, T element) {
			return original.set(index, element);
		}

		@Override
		public int size() {
			return original.size();
		}

		@Override
		public List<T> subList(int fromIndex, int toIndex) {
			return original.subList(fromIndex, toIndex);
		}

		@Override
		public Object[] toArray() {
			return original.toArray();
		}

		@Override
		public <E> E[] toArray(E[] a) {
			return original.toArray(a);
		}
		
	}
	
	private class OrderComparator<T> implements Comparator<T> {
		@Override
		public int compare(T arg0, T arg1) {
			return getAcceptableOrder(arg0) - getAcceptableOrder(arg1);
		}
		
		private int getAcceptableOrder(Object object) {
			if (object instanceof IOrder) {
				int order = ((IOrder)object).getOrder();
				
				if (order >= IOrder.ORDER_MIN && order <= IOrder.ORDER_MAX) {
					return order;
				} else if (order < IOrder.ORDER_MIN) {
					return IOrder.ORDER_MIN;
				} else {
					return IOrder.ORDER_MAX;
				}
			}
			
			return IOrder.ORDER_NORMAL;
		}
		
	}

	@Override
	public IStream getStream() {
		return chatClient.getStream();
	}

	@Override
	public ITaskService getTaskService() {
		return taskService;
	}

	@Override
	public IPresenceService getPresenceService() {
		return presenceService;
	}

	@Override
	public IMessageService getMessageService() {
		return messageService;
	}

	@Override
	public IIqService getIqService() {
		return iqService;
	}

	@Override
	public IErrorService getErrorService() {
		return errorService;
	}

	@Override
	public <T> T createApi(Class<T> apiType) {
		return chatClient.createApi(apiType);
	}

	private class ErrorService implements IErrorService {

		@Override
		public void addErrorListener(IErrorListener listener) {
			errorListeners.add(listener);
		}

		@Override
		public void removeErrorListener(IErrorListener listener) {
			errorListeners.remove(listener);
		}

		@Override
		public List<IErrorListener> getErrorListeners() {
			return errorListeners;
		}
		
		@Override
		public void send(IError error) {
			getStream().send(error);
		}
	}
	
	private class IqService implements IIqService, IIqListener {
		private ConcurrentMap<Protocol, IIqListener> protocolToListeners;
		
		public IqService() {
			iqListeners.add(this);
			protocolToListeners = new ConcurrentHashMap<>();
		}

		@Override
		public void send(Iq iq) {
			if (chatClient.isConnected())
				getStream().send(iq);
		}

		@Override
		public void addListener(IIqListener listener) {
			iqListeners.add(listener);
		}

		@Override
		public void removeListener(IIqListener listener) {
			iqListeners.remove(listener);
		}

		@Override
		public void addListener(Protocol protocol, IIqListener listener) {
			protocolToListeners.put(protocol, listener);
		}

		@Override
		public void removeListener(Protocol protocol) {
			protocolToListeners.remove(protocol);
		}

		@Override
		public void received(Iq iq) {
			if (iq.getObject() == null) {
				return;
			}
			
			Protocol protocol = iq.getObjectProtocol(iq.getObject().getClass());
			IIqListener listener = protocolToListeners.get(protocol);
			
			if (listener != null) {
				listener.received(iq);
			}
		}

		@Override
		public List<IIqListener> getListeners() {
			return Collections.unmodifiableList(iqListeners);
		}
		
		@Override
		public IIqListener getListener(Protocol protocol) {
			return protocolToListeners.get(protocol);
		}
		
	}
	
	private class PresenceService implements IPresenceService {
		private Presence current;

		@Override
		public void send(Presence presence) {
			if (logger.isTraceEnabled()) {
				logger.trace("ready to send presence. state is {}", chatClient.getState());
			}
			
			if (chatClient.getState() == State.CONNECTED) {
				getStream().send(presence);
				
				if (isBroadcastPresence(presence)) {
					current = presence;
				}
			}
		}

		private boolean isBroadcastPresence(Presence presence) {
			return presence.getTo() == null &&
					(presence.getType() == null ||
						presence.getType() == Presence.Type.UNAVAILABLE);
		}

		@Override
		public void addListener(IPresenceListener listener) {
			presenceListeners.add(listener);
		}

		@Override
		public void removeListener(IPresenceListener listener) {
			presenceListeners.remove(listener);
		}

		@Override
		public Presence getCurrent() {
			return current;
		}

		@Override
		public List<IPresenceListener> getListeners() {
			return presenceListeners;
		}
		
	}
	
	private class MessageService implements IMessageService {

		@Override
		public void send(Message message) {
			if (logger.isTraceEnabled()) {
				logger.trace("send message. state is {}", chatClient.getState());
			}
			
			if (chatClient.getState() == State.CONNECTED) {
				getStream().send(message);
			}
		}

		@Override
		public void addListener(IMessageListener listener) {
			messageListeners.add(listener);
		}

		@Override
		public void removeListener(IMessageListener listener) {
			messageListeners.remove(listener);
		}

		@Override
		public List<IMessageListener> getListeners() {
			return messageListeners;
		}
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void received(Stanza stanza) {
		if (stanza instanceof Iq) {
			for (IIqListener listener : iqListeners) {
				listener.received((Iq)stanza);
			}
		} else if (stanza instanceof Message) {
			for (IMessageListener listener : messageListeners) {
				listener.received((Message)stanza);
			}
		} else {
			for (IPresenceListener listener : presenceListeners) {
				listener.received((Presence)stanza);
			}
		}
		
		if (stanza.getId() != null) {
			TimeoutTask timeoutTask = tasksMonitor.remove(stanza.getId());
			if (timeoutTask != null) {
				processResponse((ITask)timeoutTask.task, new TaskStream(timeoutTask.task, getStream()), stanza);
			}
		}
	}
	
	private class TaskStream<T extends Stanza> implements IUnidirectionalStream<T> {
		private ITask<T> task;
		private IStream stream;
		
		public TaskStream(ITask<T> task, IStream stream) {
			this.stream = stream;
			this.task = task;
		}
		
		@Override
		public void send(T stanza) {
			send(stanza, defaultTaskTimeout);
		}

		@Override
		public void send(T stanza, int timeout) {
			if (chatClient.getState() != State.CONNECTED)
				return;
			
			if (stanza.getId() != null) {
				tasksMonitor.put(stanza.getId(), createTimeoutTask(stanza, timeout, task));
			}
			
			stream.send(stanza);
		}
		
		private <V extends Stanza> TimeoutTask<V> createTimeoutTask(V stanza, int timeout, ITask<V> task) {
			return new TimeoutTask<>(stanza.getId(), stanza, (System.currentTimeMillis() + timeout), task);
		}
		
	}
	
	private class TimeoutTask<T extends Stanza> {
		public String id;
		public T stanza;
		public long timeout;
		public ITask<T> task;
		
		public TimeoutTask(String id, T stanza, long timeout, ITask<T> task) {
			this.id = id;
			this.stanza = stanza;
			this.timeout = timeout;
			this.task = task;
		}
	}
	
	private <T extends Stanza> void processResponse(ITask<T> task, IUnidirectionalStream<T> stream, T stanza) {
		try {
			task.processResponse(stream, stanza);
		} catch (RuntimeException e) {
			processException(e);
		}
	}
	
	private void processException(RuntimeException e) {
		if (chatClient.getExceptionHandler() != null) {
			chatClient.getExceptionHandler().process(e);
		}
		
		throw e;
	}

	@Override
	public void occurred(IError error) {
		for (IErrorListener listener : errorListeners) {
			listener.occurred(error);
		}
		
		if (error instanceof StanzaError) {
			StanzaError stanzaError = (StanzaError)error;
			
			if (stanzaError.getId() != null) {
				TimeoutTask<?> timeoutTask = tasksMonitor.remove(stanzaError.getId());
				if (timeoutTask != null) {
					if (processError(stanzaError, timeoutTask))
						return;
				}
			}
		}
		
		if (chatClient.getDefaultErrorHandler() != null) {
			chatClient.getDefaultErrorHandler().process(error);
		}
	}
	
	private <T extends Stanza> boolean processError(StanzaError stanzaError, TimeoutTask<T> timeoutTask) {
		try {
			return timeoutTask.task.processError(createTaskStream(timeoutTask.task, getStream()), stanzaError);
		} catch (RuntimeException e) {
			processException(e);
		}
		
		return false;
	}
	
	private <T extends Stanza> TaskStream<T> createTaskStream(ITask<T> task, IStream stream) {
		return new TaskStream<>(task, stream);
	}
	
	public void start() {
		taskService.start();
	}
	
	public void stop() {
		taskService.stop();
	}
	
	private class TaskService implements ITaskService {
		private volatile boolean stopFlag = false;
		private TimeoutTaskRunnable timeoutTaskRunnable;
		private ExecutorService taskThreadPool;
		private ITimeoutHandler timeoutHandler;
		
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void execute(ITask<?> task) {
			synchronized (ChatServices.this) {
				if (chatClient.getState() == State.CONNECTED && !stopFlag) {
					taskThreadPool.execute(createTaskTriggerThread(task));
				} else {
					new Thread(new AlwaysTimeoutTaskRunnable(task)).start();
				}
			}
			
		}
		
		private class AlwaysTimeoutTaskRunnable<Y extends Stanza> implements Runnable {
			private ITask<Y> task;
			
			public AlwaysTimeoutTaskRunnable(ITask<Y> task) {
				this.task = task;
			}
			
			@Override
			public void run() {
				try {					
					task.trigger(new AlwaysTimeoutStream<>(task));
				} catch (RuntimeException e) {
					processException(e);
				}
			}
			
			private class AlwaysTimeoutStream<Z extends Stanza> implements IUnidirectionalStream<Z> {
				private ITask<Z> task;
				
				public AlwaysTimeoutStream(ITask<Z> task) {
					this.task = task;
				}
				
				@Override
				public void send(Z stanza) {
					send(stanza, getDefaultTimeout());
				}

				@Override
				public void send(final Z stanza, int timeout) {
					new Timer().schedule(new TimerTask() {

						@Override
						public void run() {
							task.processTimeout(AlwaysTimeoutStream.this, stanza);
						}
						
					}, timeout);
				}
				
			}
		}

		private <T extends Stanza> TaskTriggerThread<T> createTaskTriggerThread(ITask<T> task) {
			return new TaskTriggerThread<>(task, createTaskStream(task, getStream()));
		}
		
		private class TaskTriggerThread<T extends Stanza> implements Runnable {
			private ITask<T> task;
			private IUnidirectionalStream<T> stream;
			
			public TaskTriggerThread(ITask<T> task, IUnidirectionalStream<T> stream) {
				this.task = task;
				this.stream = stream;
			}
			
			
			@Override
			public void run() {
				try {
					task.trigger(stream);					
				} catch (RuntimeException e) {
					processException(e);
				}
			}
			
		}

		@Override
		public void setDefaultTimeout(int timeout) {
			ChatServices.this.defaultTaskTimeout = timeout;
		}

		@Override
		public int getDefaultTimeout() {
			return ChatServices.this.defaultTaskTimeout;
		}
		
		public void start() {
			stopFlag = false;
			if (taskThreadPool == null || taskThreadPool.isShutdown()) {
				taskThreadPool = createTaskThreadPool();
			}
			
			timeoutTaskRunnable = new TimeoutTaskRunnable();
			new Thread(timeoutTaskRunnable, "ChatServices Timeout Task").start();
		}
		
		public ExecutorService createTaskThreadPool() {
			return Executors.newCachedThreadPool();
		}
		
		@SuppressWarnings("unlikely-arg-type")
		public void stop() {
			stopFlag = true;
			
			if (timeoutTaskRunnable != null) {
				timeoutTaskRunnable.stop();
				timeoutTaskRunnable = null;
			}
			
			if (!tasksMonitor.isEmpty()) {
				List<TimeoutTask<?>> tasks = new ArrayList<>();
				for (TimeoutTask<?> timeoutTask : tasksMonitor.values()) {
					if (!tasks.contains(timeoutTask.task)) {
						tasks.add(timeoutTask);
					}
				}
				
				for (TimeoutTask<?> task : tasks) {
					task.task.interrupted();
				}
				
				tasksMonitor.clear();
			}
			
			if (taskThreadPool != null) {
				taskThreadPool.shutdown();
			}
		}

		@Override
		public void setDefaultTimeoutHandler(ITimeoutHandler timeoutHandler) {
			this.timeoutHandler = timeoutHandler;
		}
		
		public ITimeoutHandler getDefaultTimeoutHandler() {
			return timeoutHandler;
		}
		
		@Override
		public <K extends Stanza, V> V execute(ISyncTask<K, V> task) throws ErrorException {
			return new SyncTaskTemplate<K, V>().execute(task);
		}
	}
	
	private class SyncTaskTemplate<K extends Stanza, V> {
		public V execute(ISyncTask<K, V> syncTask) throws ErrorException {
			SyncTaskWrapper task = new SyncTaskWrapper(syncTask);
			if (logger.isTraceEnabled()) {
				logger.trace("Ready to execute a task. Task class is {}.", syncTask.getClass().getName());
			}
			
			taskService.execute(task);
			
			try {
				return task.getResult();
			} catch (InterruptedException e) {
				throw new RuntimeException("Sync task error.", e);
			}
		}
		
		private class SyncTaskWrapper implements ITask<K> {
			private ISyncTask<K, V> syncTask;
			private StanzaError error;
			private boolean timeout;
			private V result;
			
			private Lock lock = new ReentrantLock();
			private Condition condition = lock.newCondition();
			
			private SyncTaskWrapper(ISyncTask<K, V> syncTask) {
				this.syncTask = syncTask;
				timeout = false;
			}

			@Override
			public void trigger(IUnidirectionalStream<K> stream) {
				syncTask.trigger(stream);
			}

			@Override
			public void processResponse(IUnidirectionalStream<K> stream, K stanza) {
				try {
					result = syncTask.processResult(stanza);
				} catch (Exception e) {
					throw new RuntimeException("Sync task failed to process result.", e);
				} finally {
					lock.lock();
					try {
						condition.signal();
					} finally {
						lock.unlock();
					}
				}
			}

			@Override
			public boolean processError(IUnidirectionalStream<K> stream, StanzaError error) {
				this.error = error;
				
				lock.lock();
				try {
					condition.signal();
				} finally {
					lock.unlock();
				}
				
				return true;
			}

			@Override
			public boolean processTimeout(IUnidirectionalStream<K> stream, K stanza) {
				timeout = true;
				
				lock.lock();
				try {
					condition.signal();
				} finally {
					lock.unlock();
				}
				
				return true;
			}

			@Override
			public void interrupted() {
				timeout = true;
				
				lock.lock();
				try {
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
			
			public V getResult() throws InterruptedException, ErrorException {
				lock.lock();
				try {
					V ret = processResult();
					if (ret != null)
						return ret;
					
					int timeout = 60 * 5 * 1000;
					while (timeout > 0) {
						int waitingTime = Math.min(timeout, 200);
						if (!condition.await(waitingTime, TimeUnit.MILLISECONDS)) {
							timeout -= waitingTime;
							
							V result = processResult();
							if (result != null)
								return result;
						} else {
							return processResult();
						}
					}
					
					throw new ErrorException(new RemoteServerTimeout());
				} finally {
					lock.unlock();
				}
				
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
			
		}
	}
	
	private class TimeoutTaskRunnable implements Runnable {
		private volatile boolean stopFlag = false;

		@Override
		public void run() {
			while (!stopFlag)  {
				for (TimeoutTask<?> timeoutTask : tasksMonitor.values()) {
					long now = System.currentTimeMillis();
					if (timeoutTask.timeout < now) {
						TimeoutTask<?> remove = tasksMonitor.remove(timeoutTask.id);
						boolean timeoutHandled = false;
						if (remove != null) {
							timeoutHandled = processTimeout(remove);
						}
						
						if (!timeoutHandled) {
							taskService.getDefaultTimeoutHandler().process(remove.stanza);
						}
					}
				}
				
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

		private <T extends Stanza> boolean processTimeout(TimeoutTask<T> remove) {
			boolean timeoutHandled = false;
			try {
				timeoutHandled = remove.task.processTimeout(new TaskStream<>(remove.task, getStream()), remove.stanza);
			} catch (RuntimeException e) {
				processException(e);
			}
			
			return timeoutHandled;
		}
		
		public void stop() {
			stopFlag = true;
		}
		
	}
	
	@Override
	public IOxmFactory getOxmFactory() {
		return chatClient.getOxmFactory();
	}
	
}
