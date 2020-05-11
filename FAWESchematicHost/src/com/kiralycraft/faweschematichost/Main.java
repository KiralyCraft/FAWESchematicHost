package com.kiralycraft.faweschematichost;

import java.io.File;

public class Main {

	public static void main(String[] args) 
	{
		String theFolder = ".";
		if (args.length >= 1)
		{
			theFolder = args[0];
		}
		
		int port = 25532;
		if (args.length >= 2)
		{
			port = Integer.parseInt(args[1]);
		}
		
		HostingServer hs = new HostingServer(port,new File(theFolder));
		hs.start();
	}

}
