package com.a51integrated.sfs2x;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class UserJoinRoomHandler extends BaseServerEventHandler {

	public List<String> RandomRangeGenerator(int min, int max)
	{
		List<Integer> list = new ArrayList<Integer>();
		
		for(int i = 0; i < ((Mode2Ext.rows-2)*(Mode2Ext.columns-2))/2; i++)
		{
			list.add(ThreadLocalRandom.current().nextInt(min, max + 1));
		}
		
		List<Integer> clonedList = new ArrayList<Integer>(list);
		
		for(int i = 0; i < clonedList.size(); i++)
		{
			list.add(clonedList.get(i));
		}
		
		ShuffleList(list);
		
		List<String> finalList = new ArrayList<String>();
		for(int i = 0; i < list.size(); i++)
		{
			finalList.add(list.get(i).toString());
		}
		
		return finalList;
	}
	
	public void ShuffleList(List<Integer> list)
	{
		Random rnd = ThreadLocalRandom.current();
		for(int i = list.size() -1; i > 0; i--)
		{
			int index = rnd.nextInt(i + 1);
			int temp = list.get(index);
			list.set(index, list.get(i));
			list.set(i, temp);
		}
	}
	
	@Override
	public void handleServerEvent(ISFSEvent e) throws SFSException {
		Room r = (Room)e.getParameter(SFSEventParam.ROOM);
		
		if(r.getName() != "The Lobby"  && r.getUserList().toArray().length == r.getMaxUsers())
		{	
			List<String> list = RandomRangeGenerator(0, 35);
			String listToString = String.join(",", list); 
			
			Mode2Ext.SpriteIndexList = listToString;
			
			ISFSObject rtn = new SFSObject();
			rtn.putUtfString("il", listToString);
			Mode2Ext parentEx = (Mode2Ext) getParentExtension();
			parentEx.send("start", rtn, getParentExtension().getParentRoom().getUserList());

		}
	}
}
