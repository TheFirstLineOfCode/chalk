package com.thefirstlineofcode.chalk.examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.chalk.examples.cluster.IbrClusterExample;
import com.thefirstlineofcode.chalk.examples.cluster.ImClusterExample;
import com.thefirstlineofcode.chalk.examples.cluster.PingClusterExample;
import com.thefirstlineofcode.chalk.examples.lite.IbrLiteExample;
import com.thefirstlineofcode.chalk.examples.lite.PingLiteExample;

public class Main {
	private enum DeployMode {
		LITE,
		CLUSTER
	}
	
	private static final String[] EXAMPLE_NAMES = new String[] {"ibr", "ping", "im"};
	private static final Class<?>[] CLUSTER_EXAMPLE_CLASS = new Class<?>[] {
		IbrClusterExample.class,
		PingClusterExample.class,
		ImClusterExample.class
	};
	private static final Class<?>[] LITE_EXAMPLE_CLASS = new Class<?>[] {
		IbrLiteExample.class,
		PingLiteExample.class
	};
	
	private DeployMode deployMode;
	
	public static void main(String[] args) throws Exception {
		System.setProperty("chalk.negotiation.read.response.timeout", Integer.toString(10 * 60 * 1000));
		
		new Main().run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options;
		try {
			options = parseOptions(args);
		} catch (IllegalArgumentException e) {
			printUsage();
			return;
		}
		
		deployMode = getDeployMode(options);
		
		if (options.examples == null) {
			options.examples = EXAMPLE_NAMES;
		}
		
		if (deployMode == DeployMode.LITE && options.examples.length != 1) {
			throw new IllegalArgumentException("Only one example can be executed each time in deploy lite mode.");
		}
		
		for (int i = 0; i < options.examples.length; i++) {
			Class<Example> exampleClass = getExampleClass(options.examples[i]);
			
			if (exampleClass == null)
				throw new RuntimeException(String.format("Unsupported example: %s. Supported example names are: %s.", options.examples[i], getExampleNames()));
			
			Example example = exampleClass.getDeclaredConstructor().newInstance();
			try {
				example.init(options);
				example.run();
			} catch (Exception e) {
				throw e;
			} finally {
				example.clean();
			}
		}
	}

	private DeployMode getDeployMode(Options options) {
		if ("cluster".equals(options.deployMode)) {
			return DeployMode.CLUSTER;
		}
		
		return DeployMode.LITE;
	}

	private String getExampleNames() {
		String exampleNames = null;
		for (String exampleName : EXAMPLE_NAMES) {
			if (exampleNames == null) {
				exampleNames = exampleName;
			} else {
				exampleNames = exampleNames + ", " + exampleName;
			}
		}
		
		return exampleNames;
	}

	@SuppressWarnings("unchecked")
	private Class<Example> getExampleClass(String exampleName) {
		Class<?>[] exampleClasses;
		if (deployMode == DeployMode.CLUSTER) {
			exampleClasses = CLUSTER_EXAMPLE_CLASS;
		} else {
			exampleClasses = LITE_EXAMPLE_CLASS;			
		}
		
		for (int i = 0; i < EXAMPLE_NAMES.length; i++) {
			if (EXAMPLE_NAMES[i].equals(exampleName))
				return (Class<Example>)exampleClasses[i];
		}
		
		return null;
	}

	private Options parseOptions(String[] args) throws IllegalArgumentException {
		Options options = new Options();
		
		Map<String, String> mArgs = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("--")) {
				String[] examples = Arrays.copyOfRange(args, i, args.length);
				options.examples = examples;
				break;
			}
			
			String argName = null;
			String argValue = null;
			int equalMarkIndex = args[i].indexOf('=');
			if (equalMarkIndex == -1) {
				argName = args[i].trim();
				argValue = "TRUE";
			} else {
				argName = args[i].substring(2, equalMarkIndex).trim();
				argValue = args[i].substring(equalMarkIndex + 1, args[i].length()).trim();
			}
			
			if (mArgs.containsKey(argName)) {
				throw new IllegalArgumentException();
			}
			
			mArgs.put(argName, argValue);
		}
		
		for (Map.Entry<String, String> entry : mArgs.entrySet()) {
			if ("host".equals(entry.getKey())) {
				options.host = entry.getValue();
			} else if ("port".equals(entry.getKey())) {
				options.port = Integer.parseInt(entry.getValue());
			} else if ("message-format".equals(entry.getKey())) {
				String messageFormat = entry.getValue();
				if (Constants.MESSAGE_FORMAT_XML.equals(messageFormat) || Constants.MESSAGE_FORMAT_BINARY.equals(messageFormat)) {
					options.messageFormat = messageFormat;
				} else {
					throw new IllegalArgumentException(String.format("Illegal message format: %s. only 'xml' or 'binary' supported.", entry.getKey()));
				}
			} else if ("deploy-mode".equals(entry.getKey())) {
				String deployMode = entry.getValue();
				if ("lite".equals(deployMode) || "cluster".equals(deployMode)) {
					options.deployMode = deployMode;
				} else {
					throw new IllegalArgumentException(String.format("Illegal deploy mode: %s. only 'lite' or 'cluster' supported.", entry.getKey()));
				}
			} else if ("db-host".equals(entry.getKey())) {
				options.dbHost = entry.getValue();
			} else if ("db-port".equals(entry.getKey())) {
				options.dbPort = Integer.parseInt(entry.getValue());
			} else if ("db-name".equals(entry.getKey())) {
				options.dbName = entry.getValue();
			} else if ("db-user".equals(entry.getKey())) {
				options.dbUser = entry.getValue();
			} else if ("db-password".equals(entry.getKey())) {
				options.dbPassword = entry.getValue();
			} else {
				throw new IllegalArgumentException(String.format("Unknown option %s.", entry.getKey()));
			}
		}
		
		return options;
	}
	
	private void printUsage() {
		System.out.println("--------Usage--------");
		System.out.println("java com.thefirstlineofcode.chalk.examples.Main [OPTIONS] <EXAMPLE_NAMES>");
		System.out.println("OPTIONS:");
		System.out.println("--host=[]           Server address(Default is 'localhost'). ");
		System.out.println("--port=[]           Server port(Default is '5222').");		
		System.out.println("--message-format=[] Chalk message format('xml' or 'binary'. Default is 'xml').");
		System.out.println("--deploy-mode=[]    Server deploy mode('lite' or 'cluster'. Default is 'lite').");
		System.out.println("--db-host=[]        Database host(Default is 'localhost').");
		System.out.println("--db-port=[]        Database port(Default is '27017' in cluster mode or '9001' in lite mode').");
		System.out.println("--db-name=[]        Database name(Default is 'granite').");
		System.out.println("--db-user=[]        Database user(Default is 'granite').");
		System.out.println("--db-password=[]    Database password(Default is 'granite').");
		System.out.println("EXAMPLE_NAMES:");
		System.out.println("ibr                 In-Band Registratio(XEP-0077).");
		System.out.println("ping                XMPP Ping(XEP-0077).");
		System.out.println("im                  XMPP IM(RFC3921).");
	}
}
