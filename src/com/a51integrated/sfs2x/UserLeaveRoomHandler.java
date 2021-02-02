package com.a51integrated.sfs2x;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class UserLeaveRoomHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent e) throws SFSException {
		
		Mode2Ext.taskHandle.cancel(true);
		
		ISFSObject notify = new SFSObject();
		notify.putInt("stop", 0);	
		send("stop", notify, getParentExtension().getParentRoom().getUserList());
	}

}
