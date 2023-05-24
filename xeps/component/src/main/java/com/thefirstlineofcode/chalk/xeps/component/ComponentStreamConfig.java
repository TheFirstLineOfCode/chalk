package com.thefirstlineofcode.chalk.xeps.component;

import com.thefirstlineofcode.chalk.core.stream.StreamConfig;

public class ComponentStreamConfig extends StreamConfig {
	private String componentName;

	public ComponentStreamConfig(String host, int port, String componentName) {
		super(host, port);
		
		this.componentName = componentName;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

}
