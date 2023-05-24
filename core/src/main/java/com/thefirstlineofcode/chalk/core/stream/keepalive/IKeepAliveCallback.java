package com.thefirstlineofcode.chalk.core.stream.keepalive;

import java.util.Date;

import com.thefirstlineofcode.chalk.core.stream.IStream;

public interface IKeepAliveCallback {
	void received(Date time, boolean isHeartbeats);
	void sent(Date time, boolean isHeartbeats);
	void timeout(IStream stream);
}
