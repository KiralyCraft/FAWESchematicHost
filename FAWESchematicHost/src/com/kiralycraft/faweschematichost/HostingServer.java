package com.kiralycraft.faweschematichost;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HostingServer extends Thread implements Runnable
{
	private int port;
	private File basefolder;
	private ServerSocket serversocket;
	public HostingServer(int port,File basefolder)
	{
		this.port = port;
		this.basefolder = basefolder;
		log("Server initialized. Not accepting connections yet.");
	}
	private boolean doInterrupt = false;
	@Override
	public void run() 
	{
		log("Server is now accepting connection.");
		try 
		{
			serversocket = new ServerSocket(port);
			while (!doInterrupt)
			{
				Socket connection = serversocket.accept();
				InetAddress remoteIP = connection.getInetAddress();
				log(remoteIP+" has connected.");
				new ConnectionHandler(connection,basefolder).start();
				
				
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}	
		
		try 
		{
			serversocket.close();
		} catch (IOException e) 
		{
			;
		}
		
	}
	@Override
	public void interrupt() 
	{
		this.doInterrupt = true;
		super.interrupt();
	}
	@Override
	public boolean isInterrupted() 
	{
		return super.isInterrupted() && this.doInterrupt;
	}
	
	public static void log(String s)
	{
		System.out.println("["+new SimpleDateFormat("HH:mm").format(new Date())+"] [FAWESchematicHost] "+s);
	}
}
