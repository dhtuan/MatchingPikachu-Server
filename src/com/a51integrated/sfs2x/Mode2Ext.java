package com.a51integrated.sfs2x;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class Mode2Ext extends SFSExtension {

	public static int rows = 7;
	public static int columns = 12;
	public static int [][]BtnStat = new int[rows][columns];
	public static String SpriteIndexList;
	
	public class TaskRunner implements Runnable {
		public int runningCycles = 61;
		
		public void run()
		{
			try
			{				
				runningCycles--;
				trace("Inside the running task. Cycle: " + runningCycles);
				
				
				if(runningCycles <= 0)
				{
					trace("Time to stop the task!");
									
					taskHandle.cancel(true);
					
					ISFSObject rtn = new SFSObject();
					rtn.putInt("stop", 0);	
					send("stop", rtn, getParentRoom().getUserList());
					
					trace("Send to: "+ getParentRoom().getUserList());
				}
			}
			catch(Exception e)
			{
				
			}
		}
	}
	static ScheduledFuture<?> taskHandle;
	
	public void BtnStatInit()
	{
		for(int i = 0; i < rows; i++)
		{
			for(int j = 0; j < columns; j++)
			{
				if(i == 0 || i == 6 || j == 0 || j == 11)
				{
					BtnStat[i][j] = 0;
				}
				else
				{
					BtnStat[i][j] = 2;
				}
			}
		}		
	}
	
	public void init() 
	{
		BtnStatInit();	
		
		this.addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomHandler.class);
		this.addEventHandler(SFSEventType.USER_LEAVE_ROOM, UserLeaveRoomHandler.class);
	}	
	
	@Override
	public void handleClientRequest(String requestId, User sender, ISFSObject params)
	{
		switch (requestId)
		{
			case "g":
				if(getParentRoom().getName().contains(sender.getName()))
				{					
					SmartFoxServer sfs = SmartFoxServer.getInstance(); 
			        taskHandle = sfs.getTaskScheduler().scheduleAtFixedRate(new TaskRunner(), 0, 1, TimeUnit.SECONDS);	
				}

				break;
				
			case "move":
				String FirstName = params.getUtfString("n1");
				int FirstXIndex = params.getInt("x1");
				int FirstYIndex = params.getInt("y1");
				
				String SecondName = params.getUtfString("n2");
				int SecondXIndex = params.getInt("x2");
				int SecondYIndex = params.getInt("y2");
				
				Mode2Ext.BtnStat[FirstXIndex][FirstYIndex] = 1;
				Mode2Ext.BtnStat[SecondXIndex][SecondYIndex] = 1;		
				
				ISFSObject rtn = new SFSObject();
				if(CheckIfPuzzleMatch(FirstName, FirstXIndex, FirstYIndex, SecondName, SecondXIndex, SecondYIndex) == true)
				{
					Mode2Ext.BtnStat[FirstXIndex][FirstYIndex] = 0;
					Mode2Ext.BtnStat[SecondXIndex][SecondYIndex] = 0;			
					BtnsStatUpdate(FirstXIndex, FirstYIndex, SecondXIndex, SecondYIndex);

					rtn.putInt("ok", 1);
				
					if(GameEnd())
					{
						taskHandle.cancel(true);
						
						ISFSObject notify = new SFSObject();
						notify.putInt("stop", 0);	
						send("stop", notify, getParentRoom().getUserList());
						break;
					}
					
					if(!ThereValidMove())
					{
						rtn.putInt("nvm", -1);
						
						trace("ThereValidMove: " + ThereValidMove());
					}
					
					send("ok", rtn, sender);
					break;
				}
				else
				{		
					Mode2Ext.BtnStat[FirstXIndex][FirstYIndex] = 2;
					Mode2Ext.BtnStat[SecondXIndex][SecondYIndex] = 2;		
					
					rtn.putInt("no", 0);		
					send("no", rtn, sender);

					break;
				}
				
			case "nvm":
				SFSObject data = new SFSObject();
				String spriteStatString = data.getUtfString("ss");
				
				String[] spriteStatList = spriteStatString.split(",");				
				String[] spriteIndexList = SpriteIndexList.split(",");
				
				List<Integer> SpriteStat = new ArrayList<Integer>();
				List<Integer> SpriteIndex = new ArrayList<Integer>();
 				
				for(int i = 0; i < spriteIndexList.length; i++)
				{
					SpriteStat.add(Integer.parseInt(spriteStatList[i]));
					SpriteIndex.add(Integer.parseInt(spriteIndexList[i]));
				}
				
				Random rnd = ThreadLocalRandom.current();
				for(int i = spriteIndexList.length -1; i > 0; i--)
				{
					if(SpriteStat.get(i) == 0)
					{
						continue;
					}
					int index = rnd.nextInt(i + 1);
					int temp = SpriteIndex.get(index);
					SpriteIndex.set(index, SpriteIndex.get(i));
					SpriteIndex.set(i, temp);
				}
				
				List<String> finalList = new ArrayList<String>();
				for(int i = 0; i < SpriteIndex.size(); i++)
				{
					finalList.add(SpriteIndex.get(i).toString());
				}
				
				String SpriteIndexToString = String.join(",", finalList);
				
				List<RoomVariable> RoomSprites = new ArrayList<RoomVariable>();
				RoomSprites.add(new SFSRoomVariable("sp", SpriteIndexToString));
				getApi().setRoomVariables(null, getParentRoom(), RoomSprites);
				
				break;
		}
	}
	
	public void BtnsStatUpdate(int x1, int y1, int x2, int y2)
	{
		List<String> BtnsName = new ArrayList<String>();
		List<String> BtnsStat = new ArrayList<String>();
		for(int i = 0; i < Mode2Ext.rows; i++)
		{
			for(int j = 0; j < Mode2Ext.columns; j++)
			{
				String name = i + " : " + j;
				String stat = Integer.toString(Mode2Ext.BtnStat[i][j]);
				
				BtnsName.add(name);
				BtnsStat.add(stat);
			}
		}
		String BtnsNameString = String.join(",", BtnsName);
		String BtnsStatString = String.join(",", BtnsStat);
		
		List<String> MatchedPos = new ArrayList<String>();
		MatchedPos.add(Integer.toString(x1));
		MatchedPos.add(Integer.toString(y1));
		MatchedPos.add(Integer.toString(x2));
		MatchedPos.add(Integer.toString(y2));
		String MatchedPosString = String.join(",", MatchedPos);
		
		List<RoomVariable> RoomBtns = new ArrayList<RoomVariable>();
		RoomBtns.add(new SFSRoomVariable("bn", BtnsNameString));		
		RoomBtns.add(new SFSRoomVariable("bs", BtnsStatString));
		RoomBtns.add(new SFSRoomVariable("m", MatchedPosString));
		getApi().setRoomVariables(null, getParentRoom(), RoomBtns);
	}
	
	public synchronized boolean CheckIfPuzzleMatch(String n1, int x1, int y1, String n2, int x2, int y2)
	{	
		if(n1.equals(n2) && (x1 != x2 || y1 != y2))
		{
			if(PuzzleIsMatch(x1, y1, x2, y2))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean PuzzleIsMatch(int x1, int y1, int x2, int y2)
	{
		try
		{
			if(x1 == x2)
	        {
	            if(CheckLineX(y1, y2, x1))
	            {
	                return true;
	            }
	            if(CheckMoreLineY(x1, y1, x2, y2, 1) != -1)
	            {
	                return true;
	            }
	            if(CheckMoreLineY(x1, y1, x2, y2, -1) != -1)
	            {
	                return true;
	            }
	        }
	        else if(y1 == y2)
	        {
	            if(CheckLineY(x1, x2, y1))
	            {
	                return true;
	            }
	            if(CheckMoreLineX(x1, y1, x2, y2, 1) != -1)
	            {
	                return true;
	            }
	            if(CheckMoreLineX(x1, y1, x2, y2, -1) != -1)
	            {
	                return true;
	            }
	        }
	        else
	        {
	            if(CheckRectY(x1, y1, x2, y2) != -1)
	            {
	                return true;
	            }
	            if(CheckRectX(x1, y1, x2, y2) != -1)
	            {
	                return true;
	            }
	            if(CheckMoreLineX(x1, y1, x2, y2, 1) != -1)
	            {
	                return true;
	            }
	            if(CheckMoreLineX(x1, y1, x2, y2, -1) != -1)
	            {
	                return true;
	            }
	            if (CheckMoreLineY(x1, y1, x2, y2, 1) != -1)
	            {
	                return true;
	            }
	            if(CheckMoreLineY(x1, y1, x2, y2, -1) != -1)
	            {
	                return true;
	            }
	        }
	        return false;
		}
		catch(Exception e)
		{
			
		}
		return false;
	}

	public boolean CheckLineX(int y1, int y2, int x) 
	{
		int min = Math.min(y1, y2);
		int max = Math.max(y1, y2);
		
		for(int y = min; y <= max; y++)
		{
			if(Mode2Ext.BtnStat[x][y] == 2)
			{
				return false;
			}
		}		
		return true;
	}
	
	public boolean CheckLineY(int x1, int x2, int y)
	{
		int min = Math.min(x1, x2);
		int max = Math.max(x1, x2);
		
		for(int x = min; x <= max; x++)
		{
			if(Mode2Ext.BtnStat[x][y] == 2)
			{
				return false;
			}
		}		
		return true;
	}

	public int CheckRectX(int x1, int y1, int x2, int y2)
	{
		int Xmin = x1, Ymin = y1;
		int Xmax = x2, Ymax = y2;
		
		if(y1 > y2)
		{
			Xmin = x2; Ymin = y2;
			Xmax = x1; Ymax = y1;
		}
		
		for(int y = Ymin; y <= Ymax; y++)
		{
			if(CheckLineX(Ymin, y, Xmin) && CheckLineY(Xmin, Xmax, y) && CheckLineX(y, Ymax, Xmax)
					&& Mode2Ext.BtnStat[Xmax][y] != 2 && Mode2Ext.BtnStat[Xmin][y] != 2)
			{
				return y;
			}
		}	
		return -1;
	}

	public int CheckRectY(int x1, int y1, int x2, int y2)
	{
		int Xmin = x1, Ymin = y1;
		int Xmax = x2, Ymax = y2;
		
		if(x1 > x2)
		{
			Xmin = x2; Ymin = y2;
			Xmax = x1; Ymax = y1;
		}
		
		for(int x = Xmin; x <= Xmax ; x++)
		{
			if(CheckLineY(Xmin, x, Ymin) && CheckLineX(Ymin, Ymax, x) && CheckLineY(x, Xmax, Ymax) 
					&& Mode2Ext.BtnStat[x][Ymax] != 2 && Mode2Ext.BtnStat[x][Ymin] != 2)
			{
				return x;
			}
		}		
		return -1;
	}

	public int CheckMoreLineX(int x1, int y1, int x2, int y2, int type)
	{
		int Xmin = x1, Ymin = y1;
		int Xmax = x2, Ymax = y2;
		
		if(y1 > y2)
		{
			Xmin = x2; Ymin = y2;
			Xmax = x1; Ymax = y1;
		}
		
		int y = Ymax;
		int row = Xmin;
		
		if(type == -1)
		{
			y = Ymin;
			row = Xmax;
		}
		
		if(CheckLineX(Ymin, Ymax, row))
		{
			while(type == -1 ? y >= 0 : y <= Mode2Ext.columns)
			{
				if(CheckLineX(Ymin, y, Xmin) && CheckLineY(Xmin, Xmax, y) && CheckLineX(y, Ymax, Xmax)
						&& Mode2Ext.BtnStat[Xmin][y] != 2 && Mode2Ext.BtnStat[Xmax][y] != 2)
				{
					return y;
				}
				y +=type;
			}
		}	
		return -1;
	}
	
	public int CheckMoreLineY(int x1, int y1, int x2, int y2, int type) 
	{
		int Xmin = x1, Ymin = y1;
		int Xmax = x2, Ymax = y2;
		
		if (x1 > x2)
		{
			Xmin = x2; Ymin = y2;
			Xmax = x1; Ymax = y1;
		}
		
		int x = Xmax;
		int col = Ymin;
		
		if (type == -1)
		{
			x = Xmin;
			col = Ymax;
		}
		
		if (CheckLineY(Xmin, Xmax, col))
		{
			while (type == -1 ? x >= 0 : x <= Mode2Ext.rows)
			{
				if (CheckLineY(Xmin, x, Ymin) && CheckLineX(Ymin, Ymax, x) && CheckLineY(x, Xmax, Ymax) 
						&& Mode2Ext.BtnStat[x][Ymin] != 2 && Mode2Ext.BtnStat[x][Ymax] != 2)
				{
					return x;
				}
				x += type;
			}
		}
		return -1;
	}

	public boolean ThereValidMove()
	{
		String[] list = SpriteIndexList.split(",");
		List<Integer> SpriteList = new ArrayList<Integer>();
		for(int i = 0; i < list.length; i++)
		{
			SpriteList.add(Integer.parseInt(list[i]));
		}
		int len1 = 0;
		int len2 = 0;
		
		for(int i = 1; i < rows - 1; i++)
		{
			for(int j = 1; j < columns - 1; j++)
			{
				if(BtnStat[i][j] == 0)
				{
					continue;
				}
				else
				{
					for(int m = 1; m < rows - 1; m++)
					{
						for(int n = 1; n < columns - 1; n++)
						{
							if(BtnStat[m][n] == 0 || (m == i && n == j))
							{
								continue;
							}
							else
							{
								BtnStat[i][j] = 1;
								BtnStat[m][n] = 1;
								if(PuzzleIsMatch(i, j, m, n) && SpriteList.get(len1) == SpriteList.get(len2))
								{
									BtnStat[i][j] = 2;
									BtnStat[m][n] = 2;
																	
									return true;
								}
								BtnStat[i][j] = 2;
								BtnStat[m][n] = 2;
							}
							len2++;
						}
					}
				}
				len1++;
			}
		}
		
		return false;
	}
	
	public boolean GameEnd()
	{
		for(int i = 1; i < rows - 1; i++)
		{
			for(int j = 1; j < columns - 1; j++)
			{
				if(BtnStat[i][j] == 2)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void destroy() 
	{
		super.destroy();
		
		if(taskHandle != null)
		{
			taskHandle.cancel(true);
		}
	}
}
