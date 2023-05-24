package com.thefirstlineofcode.chalk.demo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationField;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationForm;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.logger.LogConfigurator;
import com.thefirstlineofcode.chalk.logger.LogConfigurator.LogLevel;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistration;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistrationCallback;
import com.thefirstlineofcode.chalk.xeps.ibr.IbrPlugin;
import com.thefirstlineofcode.chalk.xeps.ibr.RegistrationException;

public class Main {
	private static final String APP_NAME_CHALK_DEMO = "chalk-demo";

	public static void main(String[] args) throws RegistrationException {
		new Main().run(args);
	}
	
	private class Config {
		public String host = "localhost";
		public int port = 5222;
		public String messageFormat = "xml";
		public LogLevel logLevel = LogLevel.INFO;
	}
	
	private void run(String[] args) throws RegistrationException {
		Config config;
		try {
			config = getConfig(args);
		} catch (IllegalArgumentException e) {
			printUsage();
			return;
		}
		
		new LogConfigurator().configure(APP_NAME_CHALK_DEMO, config.logLevel);
		
		createAccounts(config);
		
		System.setProperty("chalk.stream.config.host", config.host);
		System.setProperty("chalk.stream.config.port", Integer.toString(config.port));
		System.setProperty("chalk.stream.config.message.format", config.messageFormat.toString().toLowerCase());
		
		new com.thefirstlineofcode.chalk.demo.Demo().run();
	}
	
	private void createAccounts(Config config) throws RegistrationException {
		StandardStreamConfig streamConfig = new StandardStreamConfig(config.host, config.port);
		streamConfig.setTlsPreferred(false);
		if (config.messageFormat != null)
			streamConfig.setProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT, config.messageFormat);
		IChatClient chatClient = new StandardChatClient(streamConfig);
		chatClient.register(IbrPlugin.class);
		
		String[][] accounts = new String[][] {
			{"dongger", "a_stupid_man"},
			{"agilest", "a_good_guy"},
			{"smartsheep", "a_pretty_girl"},
			{"jelly", "another_pretty_girl"}
		};
		
		for (String[] account : accounts) {
			createAccount(chatClient, account);
		}
	}

	private void createAccount(IChatClient chatClient, final String[] account) throws RegistrationException {
		IRegistration registration = chatClient.createApi(IRegistration.class);
		registration.addConnectionListener(new ConnectionListenerAdapter() {			
			@Override
			public void messageSent(String message) {
				printOut(message);
			}
			
			@Override
			public void messageReceived(String message) {
				printIn(message);
			}
			
			@Override
			public void exceptionOccurred(ConnectionException exception) {
				OutputStream os = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(os);
				exception.printStackTrace(ps);
				print(String.format("Connection error: %s, %s.", exception.getType(), os.toString()));
			}
		});
		
		registration.register(new RegistrationCallback(account[0], account[1]));
	}
	
	private void print(String message) {
		System.out.println(String.format("\t%s: %s.", "Main", message));
	}
	
	private void printOut(String message) {
		System.out.println(String.format("->%s: %s.", "Main", message));
	}
	
	private void printIn(String message) {
		System.out.println(String.format("<-%s: %s.", "Main", message));
	}
	
	private class RegistrationCallback implements IRegistrationCallback {
		private String name;
		private String password;
		
		public RegistrationCallback(String name, String password) {
			this.name = name;
			this.password = password;
		}

		@Override
		public Object fillOut(IqRegister iqRegister) {
			if (iqRegister.getRegister() instanceof RegistrationForm) {
				RegistrationForm form = new RegistrationForm();
				form.getFields().add(new RegistrationField("username", name));
				form.getFields().add(new RegistrationField("password", password));
				
				return form;
			} else {
				throw new RuntimeException("Can't get registration form.");
			}
		}
		
	}

	private Config getConfig(String[] args) throws IllegalArgumentException {
		Config config = new Config();
		
		Map<String, String> mArgs = new HashMap<>();
		for (String arg : args) {
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException();
			}
			
			int equalMarkIndex = arg.indexOf('=');
			if (equalMarkIndex == -1) {
				throw new IllegalArgumentException();
			}
			
			String argName = arg.substring(2, equalMarkIndex);
			String argValue = arg.substring(equalMarkIndex + 1, arg.length());
			
			if (argName == null || argValue == null) {
				throw new IllegalArgumentException();
			}
			
			if (mArgs.containsKey(argName)) {
				throw new IllegalArgumentException();
			}
			
			mArgs.put(argName, argValue);
		}
		
		for (Map.Entry<String, String> entry : mArgs.entrySet()) {
			if ("host".equals(entry.getKey())) {
				config.host = entry.getValue();
			} else if ("port".equals(entry.getKey())) {
				config.port = Integer.parseInt(entry.getValue());
			} else if ("message-format".equals(entry.getKey())) {
				String value = entry.getValue();
				if ("xml".equals(value) || "binary".equals(value)) {
					config.messageFormat = value;
				} else {
					throw new IllegalArgumentException(String.format("Invalid message format: %s.", value));
				}
			} else if ("log-level".equals(entry.getKey())) {
				String value = entry.getValue();
				if ("info".equals(value)) {
					config.logLevel = LogLevel.INFO;
				} else if ("debug".equals(value)) {
					config.logLevel = LogLevel.DEBUG;
				} else if ("trace".equals(value)) {
					config.logLevel = LogLevel.TRACE;					
				} else {
					throw new IllegalArgumentException(String.format("Invalid log level: %s.", value));
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
		
		return config;
	}

	private void printUsage() {
		System.out.println("java com.thefirstlineofcode.chalk.demo.Main [OPTIONS]");
		System.out.println("OPTIONS:");
		System.out.println("--host=<SERVER_ADDRESS>\t\tServer address");
		System.out.println("--port=<SERVER PORT>\t\tServer port");
		System.out.println("--message-format=<MESSAGE_FORMAT>\t\tMessage format(xml or binary)");
		System.out.println("--log-level=<LOG_LEVEL>\t\tLog level(info, debug or trace)");
	}
}
