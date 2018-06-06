package mygame;

import beans.PlayerData;
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

public abstract class ServerMain {

    private static final int FINALPORT = 80;
    private static final int LOCALHOSTPORT = 8089;
    private static boolean USELOCALPORT = true; //if true, port for local use; if false it uses the final port (port of the server)
    private final int PORTNR;
    private JTextComponent taLog;
    private ServerThread st;
    private String chat = "";
    private ArrayList<ClientCommunicationThread> cliComList = new ArrayList<ClientCommunicationThread>();
    private ArrayList<String> teams = new ArrayList<>(Arrays.asList("4AHIF", "4BHIF", "Kolle Kidz"));

    public ServerMain(int portnr) {
        this.PORTNR = portnr;
    }

    public static void main(String[] args) {
        ServerMain obj = new ServerMain() {
            @Override
            protected Object performRequest(Object request) {
                return request;
            }
        };
        obj.startServer();
    }

    public ServerMain(int PORTNR, JTextComponent taLog) {
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
            //System.out.println(message);
        }
    }

    public void startServer() {
        if (st == null || !st.isAlive()) {

            try {
                st = new ServerThread();
                st.start();
            } catch (IOException ex) {
                st = null;
                log("Starting server failed");
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
        log("Socket is closed!!!!!!!!!!!!");
        if (st != null && st.isAlive()) {
            st.interrupt();
        }

    }

    class ServerThread extends Thread {

        private final ServerSocket serverSocket;

        public ServerThread() throws IOException {
            serverSocket = new ServerSocket(PORTNR);
            log("Server started on port: " + PORTNR);
            //serverSocket.setSoTimeout(250);
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    log("Connected to client: " + socket.getRemoteSocketAddress());

                    synchronized (cliComList) {
                        cliComList.add(new ServerMain.ClientCommunicationThread(socket));
                    }

                } catch (SocketTimeoutException e) {
                    //log("timeout");
                } catch (IOException e) {
                    log("Connection failed");
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Server could not be closed!");
            }
            log("Server shutdown");
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
                    log("Server receaved: " + request);
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
                log("Client '" + socket.getRemoteSocketAddress() + "' closed connection to Server!");
            } catch (IOException | ClassNotFoundException e) {
                log("Error in ClientCommunicationThread " + e.toString());
                //System.exit(0);
            }
            //log("request finished " + socket.getRemoteSocketAddress());

            synchronized (cliComList) {
                cliComList.remove(this);
                log("Client removed from Client list");
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

}
