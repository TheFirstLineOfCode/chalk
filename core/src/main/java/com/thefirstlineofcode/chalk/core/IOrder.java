package com.thefirstlineofcode.chalk.core;

public interface IOrder {
	public static final int ORDER_MIN = 0;
	public static final int ORDER_NORMAL = 100;
	public static final int ORDER_MAX = 200;
	
	int getOrder();
}
