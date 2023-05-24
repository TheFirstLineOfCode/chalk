package com.thefirstlineofcode.chalk.im.roster;


public class RosterError {
	public enum Reason {
		ROSTER_RETRIEVE_ERROR,
		ROSTER_RETRIEVE_TIMEOUT,
		ROSTER_ADD_ERROR,
		ROSTER_ADD_TIMEOUT,
		ROSTER_UPDATE_ERROR,
		ROSTER_UPDATE_TIMEOUT,
		ROSTER_DELETE_ERROR,
		ROSTER_DELETE_TIMEOUT
	}
	
	private Reason reason;
	private Object detail;
	
	public RosterError(Reason reason) {
		this(reason, null);
	}
	
	public RosterError(Reason reason, Object detail) {
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
