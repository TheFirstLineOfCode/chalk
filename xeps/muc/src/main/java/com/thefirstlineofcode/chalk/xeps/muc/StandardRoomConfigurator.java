package com.thefirstlineofcode.chalk.xeps.muc;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.xeps.muc.GetMemberList;
import com.thefirstlineofcode.basalt.xeps.muc.PresenceBroadcast;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig.AllowPm;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig.WhoIs;
import com.thefirstlineofcode.basalt.xeps.xdata.Field;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public abstract class StandardRoomConfigurator implements IRoomConfigurator {

	@Override
	public XData configure(XData form) {
		if (!isStandardRoomConfig(form)) {
			throw new IllegalArgumentException(String.format("%s only can configure standard muc configuration form.", getClass().getName()));
		}
		
		RoomConfig roomConfig = configure(convertConfigFormToRoomConfig(form));
		return convertRoomConfigToConfigForm(roomConfig);
	}
	
	private XData convertRoomConfigToConfigForm(RoomConfig roomConfig) {
		XData xData = new XData(XData.Type.SUBMIT);
		
		xData.getFields().add(createFormTypeField());
		addToFormIfNotNull(xData, createRoomNameField(roomConfig));
		addToFormIfNotNull(xData, createRoomDescField(roomConfig));
		addToFormIfNotNull(xData, createLangField(roomConfig));
		addToFormIfNotNull(xData, createEnalbleLoggingField(roomConfig));
		addToFormIfNotNull(xData, createChangeSubjectField(roomConfig));
		addToFormIfNotNull(xData, createAllowInvitesField(roomConfig));
		addToFormIfNotNull(xData, createAllowPmField(roomConfig));
		addToFormIfNotNull(xData, createMaxUsersField(roomConfig));
		addToFormIfNotNull(xData, createPresenceBroadcastField(roomConfig));
		addToFormIfNotNull(xData, createGetMemberListField(roomConfig));
		addToFormIfNotNull(xData, createPublicRoomField(roomConfig));
		addToFormIfNotNull(xData, createPersistentRoomField(roomConfig));
		addToFormIfNotNull(xData, createModeratedRoomField(roomConfig));
		addToFormIfNotNull(xData, createMemeberOnlyField(roomConfig));
		addToFormIfNotNull(xData, createPasswordProtectedRoomField(roomConfig));
		addToFormIfNotNull(xData, createRoomSecretField(roomConfig));
		addToFormIfNotNull(xData, createWhoIsField(roomConfig));
		addToFormIfNotNull(xData, createMaxHistoryFetchField(roomConfig));
		addToFormIfNotNull(xData, createRoomAdminsField(roomConfig));
		addToFormIfNotNull(xData, createRoomOwnersField(roomConfig));
		
		return xData;
	}
	
	private void addToFormIfNotNull(XData xData, Field field) {
		if (field == null || field.getValues().isEmpty())
			return;
		
		xData.getFields().add(field);
	}

	private Field createRoomOwnersField(RoomConfig roomConfig) {
		if (roomConfig.getOwners().isEmpty())
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_roomowners");
		
		for (JabberId jid : roomConfig.getOwners()) {
			field.getValues().add(jid.toString());
		}
		
		return field;
	}

	private Field createRoomAdminsField(RoomConfig roomConfig) {
		if (roomConfig.getAdmins().isEmpty())
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_roomadmins");
		
		for (JabberId jid : roomConfig.getAdmins()) {
			field.getValues().add(jid.toString());
		}
		
		return field;
	}

	private Field createMaxHistoryFetchField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#maxhistoryfetch");
		field.getValues().add(Integer.toString(roomConfig.getMaxHistoryFetch()));
		
		return field;
	}

	private Field createWhoIsField(RoomConfig roomConfig) {
		if (roomConfig.getWhoIs() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_whois");		
		field.getValues().add(roomConfig.getWhoIs().toString().toLowerCase());
		
		return field;
	}

	private Field createRoomSecretField(RoomConfig roomConfig) {
		if (roomConfig.getRoomSecret() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_roomsecret");
		field.getValues().add(roomConfig.getRoomSecret());
		
		return field;
	}

	private Field createPasswordProtectedRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_passwordprotectedroom");
		field.getValues().add(roomConfig.isPasswordProtectedRoom() ? "1" : "0");
		
		return field;
	}

	private Field createMemeberOnlyField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_membersonly");
		field.getValues().add(roomConfig.isMembersOnly() ? "1" : "0");
		
		return field;
	}

	private Field createModeratedRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_moderatedroom");
		field.getValues().add(roomConfig.isModeratedRoom() ? "1" : "0");
		
		return field;
	}

	private Field createPersistentRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_persistentroom");
		field.getValues().add(roomConfig.isPersistentRoom() ? "1" : "0");
		
		return field;
	}

	private Field createPublicRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_publicroom");
		field.getValues().add(roomConfig.isPublicRoom() ? "1" : "0");
		
		return field;
	}

	private Field createGetMemberListField(RoomConfig roomConfig) {
		if (roomConfig.getGetMemberList() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_getmemberlist");
		
		GetMemberList getMemberList = roomConfig.getGetMemberList();
		if (getMemberList.isModerator()) {
			field.getValues().add("moderator");
		}
		if (getMemberList.isParticipant()) {
			field.getValues().add("participant");
		}
		if (getMemberList.isVisitor()) {
			field.getValues().add("visitor");
		}
		
		return field;
	}

	private Field createPresenceBroadcastField(RoomConfig roomConfig) {
		if (roomConfig.getPresenceBroadcast() == null) {
			return null;
		}

		Field field = new Field();
		field.setVar("muc#roomconfig_presencebroadcast");
		
		PresenceBroadcast presenceBroadcast = roomConfig.getPresenceBroadcast();
		if (presenceBroadcast.isModerator()) {
			field.getValues().add("moderator");
		}
		if (presenceBroadcast.isParticipant()) {
			field.getValues().add("participant");
		}
		if (presenceBroadcast.isVisitor()) {
			field.getValues().add("visitor");
		}
		
		return field;
	}

	private Field createMaxUsersField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_maxusers");
		field.getValues().add(Integer.toString(roomConfig.getMaxUsers()));
		
		return field;
	}

	private Field createAllowPmField(RoomConfig roomConfig) {
		if (roomConfig.getAllowPm() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_allowpm");
		field.getValues().add(roomConfig.getAllowPm().toString().toLowerCase());
		
		return field;
	}

	private Field createAllowInvitesField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_allowinvites");
		field.getValues().add(roomConfig.isAllowInvites() ? "1" : "0");
		
		return field;
	}

	private Field createChangeSubjectField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_changesubject");
		field.getValues().add(roomConfig.isChangeSubject() ? "1" : "0");
		
		return field;
	}

	private Field createEnalbleLoggingField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setVar("muc#roomconfig_enablelogging");
		field.getValues().add(roomConfig.isEnableLogging() ? "1" : "0");
		
		return field;
	}

	private Field createLangField(RoomConfig roomConfig) {
		if (roomConfig.getLang() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_lang");
		field.getValues().add(roomConfig.getLang());
		
		return field;
	}

	private Field createRoomDescField(RoomConfig roomConfig) {
		if (roomConfig.getRoomDesc() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_roomdesc");
		field.getValues().add(roomConfig.getRoomDesc());
		
		return field;
	}

	private Field createRoomNameField(RoomConfig roomConfig) {
		if (roomConfig.getRoomName() == null)
			return null;
		
		Field field = new Field();
		field.setVar("muc#roomconfig_roomname");
		field.getValues().add(roomConfig.getRoomName());
		
		return field;
	}

	private Field createFormTypeField() {
		Field field = new Field();
		field.setVar("FORM_TYPE");
		field.getValues().add("http://jabber.org/protocol/muc#roomconfig");
		
		return field;
	}

	private RoomConfig convertConfigFormToRoomConfig(XData form) {
		RoomConfig roomConfig = new RoomConfig();
		for (Field field : form.getFields()) {
			if ("muc#roomconfig_roomname".equals(field.getVar())) {
				roomConfig.setRoomName(getFieldValue(field));
			} else if ("muc#roomconfig_roomdesc".equals(field.getVar())) {
				roomConfig.setRoomDesc(getFieldValue(field));
			} else if ("muc#roomconfig_lang".equals(field.getVar())) {
				roomConfig.setLang(getFieldValue(field));
			} else if ("muc#roomconfig_enablelogging".equals(field.getVar())) {
				roomConfig.setEnableLogging(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_changesubject".equals(field.getVar())) {
				roomConfig.setChangeSubject(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_allowinvites".equals(field.getVar())) {
				roomConfig.setAllowInvites(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_allowpm".equals(field.getVar())) {
				roomConfig.setAllowPm(AllowPm.valueOf(getFieldValue(field).toUpperCase()));
			} else if ("muc#roomconfig_maxusers".equals(field.getVar())) {
				roomConfig.setMaxUsers(getFieldIntValue(field));
			} else if ("muc#roomconfig_presencebroadcast".equals(field.getVar())) {
				roomConfig.setPresenceBroadcast(getPresenceBroadcast(field));
			} else if ("muc#roomconfig_getmemberlist".equals(field.getVar())) {
				roomConfig.setGetMemberList(getGetMemberList(field));
			} else if ("muc#roomconfig_publicroom".equals(field.getVar())) {
				roomConfig.setPublicRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_persistentroom".equals(field.getVar())) {
				roomConfig.setPersistentRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_moderatedroom".equals(field.getVar())) {
				roomConfig.setModeratedRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_membersonly".equals(field.getVar())) {
				roomConfig.setMembersOnly(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_passwordprotectedroom".equals(field.getVar())) {
				roomConfig.setPasswordProtectedRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_roomsecret".equals(field.getVar())) {
				roomConfig.setRoomSecret(getFieldValue(field));
			} else if ("muc#roomconfig_whois".equals(field.getVar())) {
				roomConfig.setWhoIs(WhoIs.valueOf(getFieldValue(field).toUpperCase()));
			} else if ("muc#maxhistoryfetch".equals(field.getVar())) {
				roomConfig.setMaxHistoryFetch(getFieldIntValue(field));
			} else if ("muc#roomconfig_roomadmins".equals(field.getVar())) {
				roomConfig.setAdmins(getFieldJidsValue(field));
			} else if ("muc#roomconfig_roomowners".equals(field.getVar())) {
				roomConfig.setOwners(getFieldJidsValue(field));
			} else {
				// ignore
				continue;
			}
		}
		
		return roomConfig;
	}

	private List<JabberId> getFieldJidsValue(Field field) {
		List<JabberId> jids = new ArrayList<>();
		
		for (String value : field.getValues()) {
			jids.add(JabberId.parse(value));
		}
		
		return jids;
	}

	private GetMemberList getGetMemberList(Field field) {
		GetMemberList getMemberList = new GetMemberList();
		for (String value : field.getValues()) {
			if ("moderator".equals(value)) {
				getMemberList.setModerator(true);
			} else if ("participant".equals(value)) {
				getMemberList.setParticipant(true);
			} else if ("visitor".equals(value)) {
				getMemberList.setVisitor(true);
			} else {
				throw new IllegalArgumentException(String.format("Invalid get member list config: %s.", value));
			}
		}
		
		return getMemberList;
	}

	private PresenceBroadcast getPresenceBroadcast(Field field) {
		PresenceBroadcast presenceBroadcast = new PresenceBroadcast();
		for (String value : field.getValues()) {
			if ("moderator".equals(value)) {
				presenceBroadcast.setModerator(true);
			} else if ("participant".equals(value)) {
				presenceBroadcast.setParticipant(true);
			} else if ("visitor".equals(value)) {
				presenceBroadcast.setVisitor(true);
			} else {
				throw new IllegalArgumentException(String.format("Invalid presence broadcast config: %s.", value));
			}
		}
		
		return presenceBroadcast;
	}

	private int getFieldIntValue(Field field) {
		return Integer.valueOf(field.getValues().get(0));
	}

	private boolean getFieldBooleanValue(Field field) {
		String value = field.getValues().get(0);
		if ("1".equals(value)) {
			return true;
		} else if ("0".equals(value)) {
			return false;
		} else {
			throw new IllegalArgumentException(String.format("%s isn't a boolean value(1 or 0).", value));
		}
	}

	private String getFieldValue(Field field) {
		if (field.getValues().isEmpty())
			return null;
		
		return field.getValues().get(0);
	}

	private boolean isStandardRoomConfig(XData form) {
		for (Field field : form.getFields()) {
			if (Field.Type.HIDDEN.equals(field.getType()) &&
					"FORM_TYPE".equals(field.getVar())) {
				return "http://jabber.org/protocol/muc#roomconfig".equals(field.getValues().get(0));
			}
		}
		
		return false;
	}
	
	protected abstract RoomConfig configure(RoomConfig roomConfig);

}
