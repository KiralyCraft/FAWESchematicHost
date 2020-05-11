package com.kiralycraft.faweschematichost;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionHandler extends Thread implements Runnable
{
	private enum ActionType
	{
		UNKNOWN,UPLOAD,DOWNLOAD
	}
	private class Action
	{
		private String fileID;
		private ActionType actionType;
	}
	
	private Socket connection;
	private File baseFolder;
	private InputStream is;
	private OutputStream os;
	
	private Pattern postPattern = Pattern.compile("POST \\/upload\\.php\\?([a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}) HTTP\\/");
	private Pattern getPattern = Pattern.compile("GET \\/\\?key=([a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8})&type=schematic HTTP\\/");
	private Pattern headerPattern = Pattern.compile("(.*): (.*)");
	
	private final String boundaryDefine = "boundary=";
	private final String contentDispositionName = "name=";

	
	public ConnectionHandler(Socket connection, File basefolder) 
	{
		this.connection = connection;
		this.baseFolder = basefolder;
	}

	@Override
	public void run() 
	{
		try 
		{
			is = connection.getInputStream();
			os = connection.getOutputStream();
			
			String readSoFar = readDataChunk(is);
			
			Action action = processRequest(readSoFar);
			
			log("Receiving a "+action.actionType+" request from \""+connection.getInetAddress().getHostAddress()+"\" for schematic ID: "+action.fileID);
			
			if (!action.actionType.equals(ActionType.UNKNOWN))
			{
				HashMap<String, HashSet<String>> headerData = getHeaderFormatData(readSoFar);
				
				if (action.actionType.equals(ActionType.UPLOAD))
				{
					handleUploadFile(action.fileID,headerData);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{	
			try 
			{
				is.close();
				os.close();
			} 
			catch (Exception e2) 
			{
				;
			}
		}
	}
	private HashMap<String, HashSet<String>> getHeaderFormatData(String readSoFar) 
	{
		String[] rowData = readSoFar.split("\\r\\n");
		HashMap<String,HashSet<String>> headerData = new HashMap<String,HashSet<String>>();
		for (String header:rowData)
		{
			Matcher headerMatcher = headerPattern.matcher(header);
			if (headerMatcher.matches())//Read matching headers
			{
				String key = headerMatcher.group(1).trim();
				String value = headerMatcher.group(2).trim();
				HashSet<String> toAddHeaderData = new HashSet<String>();
				toAddHeaderData.add(value);
				headerData.put(key, toAddHeaderData);
			}
		}
		return headerData;
	}

	private String readDataChunk(InputStream is2) throws IOException 
	{
		String readSoFar = "";
		int byteRead;
		Queue<Integer> lastFourBytes = new LinkedList<Integer>();
		while ((byteRead = is.read()) > 0)
		{
			readSoFar+=(char)byteRead;
		
			if (lastFourBytes.size()==4)
			{
				lastFourBytes.poll();
				lastFourBytes.add(byteRead);
				if (byteRead == 10)
				{
					ArrayList<Integer> list = new ArrayList<Integer>(lastFourBytes);
					if (list.get(0) == 13 && list.get(1) == 10 && list.get(2) == 13 && list.get(3)==10)
					{
						break;
					}
				}
			}
			else
			{
				lastFourBytes.add(byteRead);
			}
		}
		return readSoFar;
	}

	private void handleUploadFile(String fileID, HashMap<String, HashSet<String>> headerData) throws IOException 
	{
		HashSet<String> contentTypeList = headerData.get("Content-Type");
		if (contentTypeList!=null)
		{
			String boundary = null;
			
			for (String contentType:contentTypeList)
			{
				if (boundary == null)
				{
					String[] contentTypeSplit = contentType.split(";");
					for (String possibleBoundary:contentTypeSplit)
					{
						possibleBoundary = possibleBoundary.trim();
						if (possibleBoundary.startsWith(boundaryDefine))
						{
							boundary = possibleBoundary.substring(possibleBoundary.indexOf(boundaryDefine)+boundaryDefine.length());
							break;
						}
					}
				}
				else
				{
					break;
				}
			}
			
			if (boundary!=null)
			{
				String dataChunk;
				
				
				boolean foundFile = false;
				while(!(dataChunk = readDataChunk(is)).trim().equals("--"))
				{
					HashMap<String, HashSet<String>> multipartData = getHeaderFormatData(dataChunk);
					
					if (multipartData.containsKey("Content-Disposition"))
					{
						HashSet<String> contentDisposition = multipartData.get("Content-Disposition");
						
						String contentName = null;
						for (String formDataName:contentDisposition)
						{
							if (contentName == null)
							{
								String[] formDataNameSplit = formDataName.split(";");
								for (String possibleDataFormName:formDataNameSplit)
								{
									possibleDataFormName = possibleDataFormName.trim();
									
									if (possibleDataFormName.startsWith(contentDispositionName))
									{
										contentName = possibleDataFormName.substring(possibleDataFormName.indexOf(contentDispositionName)+contentDispositionName.length()).replace("\"", "");
										break;
									}
								}
							}
							else
							{
								break;
							}
						}
						
						if (contentName!=null && contentName.equals("schematicFile"))
						{
							foundFile = true;
							boolean downloadSuccess = downloadFile(is,boundary,fileID);
							if (downloadSuccess)
							{
								log("Downloaded file with ID: "+fileID+" from "+connection.getInetAddress().getHostAddress()+" with size "+(int)Math.ceil(new File(baseFolder,fileID).length()/1024.0)+" kb");
								writeUploadOK();
							}
							else
							{
								log("Could not download schematic file from "+connection.getInetAddress().getHostAddress());
							}
						}
					}
				}
				
				if (!foundFile)
				{
					log("Could not find a suitable schematic file in the request from "+connection.getInetAddress().getHostAddress());
				}
			}
			else
			{
				log("No boundary found in Content-Type of request from "+connection.getInetAddress().getHostAddress());
			}
		}
		else
		{
			log("Received request with missing Content-Type from "+connection.getInetAddress().getHostAddress());
		}
		
		
		
	}

	private boolean downloadFile(InputStream is2, String boundary, String fileID) throws IOException 
	{
		FileOutputStream fos = new FileOutputStream(new File(baseFolder,fileID));
		
		String expectedQueue = "--"+boundary;
		int expectedQueueLength = expectedQueue.length();
		
		LinkedList<Integer> lastBytesQueue = new LinkedList<Integer>();
		
		boolean writeResult = false;
		
		int byteRead;
		while (true)
		{
			byteRead = is.read();
			fos.write(byteRead);
		
			if (lastBytesQueue.size()==expectedQueueLength)
			{
				lastBytesQueue.poll();
				lastBytesQueue.add(byteRead);
				
				
				boolean allOk = true;
				int currentIndex = 0;
				for (Integer currentByte:lastBytesQueue)
				{
					if (expectedQueue.charAt(currentIndex) != currentByte)
					{
						allOk=false;
						break;
					}
					currentIndex++;
				}
				if (allOk)
				{
					writeResult = true;
					break;
				}
			}
			else
			{
				lastBytesQueue.add(byteRead);
			}
		}
		//TODO this writes the last boundary to the file,after it's finished though it does not seem to complain
		
		if (writeResult)
		{
			fos.close();
		}
		return writeResult;
		
	}

	private Action processRequest(String readSoFar) 
	{
		Matcher tempMatcher;
		Action toReturn = new Action();
		
		if ((tempMatcher = postPattern.matcher(readSoFar)).find())
		{
			toReturn = new Action();
			toReturn.actionType = ActionType.UPLOAD;
			toReturn.fileID = tempMatcher.group(1);
			return toReturn;
		}
		
		if ((tempMatcher = getPattern.matcher(readSoFar)).find())
		{
			toReturn = new Action();
			toReturn.actionType = ActionType.DOWNLOAD;
			toReturn.fileID = tempMatcher.group(1);
			return toReturn;
		}
		
		toReturn.actionType = ActionType.UNKNOWN;
		return toReturn;
	}

	
	private void writeUploadOK() throws IOException
	{
		os.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		os.write(new String("Connection: close\r\n").getBytes());
		os.write(new String("Content-Length: 1\r\n").getBytes());
		os.write(new String("\r\n").getBytes());
		os.write(new String("<").getBytes());
		os.flush();
	}
	private void writeHeaders(BufferedOutputStream bos2, File toSend) throws IOException 
	{
		bos2.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		bos2.write(new String("Content-Type: image\r\n").getBytes());
		bos2.write(new String("Connection: close\r\n").getBytes());
		bos2.write(new String("Content-Length: "+toSend.length()+"\r\n").getBytes());
		bos2.write(new String("\r\n").getBytes());
	}
	private void writeFile(BufferedOutputStream bos2, File toSend) throws IOException
	{
		int len;
		byte fileBuf[] = new byte[4096];
		BufferedInputStream dis = new BufferedInputStream(new FileInputStream(toSend));
		while ((len = dis.read(fileBuf))>=0)
		{
			bos2.write(fileBuf, 0, len);
		}
		dis.close();
		bos2.close();
	}
	private void writeInvalidFile(BufferedOutputStream bos2) throws IOException
	{
		byte[] webpage = Base64.getDecoder().decode(StaticFieldz.MISSING_PAGE64);
		bos2.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		bos2.write(new String("Content-Type: text/html\r\n").getBytes());
		bos2.write(new String("Connection: close\r\n").getBytes());
		bos2.write(new String("Content-Length: "+webpage.length+"\r\n").getBytes());
		bos2.write(new String("\r\n").getBytes());
		bos2.write(webpage);
		bos2.close();
	}
	
	public static void log(String s)
	{
		System.out.println("["+new SimpleDateFormat("HH:mm").format(new Date())+"] [FAWESchematicHost] "+s);
	}
}
