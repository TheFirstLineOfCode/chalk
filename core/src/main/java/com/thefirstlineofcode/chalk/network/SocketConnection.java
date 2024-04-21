package com.thefirstlineofcode.chalk.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.binary.BinaryUtils;
import com.thefirstlineofcode.basalt.oxm.binary.BxmppConversionException;
import com.thefirstlineofcode.basalt.oxm.binary.IBinaryXmppProtocolConverter;
import com.thefirstlineofcode.basalt.oxm.binary.IBinaryXmppProtocolFactory;
import com.thefirstlineofcode.basalt.oxm.preprocessing.IMessagePreprocessor;
import com.thefirstlineofcode.basalt.oxm.preprocessing.OutOfMaxBufferSizeException;
import com.thefirstlineofcode.basalt.oxm.preprocessing.XmlMessagePreprocessorAdapter;
import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.chalk.core.stream.keepalive.IKeepAliveManager;

public class SocketConnection implements IConnection, HandshakeCompletedListener {
	private static final Logger logger = LoggerFactory.getLogger(SocketConnection.class);
	
	private static final int DEFAULT_READ_QUEUE_SIZE = 64;
	private static final int DEFAULT_WRITE_QUEUE_SIZE = 64;
	private static final int DEFAULT_BLOCKING_TIMEOUT = 2 * 1000;
	private static final int DEFAULT_CONNECT_TIMEOUT = 10 * 1000;
	
	private Socket socket;
	private BlockingQueue<byte[]> sendingQueue;
	private BlockingQueue<byte[]> receivingQueue;
	
	private Thread receivingThread;
	private Thread sendingThread;
	private Thread processingThread;
	
	private List<IConnectionListener> listeners;
	
	private HandshakeCompletedEvent handshakeCompletedEvent;
	
	private volatile boolean stopThreadsFlag;
	private int blockingTimeout;
	
	private boolean useBinaryFormat = false;
	private IBinaryXmppProtocolFactory bxmppProtocolFactory;
	private IBinaryXmppProtocolConverter bxmppProtocolConverter;
	private IMessagePreprocessor messagePreprocessor;
	
	private Charset charset;
	private CharsetDecoder decoder;
	
	public SocketConnection() {
		this(Constants.MESSAGE_FORMAT_XML);
	}
	
	public SocketConnection(String messageFormat) {
		this(messageFormat, null);
	}
	
	public SocketConnection(String messageFormat, Socket socket) {
		this(messageFormat, socket, DEFAULT_READ_QUEUE_SIZE, DEFAULT_WRITE_QUEUE_SIZE);
	}
	
	public SocketConnection(String messageFormat, Socket socket, int readQueueSize, int writeQueueSize) {
		this(messageFormat, socket, readQueueSize, writeQueueSize, DEFAULT_BLOCKING_TIMEOUT);
	}
	
	public SocketConnection(String messageFormat, Socket socket, int readQueueSize, int writeQueueSize, int blockingTimeout) {
		this.socket = socket;
		stopThreadsFlag = false;
		this.blockingTimeout = blockingTimeout;
		
		sendingQueue = new ArrayBlockingQueue<>(writeQueueSize);
		receivingQueue = new ArrayBlockingQueue<>(readQueueSize);
		listeners = new CopyOnWriteArrayList<>();
		
		if (Constants.MESSAGE_FORMAT_BINARY.equals(messageFormat)) {
			useBinaryFormat = true;
			try {
				bxmppProtocolFactory = createBxmppProtocolConverterFactory();
			} catch (Exception e) {
				logger.warn("Can't create BXMPP protocol converter. Please add gem BXMPP libraries to your classpath. Ignore to configure message format to binary. Still use XML message format.");
				useBinaryFormat = false;
			}
			
			if (bxmppProtocolFactory != null) {
				bxmppProtocolConverter = bxmppProtocolFactory.createConverter();
				messagePreprocessor = bxmppProtocolFactory.createPreprocessor();
				return;
			}
		}
		
		if (!Constants.MESSAGE_FORMAT_XML.equals(messageFormat)) {
			logger.warn("Illegal message format: {}. It will be ignored. Still use XML message format.", messageFormat);
		}
		
		messagePreprocessor = new XmlMessagePreprocessorAdapter();
		charset = Charset.forName(Constants.DEFAULT_CHARSET);
		decoder = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
	}
	
