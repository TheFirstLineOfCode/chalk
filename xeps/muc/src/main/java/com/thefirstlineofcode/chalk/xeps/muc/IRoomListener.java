package com.thefirstlineofcode.chalk.xeps.muc;

import com.thefirstlineofcode.chalk.xeps.muc.events.RoomEvent;

public interface IRoomListener {
	void received(RoomEvent<?> event);
}
