package com.kiralycraft.faweschematichost;

import java.io.File;

public class Main {

	public static void main(String[] args) 
	{
		HostingServer hs = new HostingServer(25532,new File("."));
		hs.start();
	}

}
