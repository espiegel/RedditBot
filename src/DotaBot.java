import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jibble.pircbot.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class DotaBot extends PircBot
{
	public static final long IRC_POST_INTERVAL = 3000;
    public static final int API_LIMIT_WARN_THRESHOLD = 45;
    
	//private String SAVEPATH = "botdata.ser";
	private String CHANNEL = "#r/dota2";
	private String MYNAME = "RedditBot"; // Changed from KaelTheInvoker
	private static int STREAMDELAY = 5000;
	
	private long time = getCurrentTime() - STREAMDELAY;
	
	public DotaBot()
	{
		setName(MYNAME);
		setVersion("0.1 alpha");
		setLogin(MYNAME);
		loadAll(CHANNEL);
	}
	
	private long getCurrentTime()
	{
		return Calendar.getInstance().getTimeInMillis();
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
		
	protected void onDisconnect()
	{
		saveAll(CHANNEL);
	}
	
	protected void onJoin(String channel,
            String sender,
            String login,
            String hostname)
	{
	}
		
	protected void onPart(String channel, String sender, String login, String hostname)
	{
	}
	
	// Parses all outgoing output from the bot
	public void output(String channel, String msg)
	{
		sendMessage(channel, msg);
		try {
			Thread.sleep(DotaBotMain.msgRate);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean checkOp(String channel, String name)
	{
		boolean op = false;
		User[] users = getUsers(channel);
		
		for(int i=0;i<users.length;i++)
		{
			if(users[i].getNick().equals(name))
			{		
				if(users[i].isOp() || users[i].getNick().equals("Zaknafein"))
					op = true;
			}
		}
		
		return op;
	}
	
	/*
	private boolean userInChannel(String channel, String name)
	{
		User[] users = getUsers(channel);
		
		for(int i=0;i<users.length;i++)
			if(users[i].getNick().equals(name))
				return true;
		
		return false;
	}
	*/
	
	private void saveAll(String channel)
	{
		DotaBotMain.saveAll();
	}
	
	private void loadAll(String channel)
	{
	}
	
	
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
	{		
		saveAll(CHANNEL);	
	}
	

	private String readUrl(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	    	System.out.println("about to open url");
	        URL url = new URL(urlString);
	        
	        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
	        HttpURLConnection.setFollowRedirects(false);
	        huc.setConnectTimeout(15 * 1000);
	        huc.connect();
	        InputStream input = huc.getInputStream();
	        reader = new BufferedReader(new InputStreamReader(input));
	        
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        System.out.println("finished getting url");
	        return buffer.toString();
	    }
	    catch(Exception e)
	    {
	    	throw new Exception("Couldn't open address");
	    }
	    finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	private List<Match> getJoinDotaMatches() {
		String address = "http://api.dotaprj.me/v2/jd/";
		String output = "";
		List<Match> matches = new ArrayList<Match>();
		
		try {
			String response = readUrl(address);

			JSONObject json = (JSONObject) JSONValue.parse(response);
			//System.out.println(json);

			JSONArray upcoming = (JSONArray) json.get("upcoming");
			//System.out.println(upcoming);

			for(Object o : upcoming) {
				JSONObject match = (JSONObject)o;
				//System.out.println(match);

				String tournament = (String) match.get("eventName");
				Long time = (Long) match.get("timeStamp");
				String team1name = (String) match.get("team1");
				String team2name = (String) match.get("team2");

				//long time = Long.parseLong(timestr);
				long myTime = System.currentTimeMillis();
				long countdown = time * 1000 - myTime;// - 10800000;

				Match m = new Match(team1name, team2name, tournament, countdown);					
				matches.add(m);
				
				/*Date date = new Date(countdown);

				DateFormat formatterHour = new SimpleDateFormat("HH");
				DateFormat formatterMin = new SimpleDateFormat("mm");

				String formatTime;
				if(countdown < 0)
					formatTime = "LIVE";
				else
					formatTime = (countdown / (24*3600*1000))+"d"+formatterHour.format(date)+"h"+formatterMin.format(date)+"m";

				output = output + (output.equals("")?"":" | ")+formatTime+" \u0002"+team1name.replaceAll(" ","")+" vs. "+team2name.replaceAll(" ","")+"\u000F "+tournament;				
                */
				
				
			}
			return matches;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// old
	@Deprecated
	private List<Match> oldGetJoinDotaMatches()
	{
		String address = "http://api.dotaprj.me/v2/jd/";
		String output = "";
		List<Match> matches = new ArrayList<Match>();
		
		try {				
			JsonFactory factory = new JsonFactory();				
			JsonParser jsonParser;
			try {
				jsonParser = factory.createJsonParser(readUrl(address));
			} catch (Exception e) {
				System.out.println("Couldn't open the address "+address);
				return null;
			}

			int count = 0;
			while(count < 2)
			{
				if(jsonParser.nextToken() == JsonToken.END_ARRAY)
					count++;
				
				System.out.println(jsonParser.getCurrentToken()+", "+jsonParser.getText());

				if(jsonParser.getCurrentToken() == JsonToken.START_OBJECT)
				{
					System.out.println("start of array!");
					String tournament = "";
					String timestr = "";
					String team1name = "";
					String team2name = "";
					while(jsonParser.nextToken() != JsonToken.END_OBJECT)
					{
						String fieldName = jsonParser.getCurrentName();						
						String text = jsonParser.getText();

						if(fieldName == null)
							continue;
						
						if(fieldName.equals("eventName"))
							tournament = text;
						if(fieldName.equals("timeStamp"))
							timestr = text;
						if(fieldName.equals("team1"))
							team1name = text;
						if(fieldName.equals("team2"))
							team2name = text;												
					}
									
					long time = Long.parseLong(timestr);
					long myTime = System.currentTimeMillis();
					long countdown = time * 1000 - myTime;// - 7200000;// - 10800000;

					Match m = new Match(team1name, team2name, tournament, countdown);					
					matches.add(m);
					
					/*Date date = new Date(countdown);

					DateFormat formatterHour = new SimpleDateFormat("HH");
					DateFormat formatterMin = new SimpleDateFormat("mm");

					String formatTime;
					if(countdown < 0)
						formatTime = "LIVE";
					else
						formatTime = (countdown / (24*3600*1000))+"d"+formatterHour.format(date)+"h"+formatterMin.format(date)+"m";

					output = output + (output.equals("")?"":" | ")+formatTime+" \u0002"+team1name.replaceAll(" ","")+" vs. "+team2name.replaceAll(" ","")+"\u000F "+tournament;
					System.out.println("end of array");*/
				}
			}
			return matches;
		} catch (IOException e) {
			System.out.println("Couldn't open address "+address);
			e.printStackTrace();
			return null;
		}
	}
	// Old - R.I.P skyride's website
	/*private String getJoinDotaMatches()
	{
		String address = "http://skyride.org/rdota2/json.php";
		String output = "";
		int count = 0;
		
		try {				
			JsonFactory factory = new JsonFactory();				
			JsonParser jsonParser;
			String url = readUrl(address);
			System.out.println(url);
			if(url.isEmpty() || url.equals("[]"))
				return output;
			
			jsonParser = factory.createJsonParser(url);

			while(jsonParser.nextToken() != JsonToken.END_ARRAY)
			{
				if(count >= 5)
					break;
				
				if(jsonParser.getCurrentToken() == JsonToken.START_OBJECT)
				{
					count++;
					String tournament = "";
					String timestr = "";
					String team1name = "";
					String team2name = "";
					while(jsonParser.nextToken() != JsonToken.END_OBJECT)
					{
						String fieldName = jsonParser.getCurrentName();						
						String text = jsonParser.getText();
						
						if(fieldName.equals("tournament"))
							tournament = text;
						if(fieldName.equals("time"))
							timestr = text;
						if(fieldName.equals("team1name"))
							team1name = text;
						if(fieldName.equals("team2name"))
							team2name = text;												
					}
					
					// Don't display a match that has a screwed up time
					if(timestr.isEmpty())
						continue;
					
					long time = Long.parseLong(timestr);
					long myTime = System.currentTimeMillis();
					long countdown = time * 1000 - myTime;// - 10800000;
					
					long minutes = (countdown / (60*1000));// + 120;					
					long days = minutes / (24*60);
					long hours = (minutes / 60) % 24;
					long rminutes = minutes % 60;
					
					String formatTime;
					if(countdown < 0)
						formatTime = "LIVE";
					else
						formatTime = days+"d "+hours+"h "+rminutes+"m";
					
					output = output + (output.equals("")?"":" | ")+formatTime+" \u0002"+team1name.replaceAll(" ","")+" vs. "+team2name.replaceAll(" ","")+"\u000F "+tournament;
				}
				
			}	
			return output;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error parsing data";
		}
	}*/
	
	// Gosugamers website
	@Deprecated
	private String getMatches()
	{
		String address = "http://skyride.org/rdota2/matches.json"; // reddit sidebar json
		String output = "";
		int count = 0;
		
		try {				
			JsonFactory factory = new JsonFactory();				
			JsonParser jsonParser;
			String url = readUrl(address);
			System.out.println(url);
			if(url.isEmpty() || url.equals("[]"))
				return output;
			
			jsonParser = factory.createJsonParser(url);

			while(jsonParser.nextToken() != JsonToken.END_OBJECT)
			{					
				String fieldName = jsonParser.getCurrentName();
				if(fieldName == null)
					continue;							
				
				if(count >= 5)
					break;
				
				if(fieldName.startsWith("game"))
				{
					jsonParser.nextToken();
					
					count++;
					int i=0;
					String name = "*";
					String team1 = "*";
					String team2 = "*";
					long time=0;
					while(jsonParser.nextToken() != JsonToken.END_ARRAY)
					{
						i++;
						if(i == 1)
							name = jsonParser.getText();							
						if(i == 2)
							time = Long.parseLong(jsonParser.getText());
						if(i == 5)
							team1 = jsonParser.getText();
						if(i == 9)
							team2 = jsonParser.getText();
						

						//System.out.println(jsonParser.getText());
					}
					
					long myTime = System.currentTimeMillis();
					long countdown = time * 1000 - myTime;// - 10800000;

					long minutes = (countdown / (60*1000));// + 120;					
					long days = minutes / (24*60);
					long hours = (minutes / 60) % 24;
					long rminutes = minutes % 60;
					
					/*
					Date date = new Date(countdown);
					DateFormat formatterHour = new SimpleDateFormat("HH");
					DateFormat formatterMin = new SimpleDateFormat("mm");
					*/
					
					String formatTime;
					if(countdown < 0) // changed from -7200000
						formatTime = "LIVE";
					else
						//formatTime = (countdown / (24*3600*1000))+"d"+formatterHour.format(date)+"h"+formatterMin.format(date)+"m";
						formatTime = days+"d "+hours+"h "+rminutes+"m";
					
					output = output + (output.equals("")?"":" | ")+formatTime+" \u0002"+team1+" vs. "+team2+"\u000F "+name;
				}
			}						
			
			return output;
		} catch (Exception e) {
			System.out.println("Couldn't open address "+address);
			e.printStackTrace();
			return "Error parsing data";
		}
	}
	
	private String getMatchDetails(String name, String match)
	{
		String url = "http://dotabuff.com/matches/"+match;
		String result = "";
		
		try
		{
			Document doc = Jsoup.connect(url).get();		
			Elements e = doc.select("a:contains("+name+")");		
			Element row = e.first().parent().parent();		
			String hero = row.getElementsByClass("hero-link").text();		
			Elements data = row.getElementsByClass("cell-centered");
			
			String level = data.get(0).text();		
			String kills = data.get(1).text();		
			String deaths = data.get(2).text();		
			String assists = data.get(3).text();		
			String gold = data.get(4).text();		
			String lh = data.get(5).text();		
			String dn = data.get(6).text();	
			String xpm = data.get(7).text();		
			String gpm = data.get(8).text();
			
			Elements items = row.getElementsByClass("image-container-item");
			List<String> itemNames = new ArrayList<String>();
			for(Element i : items)
			{
				String itemName = i.child(0).attr("href");
				itemName = itemName.replaceFirst("/items/", "").replaceAll("-", " ");
				itemNames.add(itemName);
			}
			
			result += name+" played "+hero+" level "+level+", "+kills+" kills, "+deaths+" deaths, "+assists+" assists, "+
								gold+" gold, "+lh+" lasthits, "+dn+" denies, "+xpm+" xpm, "+gpm+" gpm. Items: ";
			
			if(itemNames.isEmpty())
				result += "None";
			
			Iterator<String> i = itemNames.iterator();
			while(i.hasNext())
			{
				result += i.next();
				if(i.hasNext())
					result += ", ";
			}
			
			return result;
		}
		catch (Exception e) {
			return "Error parsing data";
		}
	}
	public void onMessage(String channel, String sender, String login,
			String hostname, String  message)
	{
		if( (int)(Math.random() * (500)) == 0)
			saveAll(CHANNEL);		
				
		if(message.equals("!save"))
		{
			if(!checkOp(channel,sender))
				return;
			
			saveAll(CHANNEL);
		}
		
		if(message.equals("!notifyon"))
		{
			if(!checkOp(channel,sender))
				return;
			
			DotaBotMain.setNotifyAll(true);
			output(channel, "Stream notifications are now on.");
			saveAll(CHANNEL);
		}
		
		if(message.equals("!notifyoff"))
		{
			if(!checkOp(channel,sender))
				return;
			
			DotaBotMain.setNotifyAll(false);
			output(channel, "Stream notifications are now off.");
			saveAll(CHANNEL);
		}
		
		if(message.equals("!update"))
		{
			if(!checkOp(channel,sender))
				return;
			
			DotaBotMain.updateAllStreams();
			output(channel, "Updated all streams successfully.");
			saveAll(CHANNEL);
		}
		
		if(message.startsWith("!db "))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			String[] args = message.split(" ", 3);
			
			if(args.length != 3)
				return;
			
			
			String match = args[1];
			String player = args[2];
			
			output(channel, getMatchDetails(player, match));
		}
		
		if(message.equalsIgnoreCase("!matches") || message.equalsIgnoreCase("!m"))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			//String matches = getMatches();
			//String matches = getJoinDotaMatches();
			
			List<Match> matches = getJoinDotaMatches();
			Collections.sort(matches);
			
			String output = "";
			for(Match m : matches) {
				
				long countdown = m.getTimestamp();
				Date date = new Date(countdown);

				DateFormat formatterHour = new SimpleDateFormat("HH");
				DateFormat formatterMin = new SimpleDateFormat("mm");

				String formatTime;
				if(countdown < 0)
					formatTime = "LIVE";
				else
					formatTime = (countdown / (24*3600*1000))+"d"+formatterHour.format(date)+"h"+formatterMin.format(date)+"m";

				output = output + (output.equals("")?"":" | ")+formatTime+" \u0002"+m.getTeam1().replaceAll(" ","")+" vs. "+m.getTeam2().replaceAll(" ","")+"\u000F "+m.getTournament();
				System.out.println("end of array");
			}
			
			if(!matches.isEmpty())
				output(channel, output);
			else
				output(channel, "Could not get matches from joinDota.com");
		}
		
		if(message.equalsIgnoreCase("!streams") || message.equalsIgnoreCase("!s"))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			List<Stream> list = DotaBotMain.getStreams(false);
			List<Stream> online = new ArrayList<Stream>();
			
			String output = "";
			//String output = "Current Live Streams: ";
			
			for(Stream s : list)
				if(s.isOnline())
					online.add(s);
			
			if(online.isEmpty())
			{
				output(channel, "No streams currently online.");
				return;
			}
			
			Iterator<Stream> i = online.iterator();
			while(i.hasNext())
			{
				Stream s = i.next();

				if(s.getType().equalsIgnoreCase("twitch"))
					output += Colors.UNDERLINE+"www.twitch.tv/"+s.getChannel()+Colors.NORMAL+" ("+s.getViewers()+")";
				else
					output += Colors.UNDERLINE+s.getName()+Colors.NORMAL+" ("+s.getViewers()+") "+ s.getUrl();
				
				if(i.hasNext())
					output += " | ";
			}
			
			output(channel, output);
		}
		
		if(message.equals("!listall"))
		{
			if(!checkOp(channel,sender))
				return;
			
			List<Stream> list = DotaBotMain.getStreams(false);
			String output = "";
			int counter = 0;
			int max = 7;
			
			Iterator<Stream> i = list.iterator();
			while(i.hasNext())
			{

				Stream s = i.next();
				output += "\u0002"+s.getName()+"\u000F "+s.getChannel()+" "+s.getType();
				
				counter++;
				if(counter >= max)
				{
					counter = 0;
					sendNotice(sender, output);
					output = "";
					continue;
					
				}
				
				if(i.hasNext())
					output += " | ";
			}
			
			if(!output.isEmpty())
				sendNotice(sender, output);
		}
		
		if(message.equals("!listimportant"))
		{
			if(!checkOp(channel,sender))
				return;
			
			List<Stream> list = DotaBotMain.getStreams(true);
			String output = "";
			int counter = 0;
			int max = 7;
			
			Iterator<Stream> i = list.iterator();
			while(i.hasNext())
			{

				Stream s = i.next();
				output += "\u0002"+s.getName()+"\u000F "+s.getChannel()+" "+s.getType();
				
				counter++;
				if(counter >= max)
				{
					counter = 0;
					sendNotice(sender, output);
					output = "";
					continue;
					
				}
				
				if(i.hasNext())
					output += " | ";
			}
			
			if(!output.isEmpty())
				sendNotice(sender, output);
		}
		
		if(message.startsWith("!idea "))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			String[] args = message.split("!idea");
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			String output = dateFormat.format(date)+", User: "+sender+", Idea: ";
			
			for(String s : args)
				output += s;
			
			output += "\n";
			if(DotaBotMain.addIdea(output))
				output(channel,"Idea added successfully, thanks for submitting.");
			else
				output(channel,"Error writing idea.");
			saveAll(CHANNEL);
		}
		
		if(message.startsWith("!add "))
		{
			if(!checkOp(channel,sender))
				return;
			
			String[] args = message.split(" ");
			
			if(args.length != 4)
				return;
			
			String streamName = args[1];
			String streamChannel = args[2];
			String streamType = args[3];
			
			if(DotaBotMain.addStream(streamName, streamChannel, streamType))
				output(channel, "Added the stream: "+streamName+", with channel: "+streamChannel+" and of type "+streamType+" successfully.");
			
			saveAll(CHANNEL);
		}
		
		if(message.startsWith("!iadd "))
		{
			if(!checkOp(channel,sender))
				return;
			
			String[] args = message.split(" ");
			
			if(args.length != 2)
				return;
			
			String streamChannel = args[1];
			
			if(DotaBotMain.addImportant(streamChannel))
				output(channel, "Added the stream "+streamChannel+" to the notification list.");

			saveAll(CHANNEL);
		}
		
		if(message.startsWith("!remove "))
		{
			if(!checkOp(channel,sender))
				return;
			
			String[] args = message.split(" ");
			
			if(args.length != 2)
				return;
			
			String streamChannel = args[1];
			
			if(DotaBotMain.removeStream(streamChannel))
				output(channel, "Stream "+streamChannel+" removed successfully.");
			
			saveAll(CHANNEL);
		}
			
		if(message.startsWith("!iremove "))
		{
			if(!checkOp(channel,sender))
				return;
			
			String[] args = message.split(" ");
			
			if(args.length != 2)
				return;
			
			String streamChannel = args[1];
			
			if(DotaBotMain.removeImportant(streamChannel))
				output(channel, "Stream "+streamChannel+" removed successfully from the notification list.");
			
			saveAll(CHANNEL);
		}
		
		if(message.equals("!die"))
		{
			if(!checkOp(channel,sender))
				return;
			
			
			saveAll(CHANNEL);			
			try {
			Thread.sleep(2000);
			}
			catch(Exception ex)
			{
				output(channel, "I'm not sleepy!");
				ex.printStackTrace();
			}
			
			this.disconnect();
			System.exit(0);
		}
		
		// R.I.P dotabuff
		// Nevermind dotabuff is back :)
		if(message.startsWith("!dotabuff "))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			String[] split = message.split(" ", 2);
			if(split.length != 2)
				return;
			
			String name = split[1];
			
			String url = "http://dotabuff.com/players/"+name;
			
			try {
			Document doc = Jsoup.connect(url).get();
			String text = doc.body().html();
			System.out.println(text);
			
			String playername = doc.select("div.content-header-title").text();
			int won = Integer.parseInt(doc.select("span.won").text());
			int lost = Integer.parseInt(doc.select("span.lost").text());
	
			double winrate = (double)won / ((double)(won + lost));
			winrate = round(winrate*100, 2);
			
			output(channel, "Calculating stats from dotabuff.com ...");
			output(channel, playername+": Won="+won+", Lost="+lost+", Winrate="+winrate+"%");
			}
			catch (Exception e) {
				e.printStackTrace();
				output(channel, "Error connecting to dotabuff.");
			}
		}
		
		if(message.equals("!help"))
		{
			if(getCurrentTime() < time + STREAMDELAY)
				return;
			
			time = getCurrentTime();
			
			sendNotice(sender, "Commands: !streams, !matches, !idea [idea/comment/bug/thanks/etc], !db [matchid] [name]");
			
			if(checkOp(channel, sender))
			{
				sendNotice(sender, "Admin Commands: !die, !save, !listall, !add [name] [channel] [type], !remove [channel], !iadd [channel], iremove [channel], !listimportant, !notify[on/off]");
			}
		}
	}
}