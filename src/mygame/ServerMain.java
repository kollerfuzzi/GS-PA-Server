package mygame;

import beans.Damage;
import beans.PingPacket;
import beans.PlayerData;
import beans.PlayerStatus;
import beans.RoundInfo;
import beans.RoundTime;
import beans.ScoreBoard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.text.JTextComponent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

public abstract class ServerMain {

    private boolean serverIsRunning = false;
    private static boolean CLI = false;  //if true, server starts with commandline interface.
    private static final int FINALPORT = 80;
    private static final int LOCALHOSTPORT = 8089;
    private static boolean USELOCALPORT = true; //if true, port for local use; if false it uses the final port (port of the server)
    private final int PORTNR;
    private JScrollPane jsp;
    private JTextArea taLog;
    private ServerThread st;
    private ArrayList<ClientCommunicationThread> cliComList = new ArrayList<ClientCommunicationThread>();
    private ArrayList<String> teams = new ArrayList<>(Arrays.asList("4AHIF", "4BHIF", "Kolle Kidz"));
    private JFrame serverFrame;
    private JButton btStartStopServer;
    private JLabel lbTimeRunning;
    private JPanel jpInput;
    private ScoreBoard scoreBoard; //this class 
    private RoundInfo activeRound = null;
    private final int SECONDSTILLNEWROUND = 15;
    private RoundThread roundThread = null;

