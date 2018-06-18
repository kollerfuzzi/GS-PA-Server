/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author alex
 */
public class ScoreBoard {

    private Map<String, SinglePlayerData> playerMap = new TreeMap<>();
    public String[] columnNames = {"Users",
        "Kills",
        "Deaths",
        "K/D",
        "Ping"};

    
    public void playerKill(String playerID){
        playerMap.get(playerID).setKills(playerMap.get(playerID).getKills()+1);
    }
    
    public void playerDeath(String playerID){
        playerMap.get(playerID).setDeaths(playerMap.get(playerID).getDeaths()+1);
    }
    
    public void playerJoin(String PlayerID) {
        playerMap.put(PlayerID, new SinglePlayerData(0, 0, -1, LocalDateTime.now()));
    }

    public boolean checkIfPlayerExists(String PlayerID) {
        return playerMap.containsKey(PlayerID);
    }

    public void playerDisconnect(String PlayerID) {
        if(playerMap.containsKey(PlayerID)){
            playerMap.remove(PlayerID);
        }
        System.out.println("Player not in scoreBoard any more.");
    }

    public void updatePing(PingPacket pingPackerl) {
        playerMap.get(pingPackerl.getPlayerID()).setPing(pingPackerl.getPingValue());
    }
    /**
     * Object with the data for the JTable oder was auch immer du damit machen willst.
     * @return Object[][] 
     */
    public Object[][] getScoreBoardData(){
        
        Object[][] data = new Object[5][playerMap.size()];
        int i = 0;
        for (String keystr : playerMap.keySet()) {
            SinglePlayerData spd = playerMap.get(keystr);
            
            data[1][i] = keystr;
            data[2][i] = spd.getKills();
            data[3][i] = spd.getDeaths();
            data[4][i] = ((double)spd.getKills())/((double)spd.getDeaths());
            data[5][i] = spd.getPing();
       
        }
        
        return data;
    }

    private class SinglePlayerData {

        //private String PlayerID; //username
        private int kills;
        private int deaths;
        private double ping; //-1 = kein ping
        private LocalDateTime join;

        public SinglePlayerData(int kills, int deaths, double ping, LocalDateTime join) {
            this.kills = kills;
            this.deaths = deaths;
            this.ping = ping;
            this.join = join;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }

        public double getPing() {
            return ping;
        }

        public void setPing(double ping) {
            this.ping = ping;
        }

        public LocalDateTime getJoin() {
            return join;
        }

        public void setJoin(LocalDateTime join) {
            this.join = join;
        }

    }
}