	private IBinaryXmppProtocolFactory createBxmppProtocolConverterFactory() throws ClassNotFoundException, InstantiationException,
				IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?> factoryClass = Class.forName("com.thefirstlineofcode.gem.client.bxmpp.BinaryXmppProtocolFactory");
		return (IBinaryXmppProtocolFactory)factoryClass.getDeclaredConstructor().newInstance();
	}
	
	@Override
	public void connect(String host, int port) throws ConnectionException {
		connect(host, port, DEFAULT_CONNECT_TIMEOUT);
	}
	
	@Override
	public void connect(String host, int port, int timeout) throws ConnectionException {
		try {
			InetSocketAddress address = new InetSocketAddress(host, port);
			if (address.isUnresolved()) {
				throw new ConnectionException(ConnectionException.Type.ADDRESS_IS_UNRESOLVED);
			}
			
			if (socket == null) {
				socket = createSocket();
			}
			
			socket.connect(address, timeout);
		} catch (IOException e) {
			throw new ConnectionException(ConnectionException.Type.IO_ERROR, e);
		}
			
		startThreads();
	}

	private void startThreads() {
		stopThreadsFlag = false;
		
		sendingThread = new SendingThread();
		sendingThread.start();
		
		receivingThread = new ReceivingThread();
		receivingThread.start();
		
		processingThread = new ProcessingThread();
		processingThread.start();
	}

	protected Socket createSocket() throws IOException {
		Socket socket = new Socket();
		socket.setSoTimeout(blockingTimeout);
		socket.setTcpNoDelay(true);
		
		return socket;
	}

	private void processException(ConnectionException exception) {
		for (IConnectionListener listener : listeners) {
			listener.exceptionOccurred(exception);
		}
		
		// Method processException() is called by SendingThread and ReceivingThread.
		// So we need to run close() method in another thread to avoid dead lock.
		new Thread(new Runnable() {
			@Override
			public void run() {
				close();
			}
		}).start();
	}
	
	@Override
	public void close() {
		stopThreads();
		
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
			} catch (Exception e) {
				// Ignore all exceptions.
			}
		}
		socket = null;
		
		sendingQueue.clear();
		receivingQueue.clear();
		listeners.clear();
	}

	private void stopThreads() {
		stopThreadsFlag = true;
		
		if (processingThread != null) {
			try {
				processingThread.join(blockingTimeout * 2, 0);
			} catch (InterruptedException e) {
				// ignore
			}
			processingThread = null;
		}
		
		if (sendingThread != null) {
			try {
				sendingThread.join(blockingTimeout * 2, 0);
			} catch (InterruptedException e) {
				// ignore
			}
			sendingThread = null;
		}
		
		if (receivingThread != null) {
			try {
				receivingThread.join(blockingTimeout * 2, 0);
			} catch (InterruptedException e) {
				// ignore
			}
			receivingThread = null;
		}
	}
	
	@Override
	public void write(byte[] bytes) {
		if (stopThreadsFlag || isClosed()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Can't write message. stopThreadsFlag is {}. isClosed() is {}.",
						stopThreadsFlag, isClosed());
			}
			
			return;
		}
		
		try {
			sendingQueue.put(bytes);
		} catch (InterruptedException e) {
			// Ignore, connection is closed
		}
	}
	
	@Override
	public void write(String message) {
		if (useBinaryFormat) {
			try {
				write(bxmppProtocolConverter.toBinary(message));
			} catch (BxmppConversionException e) {
				throw new RuntimeException("Failed to convert message to BXMPP data.", e);
			}
		} else {
			write(message.getBytes());
		}
	}

	@Override
	public void addListener(IConnectionListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public boolean removeListener(IConnectionListener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	public IConnectionListener[] getListeners() {
		if (listeners.size() == 0)
			return new IConnectionListener[0];
		
		return listeners.toArray(new IConnectionListener[listeners.size()]);
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	private class ProcessingThread extends Thread {
		public ProcessingThread() {
			super("Socket Connection Processing Thread");
		}
		
		public void run() {
			while (true) {
				try {
					byte[] bytes = null;
					bytes = receivingQueue.poll(blockingTimeout, TimeUnit.MILLISECONDS);
					
					if (stopThreadsFlag) {
						break;
					}
					
					if (bytes == null)
						continue;
					
					String[] messages = processMessages(bytes);
					traceMessageReceived(bytes);
					
					if (messages.length != 0) {
						for (String message : messages) {
							if (isHeartBeats(message)) {
								if (logger.isTraceEnabled())
									logger.trace("{} heatbeats has received.", message.length());
								
								for (IConnectionListener listener : listeners) {
									listener.heartBeatsReceived(message.length());
								}
							} else {
								if (logger.isTraceEnabled())
									logger.trace("Message has received: {}", message);
							
								for (IConnectionListener listener : listeners) {
									listener.messageReceived(message);
								}
							}
						}
					}
				} catch (InterruptedException e) {
					break;
				}
				
			}
		}

		private void traceMessageReceived(byte[] bytes) {
			if (!logger.isTraceEnabled())
				return;
			
			if (useBinaryFormat) {
				logger.trace("{} binary message bytes has received: {}.",
						bytes.length, BinaryUtils.getHexStringFromBytes(bytes));
			} else {
				logger.trace("A XMPP string has received: {}.", new String(bytes));								
			}
		}

		private String[] processMessages(byte[] bytes) {
			String[] messages;
			try {
				messages = messagePreprocessor.process(bytes);
			} catch (ProtocolException e) {
				messages = new String[0];
				messagePreprocessor.clear();
				processException(new ConnectionException(ConnectionException.Type.BAD_PROTOCOL_MESSAGE, e));
			} catch (OutOfMaxBufferSizeException e) {
				messages = new String[0];
				messagePreprocessor.clear();
				processException(new ConnectionException(ConnectionException.Type.OUT_OF_BUFFER, e));
			}
			
			return messages;
		}
	}
	
	private boolean isHeartBeats(String message) {
		for (char c : message.toCharArray()) {
			if (!(IKeepAliveManager.CHAR_HEART_BEAT == c))
				return false;
		}
		
		return true;
	}
	
	private class ReceivingThread extends Thread {
		private static final int DEFAULT_BUFFER_SIZE = 1024 * 16;
		private InputStream input;
		
		public ReceivingThread() {
			super("Socket Connection Receiving Thread");
		}
		
		@Override
		public void run() {
			try {
				input = new BufferedInputStream(socket.getInputStream());
			} catch (IOException e) {
				processException(new ConnectionException(ConnectionException.Type.IO_ERROR, e));
				return;
			}
			
			byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
			int length = 0;
			while (true) {
				try {
					if (stopThreadsFlag) {
						break;
					}
					
					int num = input.read(buf, length, DEFAULT_BUFFER_SIZE - length);	
					if (num == -1) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							throw new RuntimeException("Unexpected exception.", e);
						}
					} else {
						length += num;
						
						if (!useBinaryFormat) {
							decoder.reset();
							CharBuffer charBuffer = CharBuffer.allocate(length);
							CoderResult coderResult = decoder.decode(ByteBuffer.wrap(buf, 0, length),
									charBuffer, false);
							
							if (coderResult.isUnmappable() || coderResult.isMalformed()) {
								try {
									coderResult.throwException();									
								} catch (Exception e) {
									if (logger.isErrorEnabled())
										logger.error("Can't decode the message.", e);
									
									throw e;
								}
							} else if (coderResult.isUnderflow()) {
								receivedMessage(buf, length);
								length = 0;
							} else {
								// coderResult.isOverflow()
								try {
									coderResult.throwException();
								} catch (Exception e) {									
									throw new RuntimeException("Char buffer size is too short???", e);
								}
							}
						} else {
							receivedMessage(buf, length);
							length = 0;
						}
					}
				} catch (SocketTimeoutException e) {
					if (stopThreadsFlag) {
						break;
					}
				} catch (IOException e) {
					processException(new ConnectionException(ConnectionException.Type.IO_ERROR, e));
					break;
				}
			}
		}

		private void receivedMessage(byte[] buf, int length) {
			receivingQueue.add(Arrays.copyOf(buf, length));
			Arrays.fill(buf, (byte)0);
		}
		
	}
	
	private class SendingThread extends Thread {
		private OutputStream output;
		
		public SendingThread() {
			super("Socket Connection Sending Thread");
		}
		
		@Override
		public void run() {
			try {
				output = socket.getOutputStream();
			} catch (IOException e) {
				processException(new ConnectionException(ConnectionException.Type.IO_ERROR, e));
				return;
			}
			
			byte[] bytes = null;
			while (true) {
				try {
					bytes = sendingQueue.poll(blockingTimeout, TimeUnit.MILLISECONDS);
					
					if (stopThreadsFlag) {
						break;
					}
					
					if (bytes == null)
						continue;
					
					output.write(bytes);
					output.flush();
					
					if (isHeartBeat(bytes)) {
						if (logger.isTraceEnabled()) {
							logger.trace(String.format("A heartbeat has sent."));
						}
					} else {
						String message = traceMessageSent(bytes);
						
						for (IConnectionListener listener : listeners) {
							listener.messageSent(message);
						}
					}
				} catch (InterruptedException e) {
					break;
				} catch (IOException e) {
					processException(new ConnectionException(ConnectionException.Type.IO_ERROR, e));
					break;
				} catch (BxmppConversionException e) {
					throw new RuntimeException("????Can't restore BXMPP data to XML.");
				}
			}
		}

		private String traceMessageSent(byte[] bytes) throws BxmppConversionException {
			String message = null;
			if (useBinaryFormat) {
				message = bxmppProtocolConverter.toXml(bytes);
				if (logger.isTraceEnabled()) {
					logger.trace("{} binary message bytes has sent: {}.",
							bytes.length, BinaryUtils.getHexStringFromBytes(bytes));
				}
			} else {
				message = new String(bytes);
				if (logger.isTraceEnabled()) {					
					logger.trace("A XMPP string has sent: {}.", message);								
				}
			}
			
			return message;
		}
	}
	
	private boolean isHeartBeat(byte[] bytes) {
		if (useBinaryFormat) {
			for (byte b : bytes) {
				if (!isHeartBeatByte(b))
					return false;
			}
		} else {
			String message = new String(bytes);
			for (int i = 0; i < message.length(); i++) {
				if (!isHeartBeartChar(message.charAt(i)))
					return false;
			}
		}
		
		return true;
	}

	private boolean isHeartBeatByte(byte b) {
		return IKeepAliveManager.BYTE_HEART_BEAT == b;
	}

	private boolean isHeartBeartChar(char c) {
		return IKeepAliveManager.CHAR_HEART_BEAT == c;
	}

	@Override
	public boolean isTlsSupported() {
		return true;
	}

	@Override
	public boolean isTlsStarted() {
		return handshakeCompletedEvent != null;
	}

	@Override
	public Certificate[] startTls() throws ConnectionException {
		stopThreads();
		
		try {
			SSLSocket sslSocket = getSslSocket();
			
			sslSocket.setSoTimeout(0);
			sslSocket.addHandshakeCompletedListener(this);
			sslSocket.startHandshake();
			
			sslSocket.setSoTimeout(blockingTimeout);
			
			socket = sslSocket;
		} catch (IOException e) {
			throw new ConnectionException(ConnectionException.Type.IO_ERROR, e);
		} catch (KeyManagementException e) {
			throw new ConnectionException(ConnectionException.Type.TLS_FAILURE, e);
		} catch (NoSuchAlgorithmException e) {
			throw new ConnectionException(ConnectionException.Type.TLS_FAILURE, e);
		}
		
		try {
			synchronized (this) {
				if (handshakeCompletedEvent == null)
					this.wait();
			}
		} catch (InterruptedException e) {
			// ignore
		}
		
		if (handshakeCompletedEvent == null) {
			throw new RuntimeException("handshakeCompletedEvent is null???");
		}
		
		startThreads();
		
		try {
			return handshakeCompletedEvent.getPeerCertificates();
		} catch (SSLPeerUnverifiedException e) {
			throw new RuntimeException("Can't get peer certificates.", e);
		}
	}

	private SSLSocket getSslSocket() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("TLSv1.2");
		} catch (NoSuchAlgorithmException e) {
			throw e;
		}
		
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {

					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
							throws CertificateException {
						
					}

					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
							throws CertificateException {
					}

					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				}
		};
		
		try {
			sslContext.init(null, trustAllCerts, new SecureRandom());
		} catch (KeyManagementException e) {
			throw e;
		}
		
		SSLSocket sslSocket = (SSLSocket)sslContext.getSocketFactory().createSocket(socket,
				socket.getInetAddress().getHostName(), socket.getPort(), true);
		sslSocket.setUseClientMode(true);
		return sslSocket;
	}

	@Override
	public void handshakeCompleted(HandshakeCompletedEvent event) {
		synchronized (this) {
			handshakeCompletedEvent = event;
			this.notify();
		}
	}

	@Override
	public boolean isClosed() {
		if (socket == null)
			return true;
		
		return socket.isClosed();
	}
	
	@Override
	public boolean isConnected() {
		if (socket == null)
			return false;
		
		return socket.isConnected();
	}

}
