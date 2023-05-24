package com.thefirstlineofcode.chalk.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.parsing.IParserFactory;
import com.thefirstlineofcode.basalt.oxm.translating.ITranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;
import com.thefirstlineofcode.chalk.core.stream.INegotiationListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.IStreamer;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;
import com.thefirstlineofcode.chalk.network.IConnection;
import com.thefirstlineofcode.chalk.network.IConnectionListener;
import com.thefirstlineofcode.chalk.network.SocketConnection;

public abstract class AbstractChatClient extends ConnectionListenerAdapter implements IChatClient, INegotiationListener {
	private Logger logger = LoggerFactory.getLogger(AbstractChatClient.class);
	
	protected StreamConfig streamConfig;
	protected State state;
	protected IStreamer streamer;
	protected Exception exception;
	protected IStream stream;
	
	private IErrorHandler errorHandler;
	private IExceptionHandler exceptionHandler;
	
	protected IOxmFactory oxmFactory;
	protected IChatSystem chatSystem;
	protected ChatServices chatServices;
	
	private Map<Class<? extends IPlugin>, CounterPluginWrapper> plugins;
	private ConcurrentMap<Class<?>, Api<?>> apis;
	private List<INegotiationListener> negotiationListeners;
	private List<IConnectionListener> connectionListeners;
	private IConnection connection;
	
	public AbstractChatClient(StreamConfig streamConfig, IConnection connection) {
		if (streamConfig == null)
			throw new IllegalArgumentException("Null stream config.");
		this.streamConfig = streamConfig;
		
		if (connection == null)
			connection = createConnection(streamConfig);
		this.connection = connection;
		connection.addListener(this);
		
		state = State.CLOSED;
		
		negotiationListeners = new OrderedList<>(new ArrayList<INegotiationListener>());
		connectionListeners = new ArrayList<IConnectionListener>();
		
		oxmFactory = createOxmFactory();
		
		chatSystem = new ChatSystem();
		plugins = new HashMap<>();
		apis = new ConcurrentHashMap<>();
		
		chatServices = new ChatServices(this);
	}

	public synchronized State getState() {
		return state;
	}
	
	protected IOxmFactory createOxmFactory() {
		return OxmService.createStandardOxmFactory();
	}
	
	@Override
	public void setStreamConfig(StreamConfig streamConfig) {
		this.streamConfig = streamConfig;
	}

	@Override
	public StreamConfig getStreamConfig() {
		return streamConfig;
	}