    /**
     * Method to generate a GUI server interface
     */
    private void initComponents() {
        serverFrame = new JFrame("GSPA - Server");
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setSize(new Dimension(500, 300));
        serverFrame.setLocationRelativeTo(null);
        serverFrame.setVisible(true);
        taLog = new JTextArea();
        scoreBoard = new ScoreBoard();

        serverFrame.setLayout(new BorderLayout());
        jsp = new JScrollPane(taLog);

        serverFrame.add(jsp, BorderLayout.CENTER);

        btStartStopServer = new JButton("startServer");
        btStartStopServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (serverIsRunning == false) {
                    startServer();
                } else {
                    stopServer();
                }
            }
        });

        lbTimeRunning = new JLabel("0");
        jpInput = new JPanel(new GridLayout(1, 2));
        jpInput.add(btStartStopServer);
        jpInput.add(lbTimeRunning);

        Thread t1 = new UpdateBtn();
        t1.start();

        serverFrame.add(jpInput, BorderLayout.SOUTH);

    }
    
    public static void main(String[] args) {
        ServerMain obj = new ServerMain() {
            @Override
            protected Object performRequest(Object request) {
                return request;
            }
        };
        if (CLI == true) {
            obj.startServer();
        } else {
            obj.initComponents();
        }
    }

    public ServerMain(int portnr) {
        this.PORTNR = portnr;
    }

    public ServerMain(int PORTNR, JTextArea taLog) {
        this(PORTNR);
        this.taLog = taLog;
    }

    public ServerMain() {
        this(USELOCALPORT ? LOCALHOSTPORT : FINALPORT);
    }

    /**
     * log methode for output on the taLog if it is not null
     * @param message 
     */
    protected void log(String message) {
        if (taLog != null) {
            taLog.setText(taLog.getText() + "\n" + message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * starts the server
     * you can check if the server is running with the boolean "serverIsRunning"
     */
    public void startServer() {

        if (st != null) {
            st = null;
            log("server was set to null");
        }

        log("starting server initiated");
        if (st == null || !st.isAlive()) {

            try {
                st = new ServerThread();
                st.start();
            } catch (IOException ex) {
                ex.printStackTrace();
                st = null;
                log("Starting server failed #C01");
                serverIsRunning = false;
            }
        }
    }
    /**
     * stopps the server
     * you can check if the server is running with the boolean "serverIsRunning"
     */
    public void stopServer() {
        if (st != null && st.isAlive()) {
            st.interrupt();
        }
        log("Socket is closed #C02");
        serverIsRunning = false;

    }

    /**
     * It actially makes a multicast to every player except the player wich sent the object.
     * If sender is null it sends a brodcast.
     * @param obj
     * @param sender
     * @throws IOException 
     */
    private void broadcastObj(Object obj, ClientCommunicationThread sender) throws IOException {
        synchronized (cliComList) {
            for (ClientCommunicationThread cct : cliComList) {
                if (sender != cct) {
                    try {
                        cct.oos.writeObject(obj);
                        //log("Sent to: " + cct.getPlayer().getPlayerID() + " from: " + sender.getPlayer().getPlayerID() + " data: " + obj);
                    } catch (NullPointerException e) {

                    }
                }
            }
        }
    }
    /**
     * Unicast of an object to one client.
     * @param obj
     * @param sender
     * @throws IOException 
     */
    private void unicasttObj(Object obj, ClientCommunicationThread sender) throws IOException {
        sender.oos.writeObject(obj);
    }
    
    /**
     * Starts a new round.
     * @throws IOException 
     */
    protected void initiateNewRound() throws IOException {
            log("initiating new round");
            activeRound = new RoundInfo(LocalDateTime.now().plusSeconds(SECONDSTILLNEWROUND));
            broadcastObj(activeRound, null);
            roundThread = null;
            roundThread = new RoundThread();
            roundThread.start();
            log("new round started");
        }

    /**
     * The ServerThread is listening for new players.
     * If a player joines he game, he gets added to the cliComList (a list with the ClientCommunicationThreads)
     */
    class ServerThread extends Thread {

        private final ServerSocket serverSocket;

        public ServerThread() throws IOException {
            serverSocket = new ServerSocket(PORTNR);
            log("Server started on port: " + PORTNR + "#C03");
            serverIsRunning = true;
            //serverSocket.setSoTimeout(250);
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    log("Connected to client: " + socket.getRemoteSocketAddress() + "#C04");

                    synchronized (cliComList) {
                        cliComList.add(new ServerMain.ClientCommunicationThread(socket));
                    }

                } catch (SocketTimeoutException e) {
                    //log("timeout#C05");
                } catch (IOException e) {
                    log("Connection failed#C06");
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Server could not be closed!#C07");
            }
            log("Server shutdown#C08");
            serverIsRunning = false;
        }
    }
    /**
     * The ClientCommunicationThread keeps an steady connection with the player till the player disconnects from the game.
     */
    class ClientCommunicationThread extends Thread {

        private Socket socket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private PlayerData player;

        public ClientCommunicationThread(Socket socket) throws SocketException {
            this.socket = socket;
            //this.socket.setSoTimeout(1000);
            start();

        }

        @Override
        public void run() {

            try {
                ois = new ObjectInputStream(socket.getInputStream());
                oos = new ObjectOutputStream(socket.getOutputStream());

                Object request = null;
                while (!(request = ois.readObject()).toString().toLowerCase().equals("exit")) {       //check what kind of request the client sends.
                    //log("Server receaved: " + request + "#09");
                    if (request instanceof PlayerStatus) {
                        handlePlayerStatusRequest(request);
                    } else if (request.toString().toLowerCase().equals("gimmiscoreboardnowwwww")) {
                        unicasttObj(scoreBoard.getScoreBoardData(), this);
                        //log("sent scoreboard ");
                    } else if (request.toString().toLowerCase().equals("gimmiping")) {
                        unicasttObj(request, this);
                    } else if (request instanceof PlayerData) {
                        handlePlayerDataRequest(request);

                    } else if (request instanceof String) {
                        handleStringRequest(request);
                    } else if (request instanceof Damage) {
                        Damage damage = (Damage) request;
                        damage.getPlayer();
                       // System.out.println("damage.getPlayer "+damage.getPlayer() + " player.getPlayerID "+ player.getPlayerID());
                        scoreBoard.playerShotBy(damage.getPlayer(), player.getPlayerID());
                        broadcastObj(request, this);
                    } else if (request.toString().toLowerCase().equals("brodcast")) {
                        unicasttObj(request, this);
                    } else if (request instanceof PingPacket) {
                        PingPacket pipa = (PingPacket) request;
                        scoreBoard.updatePing(pipa);
                    } else {
                        broadcastObj(request, this);
                    }
                }

            } catch (java.io.EOFException eofe) {
                log("Client '" + socket.getRemoteSocketAddress() + "' closed connection to Server!#C10");
            } catch (IOException | ClassNotFoundException e) {
                log("Error in ClientCommunicationThread " + e.toString() + "#C11");
                //System.exit(0);
            }
            //log("request finished " + socket.getRemoteSocketAddress() + "#C12");

            synchronized (cliComList) {
                cliComList.remove(this);
                if (player != null) {
                    scoreBoard.playerDisconnect(player.getPlayerID());       //player gets thrown out of the scoreBoard
                }
                log("Client removed from Client list#C13");
            }

        }

        public PlayerData getPlayer() {
            return player;
        }

        public void setPlayer(PlayerData player) {
            this.player = player;
        }

        

        private void handlePlayerStatusRequest(Object request) throws IOException { //handels ingoing playerstatus (like logged_in, disconnected, dead...
            log("PlayerStatusRequest: " + request.toString() + "C14");
            PlayerStatus ps = (PlayerStatus) request;
            broadcastObj(request, this);
            switch (ps.getType()) {
                case LOGGED_IN:
                    scoreBoard.playerJoin(ps.getPlayerID());
                    broadcastObj(ps.getPlayerID() + " has joined the game", null);
                    if (scoreBoard.getAnzahlOfPlayers() >= 2 && (activeRound == null)) {
                        initiateNewRound();
                    } else {
                        log("getAnzahlOfPlayers=" + scoreBoard.getAnzahlOfPlayers());
                        log("activeRound=" + activeRound);
                        log("too less players for round!");
                    }

                    //thread der die sekunden übrig raus schreibt und abbricht wenn die runde zu ende ist.
                    break;
                case DISCONNECTED:
                    scoreBoard.playerDisconnect(ps.getPlayerID());
                    for (ClientCommunicationThread clientCommunicationThread : cliComList) {
                        if (clientCommunicationThread.getPlayer().getPlayerID().equals(ps.getPlayerID())) {
                            clientCommunicationThread.interrupt();
                        }
                    }
                    broadcastObj(ps.getPlayerID() + " has left the game", null);
                    break;
                case DEAD:
                    
                    System.out.println("killerof " + ps.getPlayerID() + " is " + scoreBoard.getKillerof(ps.getPlayerID()));
                    if(scoreBoard.getKillerof(ps.getPlayerID()) != null){
                        scoreBoard.playerDeath(ps.getPlayerID(), scoreBoard.getKillerof(ps.getPlayerID()));
                        broadcastObj(scoreBoard.getKillerof(ps.getPlayerID()) + " killed " + ps.getPlayerID(), null);
                    }else{
                        scoreBoard.playerDeathHimselfe(ps.getPlayerID());
                        broadcastObj(ps.getPlayerID() + " committed suicide", null);
                    }
                    break;
                case KILLEDHIMSELF:
                    scoreBoard.playerDeathHimselfe(ps.getPlayerID());
                    broadcastObj(ps.getPlayerID() + " committed suicide", null);
                    break;
                default:
                    log("unhandeled StatusRequest " + request);
                    break;
            }

        }

        private void handlePlayerDataRequest(Object request) throws IOException {  //playerdata is the current position of the player. 
            player = (PlayerData) request;                                      //The client sends its own playerdata and the server sends it to the other players.
            //System.out.println("PlayerData received: " + player.getPlayerID() + " at " + player.getPosition());
            broadcastObj(request, this);
        }

        private void handleStringRequest(Object request) throws IOException { //was intendet to handle plain strings but is not used. Teams could easily be added in the future. 
            switch ((String) request) {
                case "TEAMS":
                    oos.writeObject(teams);
                    System.out.println(teams);
                    System.out.println("TEAMS SENT!!!");
                    break;
                default:
                    oos.writeObject("what?");
            }
        }
    }

    protected abstract Object performRequest(Object request);

    /**
     * updates the serverGUI start/stopp button
     */
    public class UpdateBtn extends Thread {

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    if (serverIsRunning == true) {
                        btStartStopServer.setText("Stop server.");
                        btStartStopServer.setBackground(Color.green);
                        if (lbTimeRunning != null) {
                            lbTimeRunning.setText((1 + Integer.parseInt(lbTimeRunning.getText())) + "");
                        }
                    } else {
                        btStartStopServer.setText("Start server.");
                        btStartStopServer.setBackground(Color.red);
                        if (lbTimeRunning != null) {
                            lbTimeRunning.setText("0");
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    
    /**
     * Thread to handles rounds. 
     * The duration of those, when the next starts and so on.
     */
    private class RoundThread extends Thread {

        private boolean roundHasStarted = false;

        @Override
        public void run() {

            while (!interrupted()) {
                try{
                if (LocalDateTime.now().isBefore(activeRound.getPredictedStarttime())) {
                    try {
                        roundHasStarted = false;
                        long durationInSeconds = Duration.between(LocalDateTime.now(), activeRound.getPredictedStarttime()).toMillis() / (long) 1000;
                        broadcastObj(new RoundTime((int) durationInSeconds, RoundTime.Type.NOTHING), null);

                    } catch (IOException ex) {
                        log("Sending RoundTime failed!");
                    }
                } else if (roundHasStarted == false) {
                    //there you should tell the client that the round has started, but we do not do that at this time.
                    try {
                        roundHasStarted = true;
                        long durationInSeconds = Duration.between(LocalDateTime.now(), activeRound.getPredictedStarttime()).toMillis() / (long) 1000;
                        broadcastObj(new RoundTime((int) durationInSeconds, RoundTime.Type.STARTED), null);
                        log("send RoundTime: Started");

                    } catch (IOException ex) {
                        log("Sending RoundTime failed!");
                    }
                } else if (roundHasStarted == true && LocalDateTime.now().isAfter(activeRound.getPredictedStarttime().plusSeconds(activeRound.getRoundlength()))) {
                    try {
                        long durationInSeconds = Duration.between(LocalDateTime.now(), activeRound.getPredictedStarttime().plusSeconds(activeRound.getRoundlength())).toMillis() / (long) 1000;
                        broadcastObj(new RoundTime((int) durationInSeconds, RoundTime.Type.STOPPED), null);
                        log("send RoundTime: Stopped");
                        this.interrupt();
                        log("interruption was triggered");

                    } catch (IOException ex) {
                        log("Sending RoundTime failed!");
                    }catch(java.lang.Error error){
                        log("java long ERROR");
                    }
                } else {
                    try {
                        long durationInSeconds = Duration.between(LocalDateTime.now(), activeRound.getPredictedStarttime().plusSeconds(activeRound.getRoundlength())).toMillis() / (long) 1000;
                        broadcastObj(new RoundTime((int) durationInSeconds, RoundTime.Type.NOTHING), null);
                    } catch (IOException ex) {
                        log("Sending RoundTime failed!");
                    }
                }
                }catch(java.lang.NullPointerException npe){
                    System.out.println("npe");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            activeRound = null;
            //log("active round set to null!");
                Timer timer = new Timer(2000, new ActionListener() {
                    boolean alreadyTriggert = false;
                @Override
                public void actionPerformed(ActionEvent e) { //This is a Timer to start a new round a couple seconds after this Object gets destroyed.
                    try {
                        if(!alreadyTriggert){
                            initiateNewRound();
                            alreadyTriggert = true;
                        }
                    } catch (IOException ex) {
                        log("initiateNewRound failed!");
                    }
                }
            });        
            timer.start();
        }

    }

}
