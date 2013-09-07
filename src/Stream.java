import static com.rosaloves.bitlyj.Bitly.as;
import static com.rosaloves.bitlyj.Bitly.shorten;

import com.rosaloves.bitlyj.Url;


public class Stream implements java.io.Serializable, Comparable<Stream> {
	@Override
	public String toString() {
		return "Stream [name=" + name + ", channel=" + channel + ", type="
				+ type + ", viewers=" + viewers + ", isOnline=" + isOnline
				+ "]";
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -779563290570849807L;
	
	private String name;
	private String channel;
	private String type;
	private int viewers;
	private boolean isOnline;
	private String url;
	
	public Stream(String name, String channel, String type) {
		this.name = name;
		this.channel = channel;
		this.viewers = 0;
		this.type = type;
		isOnline = false;
		
		if(type.equals("twitch"))
			this.url = getFixUrl("http://www.justin.tv/"+channel);
		
		// Need to add own3d support
		if(type.equals("own3d"))
			this.url = getFixUrl("http://www.own3d.tv/live/"+channel);
	}
			
	private String getFixUrl(String url)
	{		
		if(url.isEmpty())
			return "";
		
		Url nurl = as("zaknafein", "R_57db038e061b13e952fec891910f82d5").call(shorten(url));			
		return nurl.getShortUrl();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public int getViewers() {
		return viewers;
	}

	public void setViewers(int viewers) {
		this.viewers = viewers;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean equals(Stream o)
	{
		return this.getChannel().equals(o.getChannel());
	}

	@Override
	public int compareTo(Stream o) {
		if(getViewers() > o.getViewers()) return -1;
		if(getViewers() < o.getViewers()) return 1;
		return 0;
	}
	
}
