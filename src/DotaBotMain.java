import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class DotaBotMain
{
	private static String CHANNEL = "#r/dota2"; // Will have to make this functionality scalable if the bot is to be added to more channels
	//private static String CHANNEL2 = "#dota2";  // Otherwise it's fine for only 2 channels to code it like this.
	private static DotaBot bot;
	private static String SAVEPATH = "reddit.ser";
	private static String IDEAS = "ideas.txt";
	private static List<Stream> streams = new ArrayList<Stream>();
	private static List<Stream> important = new ArrayList<Stream>(); // Lets make this non-persistant
	
	private static int refreshRate = 300000; // Timeout for stream queries is 5 minutes
	public static int msgRate = 5000; // Timeout for commands
	private static int maxStreamQuery = 14;
	private static boolean notifyAll = false; // starts out as false. dont want to initially spam.
	public static boolean isNotifyAll() {
		return notifyAll;
	}

	public static void setNotifyAll(boolean notifyAll) {
		DotaBotMain.notifyAll = notifyAll;
	}

	public static boolean addImportant(String channel)
	{
		Stream stream = null;
		for(Stream s : streams)
			if(s.getChannel().equals(channel))
				stream = s;
		
		if(stream == null)
			return false;
		
		important.add(stream);
		return true;
	}
	
	public static boolean removeImportant(String channel)
	{
		Stream stream = null;
		for(Stream s : streams)
			if(s.getChannel().equals(channel))
				stream = s;
		
		if(stream == null)
			return false;
		
		important.remove(stream);
		return true;
	}
	
	public static boolean addIdea(String idea)
	{
		Writer output;
		try {
			output = new BufferedWriter(new FileWriter(IDEAS, true));
			output.append(idea);
			output.close();
			return true;
		} catch (IOException e) {
			System.out.println("Cannot add idea");
			return false;
		}
	}
	
	private static String readUrl(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	public static List<Stream> getStreams(boolean importantFlag) {
		if(!importantFlag) {
			java.util.Collections.sort(streams, new Comparator<Stream>() {
		
				@Override
				public int compare(Stream o1, Stream o2) {
					if(o1.getViewers() > o2.getViewers())
						return -1;
					return 1;
				}
				
			});
			return streams;
		}
		
		java.util.Collections.sort(important, new Comparator<Stream>() {

			@Override
			public int compare(Stream o1, Stream o2) {
				if(o1.getViewers() > o2.getViewers())
					return -1;
				return 1;
			}
			
		});
		return important;
	}
	
	public static boolean addStream(String name, String channel, String type)
	{
		for(Stream s : streams)
			if(s.getChannel().equals(channel))
				return false;
		
		streams.add(new Stream(name, channel, type));
		return true;
	}
	
	public static boolean removeStream(String channel)
	{
		for(Stream s : streams)
		{
			if(s.getChannel().equals(channel))
			{
				streams.remove(s);
				
				// Also remove from the important streams...
				if(important.contains(s))
					important.remove(s);
				
				return true;
			}
		}
		
		return false;
	}
	
	public static void main(String[] args) throws Exception
	{
		
		loadAll();
		
		bot = new DotaBot();
					
		connect();
		
		while(true)
		{
			// query the streams...
			updateAllStreams();		
			Thread.sleep(refreshRate);
			if(!bot.isConnected()) {
				System.out.println("bot isn't connected... attemping to reconnect");
				connect();
			}
			boolean found = false;
			String[] channels = bot.getChannels();
			for(String channel : channels) {
				System.out.println(channel);
				if(channel.equalsIgnoreCase(CHANNEL)) {
					found = true;
					break;
				}
			}
			
			if(!found) {
				bot.joinChannel(CHANNEL);
			}
		}
		
	}

	private static void connect() throws IOException, IrcException,
			NickAlreadyInUseException, UnsupportedEncodingException {
		
		if(bot == null)
			return;
		
		bot.setVerbose(true);
		bot.connect("se.quakenet.org");
		bot.joinChannel(CHANNEL);
		//bot.joinChannel(CHANNEL2);
		bot.setMessageDelay(1000);
		bot.setEncoding("UTF8");
			
		updateAllStreams();				
		saveAll();
	}

	private static List<String> getStreamQueryStrings(String type)
	{
		List<String> streamList = new ArrayList<String>();
		int counter = 0;
		String current = "";
		
		for(Stream s : streams)
		{
			if(!s.getType().equals(type))
				continue;
			
			current = current.concat(s.getChannel()+",");
			counter++;
			
			if(counter >= maxStreamQuery)
			{
				current = current.substring(0,current.length()-1); // Remove trailing comma
				streamList.add(current);
				counter = 0;
				current = "";				
			}
		}
		
		// Take care of the last one...
		if(counter > 0 && !current.isEmpty())
		{
			current = current.substring(0,current.length()-1); // Remove trailing comma
			streamList.add(current);
		}
		
		return streamList;
	}
	
	public static void updateTwitch()
	{
		List<String> streamList = new ArrayList<String>();
		List<Stream> newStreams = new ArrayList<Stream>();
		List<Stream> onlineStream = new ArrayList<Stream>();
		
		streamList = getStreamQueryStrings("twitch");
		
		// Query the twitch API
		for(int i=0;i<streamList.size();i++)
		{
			String address = "http://api.justin.tv/api/stream/list.json?channel="+streamList.get(i);
			try {				
				JsonFactory factory = new JsonFactory();				
				JsonParser jsonParser;
				try {
					jsonParser = factory.createJsonParser(readUrl(address));
				} catch (Exception e) {
					System.out.println("Couldn't open the address "+address);
					return;
				}

				Stream curStream = null;
				String status = "";
				String c = "";
				int viewers = 0;
				while(jsonParser.nextToken() != JsonToken.END_ARRAY)
				{					
					String fieldName = jsonParser.getCurrentName();
					if(fieldName == null)
						continue;				
					
					if(fieldName.equals("status"))
					{
						jsonParser.nextToken();
						status = jsonParser.getText().replaceAll("\n","").replaceAll("\r","");
						
					}	
					
					if(fieldName.equals("login"))
					{
						jsonParser.nextToken();

						c = jsonParser.getText();						
						System.out.println(c);
					}
					
					if(fieldName.equals("channel_count"))
					{
						jsonParser.nextToken();
						viewers = jsonParser.getIntValue();
						
						/*if(curStream == null) // Shouldn't ever happen
						{
							System.out.println("Error, curstream is null");
							continue;
						}*/
												
					}
					
					if(!c.isEmpty() && !status.isEmpty() && viewers > 0)
					{
						System.out.println(("viewers="+viewers));
						System.out.println("channel="+c+" firststatus="+status+" viewers="+viewers);
						
						boolean found = false;
						for(Stream s : streams)
						{
							if(s.getChannel().equals(c))
							{
								curStream = s;
								found = true;
								break;
							}
						}
						
						if(!found)
							continue;
						
						onlineStream.add(curStream);
						curStream.setViewers(viewers);
						
						if(!curStream.isOnline()) // Save the current stream in the new list for use later.
						{						
							if(important.contains(curStream) || notifyAll)
							{
								if(curStream.getType().equalsIgnoreCase("twitch"))
								{
									bot.output(CHANNEL, "\u0002http://www.twitch.tv/"+curStream.getChannel()+"\u000F is now online! "+status);
									//bot.output(CHANNEL2, "\u0002http://www.twitch.tv/"+curStream.getChannel()+"\u000F is now online! "+status);

								}
								else
								{
									bot.output(CHANNEL, "\u0002"+curStream.getName()+"\u000F is now online! "+curStream.getUrl());
									//bot.output(CHANNEL2, "\u0002"+curStream.getName()+"\u000F is now online! "+curStream.getUrl());
								}

								try {
									Thread.sleep(msgRate);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							curStream.setOnline(true);
							newStreams.add(curStream);
						}
						
						c = "";
						status = "";
						viewers = 0;
					}
				}						
				
			} catch (IOException e) {
				System.out.println("Couldn't open address "+address);
				e.printStackTrace();
			}
		} // End of API query loop
		
		// Turn off all the streams that we didn't find were online
		for(Stream s : streams)
			if(s.getType().equals("twitch") && !onlineStream.contains(s))
				s.setOnline(false);
	}
	
	// R.I.P Own3d
	/*
	public static void updateOwn3d()
	{
		List<String> streamList = new ArrayList<String>();
		List<Stream> newStreams = new ArrayList<Stream>();
		List<Stream> onlineStream = new ArrayList<Stream>();
		
		streamList = getStreamQueryStrings("own3d");
		
		// Query the own3d API
		for(int i=0;i<streamList.size();i++)
		{
			String address = "http://api.own3d.tv/rest/live/list.json?liveid="+streamList.get(i);
			System.out.println("own3d address is "+address);
			try {
				JsonFactory factory = new JsonFactory();				
				JsonParser jsonParser;
				try {
					jsonParser = factory.createJsonParser(readUrl(address));
				} catch (Exception e) {
					System.out.println("Couldn't open the address "+address);
					return;
				}

				Stream curStream = null;
				while(jsonParser.nextToken() != JsonToken.END_ARRAY)
				{					
					String fieldName = jsonParser.getCurrentName();
					if(fieldName == null)
						continue;				
					
					if(fieldName.equals("live_id"))
					{
						jsonParser.nextToken();
						String c = jsonParser.getText();

						System.out.println(c);
						for(Stream s : streams)
							if(s.getChannel().equals(c))
								curStream = s;
						
						onlineStream.add(curStream);
						
						if(!curStream.isOnline()) // Save the current stream in the new list for use later.
						{
							if(important.contains(curStream)) {
								bot.sendMessage(CHANNEL, curStream.getName()+" is now online! "+curStream.getUrl());
								bot.sendMessage(CHANNEL2, curStream.getName()+" is now online! "+curStream.getUrl());
							}
							curStream.setOnline(true);
							newStreams.add(curStream);
						}
					}
					
					if(fieldName.equals("live_viewers"))
					{
						jsonParser.nextToken();
						int viewers = Integer.parseInt(jsonParser.getText());
						
						if(curStream == null) // Shouldn't ever happen
						{
							System.out.println("Error, curstream is null");
							continue;
						}
						
						curStream.setViewers(viewers);
					}				
				}						
				
			} catch (IOException e) {
				System.out.println("Couldn't open address "+address);
				e.printStackTrace();
			}
		} // End of API query loop
		
		// Turn off all the streams that we didn't find were online
		for(Stream s : streams)
			if(s.getType().equals("own3d") && !onlineStream.contains(s))
				s.setOnline(false);
	}
	*/
	
	public static void updateAllStreams()
	{
		updateTwitch();
		
		// RIP own3d
		//updateOwn3d();
		
	}
	
	public static void saveAll()
	{
		
		try {
			FileOutputStream saveFile = new FileOutputStream(SAVEPATH);		
			ObjectOutputStream save = new ObjectOutputStream(saveFile);
			
			
			int size = streams.size();
			save.writeObject(size);
			
			Iterator<Stream> i = streams.iterator();
			while(i.hasNext())
			{
				Stream t = i.next();
				save.writeObject(t);
			}
			
			save.close();
			saveFile.close();
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
		
	}
	
	public static void loadAll()
	{
		
		try {
			FileInputStream saveFile = new FileInputStream(SAVEPATH);			
			ObjectInputStream save = new ObjectInputStream(saveFile);
			
			int size = (Integer) save.readObject();
			for(int i=0;i<size;i++)
			{
				Stream t = (Stream) save.readObject();
				streams.add(t);
			}
			
			save.close();	
			saveFile.close();
			System.out.println("Loaded data successfully...");
		}
		catch(Exception exc) {
			System.out.println("Couldn't load file or empty file...");
			exc.printStackTrace();
		}
		
	}
}