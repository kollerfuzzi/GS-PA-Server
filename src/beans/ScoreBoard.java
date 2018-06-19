/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author alex
 */
public class ScoreBoard implements Serializable {

    private Map<String, SinglePlayerData> playerMap = new TreeMap<>();
    public String[] columnNames = {"Users",
        "Kills",
        "Deaths",
        "K/D",
        "Ping"};

    public void playerKill(String playerID) {
        System.out.println("playerKil playerID!!!!!!!!!!!!!!!!!" + playerID);
        playerMap.get(playerID).setKills(playerMap.get(playerID).getKills() + 1);
    }

    public void playerDeath(String playerID, String killerID) {
       System.out.println("map: " + playerMap);
       System.out.println("playerID: " + playerID + "; killerID: " + killerID);
       System.out.println("PlayerID= " + playerID);
        playerMap.get(playerID).setDeaths(playerMap.get(playerID).getDeaths() + 1);
        playerKill(killerID);

    }

    public void playerDeathHimselfe(String playerID) {
        playerMap.get(playerID).setDeaths(playerMap.get(playerID).getDeaths() + 1);
    }

    public void playerJoin(String PlayerID) {
        playerMap.put(PlayerID, new SinglePlayerData(0, 0, -1, LocalDateTime.now()));
    }

    public void playerShotBy(String victomID, String killerID) {
        synchronized (playerMap) {
            System.out.println("playerShotBy called: victomID:" + victomID + " killerID:" + killerID);
            if (!victomID.equals("noone") && checkIfPlayerExists(killerID) && checkIfPlayerExists(victomID)) {
                playerMap.get(victomID).setLastShotPlayerID(killerID);
                System.out.println("read playerMap(of"+victomID+":" + playerMap.get(victomID).lastShotPlayerID);
            }else{
                System.out.println("writing the new killer failed!!!!!!!");
            }
        }
    }

    public boolean checkIfPlayerExists(String PlayerID) {
        return playerMap.containsKey(PlayerID);
    }

    public void playerDisconnect(String PlayerID) {
        if (playerMap.containsKey(PlayerID)) {
            playerMap.remove(PlayerID);
        }
        System.out.println("Player not in scoreBoard any more.");
    }

    public String getKillerof(String PlayerID) {
        if(checkIfPlayerExists(playerMap.get(PlayerID).getLastShotPlayerID())){
            return playerMap.get(PlayerID).getLastShotPlayerID();
        }
        return null;
    }

    public void updatePing(PingPacket pingPackerl) {
        playerMap.get(pingPackerl.getPlayerID()).setPing(pingPackerl.getPingValue());
    }

    public int getAnzahlOfPlayers() {
        return playerMap.size();
    }

    /**
     * Object with the data for the JTable oder was auch immer du damit machen
     * willst.
     *
     * @return Object[][]
     */
    public Object[][] getScoreBoardData() {

        String[][] data = new String[playerMap.size()][5];
        int i = 0;
        for (String keystr : playerMap.keySet()) {
            SinglePlayerData spd = playerMap.get(keystr);

            data[i][0] = keystr;
            data[i][1] = spd.getKills() + "";
            data[i][2] = spd.getDeaths() + "";
            data[i][3] = ((double) spd.getKills()) / ((double) spd.getDeaths()) + "";
            data[i][4] = spd.getPing() + "";
            i++;
        }

        return data;
    }

    private class SinglePlayerData {

        //private String PlayerID; //username
        private int kills;
        private int deaths;
        private double ping; //-1 = kein ping
        private LocalDateTime join;
        private String lastShotPlayerID;

        public SinglePlayerData(int kills, int deaths, double ping, LocalDateTime join) {
            this.kills = kills;
            this.deaths = deaths;
            this.ping = ping;
            this.join = join;
            this.lastShotPlayerID = null;
        }

        public String getLastShotPlayerID() {
            return lastShotPlayerID;
        }

        public void setLastShotPlayerID(String lastShotPlayerID) {
            this.lastShotPlayerID = lastShotPlayerID;
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