	@Override
	public void connect(IAuthenticationToken authToken) throws AuthFailureException, ConnectionException {
		beforeDoConnect();
		
		doConnect(authToken);
		
		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException("Unexpected exception.", e);
			}
			
		}
		
		afterDoConnect();
	}

	private void afterDoConnect() throws ConnectionException {
		if (exception == null) {
			synchronized(this) {				
				state = State.CONNECTED;
			}
		} else {
			// close() will clear the exception, so remember the exception before calling it.
			Exception exception = this.exception;
			close(false);
			
			if (exception instanceof NegotiationException) {
				throw (NegotiationException)exception;
			} else if (exception instanceof ConnectionException) {
				throw (ConnectionException)exception;
			} else {
				throw new RuntimeException("Unexpected exception.", exception);
			}
		}
	}

	private void beforeDoConnect() {
		synchronized (this) {			
			if (state == State.CONNECTED) {
				throw new IllegalStateException("Client has already connected.");
			}
			
			if (state == State.CONNECTING) {
				throw new IllegalStateException("Client is connecting now.");
			}
			
			state = State.CONNECTING;
		}
	}
	
	protected void doConnect(IAuthenticationToken authToken) {
		if (streamConfig == null) {
			throw new IllegalStateException("Null stream config.");
		}
		
		if (connection == null) {
			connection = createConnection(streamConfig);
		}
		connection.addListener(this);
		
		if (logger.isDebugEnabled())
			logger.debug("Chat client is trying to connect to XMPP server({}).", String.format("%s: %d", streamConfig.getHost(), streamConfig.getPort()));
		
		streamer = createStreamer(streamConfig, connection);
		streamer.setConnectionListener(this);
		streamer.setNegotiationListener(this);
		
		streamer.negotiate(authToken);
	}
	
	protected IConnection createConnection(StreamConfig streamConfig) {
		String messageFormat = streamConfig.getProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT,
				Constants.MESSAGE_FORMAT_XML);
		return new SocketConnection(messageFormat);
	}
	
	protected abstract IStreamer createStreamer(StreamConfig streamConfig, IConnection connection);
	
	@Override
	public void register(Class<? extends IPlugin> pluginClass) {
		register(pluginClass, null);
	}
	
	@Override
	public void register(Class<? extends IPlugin> pluginClass, Properties properties) {
		synchronized (plugins) {
			CounterPluginWrapper pluginWrapper;
			if (plugins.containsKey(pluginClass)) {
				pluginWrapper = plugins.get(pluginClass);
			} else {
				pluginWrapper = new CounterPluginWrapper(pluginClass, properties);
			}
			
			pluginWrapper.register();
		}
	}

	@Override
	public void unregister(Class<? extends IPlugin> pluginClass) {
		synchronized (plugins) {
			if (plugins.containsKey(pluginClass)) {
				plugins.get(pluginClass).unregister();
			}
		}
	}
	
	private class CounterPluginWrapper {
		private int count = 0;
		private Class<? extends IPlugin> pluginClass;
		private IPlugin plugin;
		private Properties properties;
		
		public CounterPluginWrapper(Class<? extends IPlugin> pluginClass, Properties properties) {
			this.pluginClass = pluginClass;
			this.properties = properties;
		}
		
		public void register() {
			if (count == 0) {
				try {
					plugin = pluginClass.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new RuntimeException(String.format("Can't initialize plugin %s.", pluginClass), e);
				}
				
				plugin.init(chatSystem, properties);
				
				plugins.put(pluginClass, this);
			}
			
			count++;
		}
		
		public void unregister() {
			count--;
			
			if (count == 0) {
				plugin.destroy(chatSystem);
				
				plugins.remove(pluginClass);
			}
		}
	}

	@Override
	public <T> T createApi(Class<T> apiType) {
		return createApi(apiType, new Class<?>[] {}, new Object[] {});
	}
	
	@Override
	public <T> T createApi(Class<T> apiType, Class<?> constuctorParamType, Object constuctorParam) {
		return createApi(apiType, new Class<?>[] {constuctorParamType}, new Object[] {constuctorParam});
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T createApi(Class<T> apiType, Class<?>[] constuctorParamTypes, Object[] constuctorParams) {
		Api<T> api = (Api<T>)apis.get(apiType);
		
		if (api == null)
			throw new RuntimeException(String.format("Api %s not be registered.", apiType.getName()));
		
		if (api.singleton) {
			if (api.singletonObject != null) {
				return (T)api.singletonObject;
			}
			
			synchronized (api) {
				if (api.singletonObject != null)
					return (T)api.singletonObject;
				
				api.singletonObject = createApiImpl(api.implType, constuctorParamTypes, constuctorParams, api.properties);
				
				return (T)api.singletonObject;
			}
		}
		
		return createApiImpl(api.implType, constuctorParamTypes, constuctorParams, api.properties);
	}
	
	public <T> T createApiImpl(Class<? extends T> apiImplType) {
		return createApiImpl(apiImplType, new Class<?>[0], new Object[0]);
	}
	
	public <T> T createApiImpl(Class<? extends T> apiImplType, Class<?>[] constuctorParamTypes, Object[] constuctorParams) {
		return createApiImpl(apiImplType, constuctorParamTypes, constuctorParams, new Properties());
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T createApiImpl(Class<? extends T> apiImplType, Class<?>[] constuctorParamTypes, Object[] constuctorParams, Properties properties) {
		if (constuctorParamTypes == null || constuctorParams == null)
			throw new IllegalArgumentException("Null constructor param types or constructor params.");
		
		if (constuctorParamTypes.length != constuctorParams.length)
			throw new IllegalArgumentException("Length of constructor param types must equal to length of constrctor params.");
		
		Constructor<?> constructor = null;
		try {
			constructor = apiImplType.getConstructor(getConstuctorParamTypesWithIChatServices(
					constuctorParamTypes));
		} catch (Exception e) {
			// ignore
		}
		
		Object object = null;
		if (constructor != null) {
			try {
				object = constructor.newInstance(getConstuctorParamsWithChatServices(constuctorParams, chatServices));
			} catch (Exception e) {
				throw new RuntimeException("Can't create api object.", e);
			}
		}
		
		if (object == null) {
			try {
				constructor = apiImplType.getConstructor(constuctorParamTypes);
			} catch (Exception e) {
				// ignore
			}
			
			if (constructor == null)
				throw new RuntimeException(String.format(
						"Can't find suitable constructor for API implementation which's type is %s.", apiImplType));
			
			try {
				object = constructor.newInstance(constuctorParams);
			} catch (Exception e) {
				throw new RuntimeException("Can't create api object.", e);
			}
			
			setChatServicesToApiObject(apiImplType, object);
		}
		
		for (Object key : properties.keySet()) {
			Object value = properties.get(key);
			
			try {
				Method writer = getWriter(key.toString(), apiImplType, value.getClass());
				if (writer != null) {
					writer.invoke(object, new Object[] {value});
				} else {
					Field field = apiImplType.getDeclaredField(key.toString());
					if (field != null) {
						boolean oldAccessible = field.isAccessible();
						try {
							field.setAccessible(true);
							field.set(object, value);
						} finally {
							field.setAccessible(oldAccessible);
						}
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
		
		return (T)object;
	}

	private Class<?>[] getConstuctorParamTypesWithIChatServices(Class<?>[] constuctorParamTypes) {
		Class<?>[] constuctorParamTypesWithIChatServices = new Class<?>[constuctorParamTypes.length + 1];
		constuctorParamTypesWithIChatServices[0] = IChatServices.class;
		
		for (int i = 0; i < constuctorParamTypes.length; i++) {
			constuctorParamTypesWithIChatServices[i + 1] = constuctorParamTypes[i];
		}
		
		return constuctorParamTypesWithIChatServices;
	}
	
	private Object[] getConstuctorParamsWithChatServices(Object[] constuctorParams, IChatServices chatServices) {
		Object[] constuctorParamsWithChatServices = new Object[constuctorParams.length + 1];
		constuctorParamsWithChatServices[0] = chatServices;
		
		for (int i = 0; i < constuctorParams.length; i++) {
			constuctorParamsWithChatServices[i + 1] = constuctorParams[i];
		}
		
		return constuctorParamsWithChatServices;
	}

	private void setChatServicesToApiObject(Class<?> apiImplType, Object object) {
		Method method = null;
		try {
			method = apiImplType.getDeclaredMethod("setChatServices", IChatServices.class);
		} catch (Exception e) {
			// ignore
		}
			
		if (method != null) {
			try {
				method.invoke(object, chatServices);
			} catch (Exception e) {
				throw new RuntimeException("Can't set chat services.", e);
			}
			
			return;
		}
		
		Field field = null;
		try {
			field = apiImplType.getDeclaredField("chatServices");
		} catch (Exception e1) {
			// ignore
		}
		
		if (field != null) {
			if (IChatServices.class.isAssignableFrom(field.getType())) {
				boolean oldAccessible = field.isAccessible();
				field.setAccessible(true);
				try {
					field.set(object, chatServices);
				} catch (Exception e) {
					throw new RuntimeException("Can't set chat services.", e);
				} finally {
					field.setAccessible(oldAccessible);
				}
			}
		}
	}

	private Method getWriter(String propertyName, Class<?> implClass, Class<?> type) {		
		String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
		
		try {
			return implClass.getDeclaredMethod(methodName, new Class<?>[] {type});
		} catch (Exception e) {
			if (type == Boolean.class) {
				type = boolean.class;
				return getWriter(propertyName, implClass, type);
			} else if (type == Integer.class) {
				type = int.class;
				return getWriter(propertyName, implClass, type);
			} else if (type == Long.class) {
				type = long.class;
				return getWriter(propertyName, implClass, type);
			} else if (type == Double.class) {
				type = double.class;
				return getWriter(propertyName, implClass, type);
			}
			
			return null;
		}
	}

	@Override
	public void close() {
		close(true);
	}
	
	protected synchronized void close(boolean graceful) {
		if (state == State.CLOSED)
			return;
		
		exception = null;
		chatServices.stop();
		
		if (isConnected()) {
			stream.close(graceful);			
		}
		
		if (stream == null && state == State.CONNECTING) {
			if (connection != null && !connection.isClosed())
				connection.close();
			
			connection = null;
		}
		
		stream = null;
		
		if (logger.isDebugEnabled())
			logger.debug("Chat client has disconnected from XMPP server({}).", String.format("%s: %d", streamConfig.getHost(), streamConfig.getPort()));
		
		state = State.CLOSED;
	}
	
	@Override
	public void before(IStreamNegotiant source) {
		for (INegotiationListener negotiationListener : negotiationListeners) {
			negotiationListener.before(source);
		}
	}
	
	@Override
	public void after(IStreamNegotiant source) {
		for (INegotiationListener negotiationListener : negotiationListeners) {
			negotiationListener.after(source);
		}
	}

	@Override
	public void occurred(NegotiationException exception) {
		for (INegotiationListener listener : negotiationListeners) {
			listener.occurred(exception);
		}
		
		this.exception = exception;
		synchronized(this) {
			notify();
		}
	}
	
	@Override
	public void done(IStream stream) {
		this.stream = stream;
		
		stream.setOxmFactory(oxmFactory);
		
		stream.addErrorListener(chatServices);
		stream.addStanzaListener(chatServices);
		
		chatServices.start();
		synchronized (this) {
			state = State.CONNECTED;
			notify();
		}
		
		if (logger.isDebugEnabled())
			logger.debug("Chat client has connected to XMPP server({}).", String.format("%s: %d", streamConfig.getHost(), streamConfig.getPort()));
		
		if (stream.getJid() != null)
			stream.getKeepAliveManager().start();
		
		for (INegotiationListener listener : negotiationListeners) {
			listener.done(stream);
		}
	}

	@Override
	public void exceptionOccurred(ConnectionException exception) {
		if (state == State.CONNECTED) {
			if (exception.getType() == ConnectionException.Type.CONNECTION_CLOSED ||
					exception.getType() == ConnectionException.Type.END_OF_STREAM) {
				synchronized (this) {
					close(false);
					
					notify();
				}
			}
			
			for (IConnectionListener connectionListener : connectionListeners) {
				connectionListener.exceptionOccurred(exception);
			}
		} else if (state == State.CONNECTING) {
			processNegotiationConnectionError(exception);
		} else { // state == State.CLOSED
			// ignore
		}
	}
	
	@Override
	public void heartBeatsReceived(int length) {
		for (IConnectionListener connectionListener : connectionListeners) {
			connectionListener.heartBeatsReceived(length);
		}
	}
	
	private synchronized void processNegotiationConnectionError(ConnectionException exception) {
		this.exception = exception;
		
		notify();
	}

	@Override
	public void messageReceived(String message) {
		for (IConnectionListener connectionListener : connectionListeners) {
			connectionListener.messageReceived(message);
		}
	}

	@Override
	public void messageSent(String message) {
		for (IConnectionListener connectionListener : connectionListeners) {
			connectionListener.messageSent(message);
		}
	}

	@Override
	public synchronized boolean isConnected() {
		return state == State.CONNECTED && stream != null && !stream.isClosed();
	}

	@Override
	public synchronized boolean isClosed() {
		return state == State.CLOSED || stream == null || stream.isClosed();
	}
	
	@Override
	public void addNegotiationListener(INegotiationListener negotiationListener) {
		if (!negotiationListeners.contains(negotiationListener))
			negotiationListeners.add(negotiationListener);
	}

	@Override
	public void removeNegotiationListener(INegotiationListener negotiationListener) {
		negotiationListeners.remove(negotiationListener);
	}
	
	@Override
	public void addConnectionListener(IConnectionListener connectionListener) {
		if (!connectionListeners.contains(connectionListener))
			connectionListeners.add(connectionListener);
	}
	
	@Override
	public boolean removeConnectionListener(IConnectionListener connectionListener) {
		return connectionListeners.remove(connectionListener);
	}
	
	@Override
	public IConnectionListener[] getConnectionListeners() {
		return connectionListeners.toArray(new IConnectionListener[connectionListeners.size()]);
	}
	
	@Override
	public List<INegotiationListener> getNegotiationListeners() {
		return negotiationListeners;
	}

	@Override
	public void setDefaultErrorHandler(IErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public IErrorHandler getDefaultErrorHandler() {
		return errorHandler;
	}
	
	public void setDefaultExceptionHandler(IExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	public IExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
	private class ChatSystem implements IChatSystem {
		@Override
		public void registerParser(ProtocolChain protocolChain, IParserFactory<?> parserFactory) {
			oxmFactory.register(protocolChain, parserFactory);
		}

		@Override
		public void unregisterParser(ProtocolChain protocolChain) {
			oxmFactory.unregister(protocolChain);
		}

		@Override
		public <T> void registerTranslator(Class<T> type, ITranslatorFactory<T> translatorFactory) {
			oxmFactory.register(type, translatorFactory);
		}

		@Override
		public void unregisterTranslator(Class<?> type) {
			oxmFactory.unregister(type);
		}
		
		@Override
		public void registerApi(Class<?> apiType) {
			registerApi(apiType, true);
		}

		@Override
		public <T> void registerApi(Class<T> apiType, Class<? extends T> apiImplType) {
			registerApi(apiType, apiImplType, true);
		}

		@Override
		public void unregisterApi(Class<?> apiType) {
			apis.remove(apiType);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void registerApi(Class<?> apiType, boolean singleton) {
			registerApi((Class)apiType, apiType, null, singleton);
		}

		@Override
		public <T> void registerApi(Class<T> apiType, Class<? extends T> apiImplType, boolean singleton) {
			registerApi(apiType, apiImplType, null, singleton);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void registerApi(Class<?> apiType, Properties properties) {
			registerApi((Class)apiType, apiType, properties, true);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void registerApi(Class<?> apiType, Properties properties, boolean singleton) {
			registerApi((Class)apiType, apiType, properties, singleton);
		}

		@Override
		public <T> void registerApi(Class<T> apiType, Class<? extends T> apiImplType, Properties properties) {
			registerApi(apiType, apiImplType, properties, true);
		}

		@Override
		public <T> void registerApi(Class<T> apiType, Class<? extends T> apiImplType,
				Properties properties, boolean singleton) {
			Api<T> api = new Api<T>(apiType, apiImplType, properties, singleton);
			apis.putIfAbsent(api.apiType, api);
		}

		@Override
		public void register(Class<? extends IPlugin> pluginClass) {
			AbstractChatClient.this.register(pluginClass);
		}

		@Override
		public void unregister(Class<? extends IPlugin> pluginClass) {
			AbstractChatClient.this.unregister(pluginClass);
		}

		@Override
		public StreamConfig getStreamConfig() {
			return AbstractChatClient.this.streamConfig;
		}
		
	}
	
	private class Api<T> {
		public Class<T> apiType;
		public Class<? extends T> implType;
		public Properties properties;
		public boolean singleton;
		public volatile Object singletonObject;
		
		public Api(Class<T> apiClass, Class<? extends T> implClass, Properties properties, boolean singleton) {
			this.apiType = apiClass;
			this.implType = implClass;
			this.properties = properties;
			if (this.properties == null) {
				this.properties = new Properties();
			}
			
			this.singleton = singleton;
		}
	}

	public ExecutorService createTaskThreadPool() {
		return Executors.newCachedThreadPool();
	}

	@Override
	public IStream getStream() {
		return stream;
	}
	
	@Override
	public IChatServices getChatServices() {
		return chatServices;
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
	
	private class OrderedList<T> implements List<T> {
		private volatile List<T> original;
		
		public OrderedList(List<T> original) {
			this.original = original;
		}

		@Override
		public boolean add(T e) {
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
	
	@Override
	public IOxmFactory getOxmFactory() {
		return oxmFactory;
	}
}
