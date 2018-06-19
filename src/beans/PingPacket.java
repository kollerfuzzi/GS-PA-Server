/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import java.io.Serializable;

/**
 *
 * @author alex
 */
public class PingPacket implements Serializable{
    private String PlayerID;
    private double pingValue;

    public String getPlayerID() {
        return PlayerID;
    }

    public void setPlayerID(String PlayerID) {
        this.PlayerID = PlayerID;
    }

    public double getPingValue() {
        return pingValue;
    }

    public void setPingValue(double pingValue) {
        this.pingValue = pingValue;
    }

    public PingPacket(String PlayerID, double pingValue) {
        this.PlayerID = PlayerID;
        this.pingValue = pingValue;
    }
    
    
    
}
