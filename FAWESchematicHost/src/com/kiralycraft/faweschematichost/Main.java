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
		HostingServer hs = new HostingServer(25532,new File(theFolder));
		hs.start();
	}

}
