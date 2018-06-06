package mygame;

import beans.PlayerData;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public abstract class ServerMain {

    private boolean serverIsRunning = false;
    private static boolean CLI = false;  //if true, server starts with commandline interface.
    private static final int FINALPORT = 80;
    private static final int LOCALHOSTPORT = 8089;
    private static boolean USELOCALPORT = true; //if true, port for local use; if false it uses the final port (port of the server)
    private final int PORTNR;
    private JTextArea taLog;
    private ServerThread st;
    private ArrayList<ClientCommunicationThread> cliComList = new ArrayList<ClientCommunicationThread>();
    private ArrayList<String> teams = new ArrayList<>(Arrays.asList("4AHIF", "4BHIF", "Kolle Kidz"));
    private JFrame serverFrame;
    private JButton btStartStopServer;
    private JLabel lbTimeRunning;
    private JPanel jpInput;
    
    private void initComponents(){
        serverFrame = new JFrame("GSPA - Server");
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setSize(new Dimension(500, 300));
        serverFrame.setLocationRelativeTo(null);
        serverFrame.setVisible(true);
        taLog = new JTextArea();
        
        serverFrame.setLayout(new BorderLayout());
        serverFrame.add(taLog, BorderLayout.CENTER);
        
        btStartStopServer = new JButton("startServer");
        btStartStopServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(serverIsRunning==false){
                    startServer();
                }else{
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
        if(CLI == true){
            obj.startServer();
        }else{
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

    protected void log(String message) {
        if (taLog != null) {
            taLog.setText(taLog.getText() + "\n" + message);
        } else {
            System.out.println(message);
        }
    }

    public void startServer() {
        if (st == null || !st.isAlive()) {

            try {
                st = new ServerThread();
                st.start();
            } catch (IOException ex) {
                st = null;
                log("Starting server failed #C01");
                serverIsRunning = false;
            }
        }
    }

    private void broadcastObj(Object obj, ClientCommunicationThread sender) throws IOException {
        synchronized (cliComList) {
            for (ClientCommunicationThread cct : cliComList) {
                if (sender != cct) {
                    try {
                        cct.oos.writeObject(obj);
                        System.out.println("Sent to: " + cct.getPlayer().getPlayerID()+ " from: " + sender.getPlayer().getPlayerID()+ " data: " + obj);
                    } catch (NullPointerException e ) {
                       
                    }
                }
            }
        }
    }

    public void stopServer() {
        if (st != null && st.isAlive()) {
            st.interrupt();
        }
        log("Socket is closed #C02");
        serverIsRunning = false;
        
    }

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
                while (!(request = ois.readObject()).toString().toLowerCase().equals("exit")) {
                    log("Server receaved: " + request + "#09");
                    if (request instanceof PlayerData) {
                        player = (PlayerData) request;
                        //System.out.println("PlayerData received: " + player.getPlayerID() + " at " + player.getPosition());
                        broadcastObj(request, this);
                    } else if (request instanceof String) {
                        switch((String)request) {
                            case "TEAMS":
                                oos.writeObject(teams);
                                System.out.println(teams);
                                System.out.println("TEAMS SENT!!!");
                                break;
                            default:
                                oos.writeObject("what?");
                        }
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
                log("Client removed from Client list#C13");
            }

            synchronized (cliComList) {
                cliComList.remove(this);
            }
        }

        public PlayerData getPlayer() {
            return player;
        }

        public void setPlayer(PlayerData player) {
            this.player = player;
        }
        
        
    }

    protected abstract Object performRequest(Object request);
    
    public class UpdateBtn extends Thread{

        @Override
        public void run() {
            while(!interrupted()){
                try {
                    if(serverIsRunning == true){
                        btStartStopServer.setText("Stop server.");
                        btStartStopServer.setBackground(Color.green);
                        if(lbTimeRunning != null){
                            lbTimeRunning.setText((1+Integer.parseInt(lbTimeRunning.getText()))+"");
                        }
                    }else{
                        btStartStopServer.setText("Start server.");
                        btStartStopServer.setBackground(Color.red);
                        if(lbTimeRunning != null){
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

}
