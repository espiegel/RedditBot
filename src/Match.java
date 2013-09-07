
public class Match implements Comparable<Match>{

	private String team1;
	private String team2;
	private String tournament;
	private long timestamp;
	
	public Match(String team1, String team2, String tournament, long timestamp) {
		this.team1 = team1;
		this.team2 = team2;
		this.tournament = tournament;
		this.timestamp = timestamp;
	}
	
	public String getTeam1() {
		return team1;
	}
	public void setTeam1(String team1) {
		this.team1 = team1;
	}
	public String getTeam2() {
		return team2;
	}
	public void setTeam2(String team2) {
		this.team2 = team2;
	}
	public String getTournament() {
		return tournament;
	}
	public void setTournament(String tournament) {
		this.tournament = tournament;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(Match o) {
		// TODO Auto-generated method stub
		return (((this.timestamp - o.timestamp) > 0) ? 1 : -1);
	}
}
