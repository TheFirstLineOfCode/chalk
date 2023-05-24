package com.thefirstlineofcode.chalk.im.subscription;

public class SubscriptionError {
	public enum Reason {
		ROSTER_SET_ERROR,
		ROSTER_SET_TIMEOUT
	}
	
	private Reason reason;
	private Object detail;
	
	public SubscriptionError(Reason reason) {
		this(reason, null);
	}
	
	public SubscriptionError(Reason reason, Object detail) {
		this.reason = reason;
		this.detail = detail;
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}

	public Object getDetail() {
		return detail;
	}

	public void setDetail(Object detail) {
		this.detail = detail;
	}
	
}
