package com.thefirstlineofcode.chalk.logger;

import java.net.URL;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogConfigurator {
	public static final String PROPERTY_KEY_DATA_DIR = "chalk.data.dir";
	public static final String PROPERTY_KEY_CHALK_APP_NAME = "chalk.app.name";
	public static final String APP_NAME_CHALK = "chalk_app";

	public enum LogLevel {
		INFO,
		DEBUG,
		TRACE
	}

	public void configure(String appName, LogLevel logLevel) {
		System.setProperty(PROPERTY_KEY_DATA_DIR, System.getProperty("user.home"));
		
		if (appName == null || appName.isEmpty()) {
			appName = APP_NAME_CHALK;
		}
		System.setProperty(PROPERTY_KEY_CHALK_APP_NAME, appName);
		
		configure(logLevel);
	}
	
	protected void configure(LogLevel logLevel) {
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
		
		if (logLevel != null) {			
			if (LogLevel.DEBUG.equals(logLevel)) {
				configureLog(lc, "logback_debug.xml");
			} else if (LogLevel.TRACE.equals(logLevel)) {
				configureLog(lc, "logback_trace.xml");
			} else {
				configureLog(lc, "logback.xml");
			}
		} else {
			configureLog(lc, "logback.xml");
		}
	}
	
	protected void configureLog(LoggerContext lc, String logFile) {
		configureLC(lc, getClass().getClassLoader().getResource(logFile));
	}
	
	protected void configureLC(LoggerContext lc, URL url) {
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			lc.reset();
			configurator.setContext(lc);
			configurator.doConfigure(url);
		} catch (JoranException e) {
			// Ignore. StatusPrinter will handle this.
		}
		
	    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}
}
