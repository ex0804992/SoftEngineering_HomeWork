import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class TCPServer{

    private static ScheduledExecutorService fScheduler = null;
    private static final int NUM_THREADS = 4;
    private ArrayList<ClientService> memberList = null;
    private List<String> serverMsgList = null;
    private ServerSocket serverSocket;
    private int serverPort = 0;
    private boolean serverOn = false;
    private int totalClient = 0;

    public TCPServer(int serverPort){
        this.serverPort = serverPort;
        memberList = new ArrayList<ClientService>();
        serverMsgList = Collections.synchronizedList(new LinkedList<String>());
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void setServerOn(){

        try
        {
            serverSocket = new ServerSocket(serverPort);
            serverOn = true;
            fScheduler.execute(new Controller());
            System.out.println("Server On!");
        }
        catch(IOException ioe)
        {
            System.out.println("Could not create server socket. Quitting.");
            System.exit(-1);
        }

        // Successfully created Server Socket. Now wait for connections.
        while(serverOn)
        {
            try
            {
                // Accept incoming connections.
                Socket clientSocket = serverSocket.accept();
                totalClient++;
                System.out.printf("Client %d connecting.\n", totalClient);

                ClientService cliThread = new ClientService(clientSocket, totalClient);
                memberList.add(cliThread);
                fScheduler.execute(cliThread);

            }
            catch(IOException ioe)
            {
                System.out.println("Exception encountered on accept. Ignoring. Stack Trace :");
                ioe.printStackTrace();
            }

        }

        try
        {
            serverSocket.close();
            System.out.println("Server Stopped");
        }
        catch(Exception ioe)
        {
            System.out.println("Problem stopping server socket");
            System.exit(-1);
        }

    }

//    @Override
//    public void run() {
//        setServerOn();
//    }

    class ClientService implements Runnable{

        private Socket myClientSocket;
        private boolean m_bRunThread = true;
        private int clientID = 0;
        private List<String> msgList = null;

        public ClientService(){
            super();
        }

        public ClientService(Socket s, int id){
            msgList = Collections.synchronizedList(new LinkedList<String>());
            clientID = id;
            myClientSocket = s;

        }

        public void putMsgInQueue(String msg){
            msgList.add(msg);
        }

        public void run(){

            // Print out details of this connection 
            System.out.println("Accepted Client Address - " + myClientSocket.getInetAddress().getHostName());
            BufferedReader in = null;
            PrintWriter out = null;

            try{

                in = new BufferedReader(new InputStreamReader(myClientSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(myClientSocket.getOutputStream()));

                // At this point, we can read for input and reply with appropriate output.


                String initialMsg = "initialization " + String.valueOf(clientID) + "\n";
//                System.out.println(initialMsg);
                out.write(initialMsg);
                out.flush();

                // Run in a loop until m_bRunThread is set to false
                while(m_bRunThread){

                    if(in.ready()){
                        String clientMsg = in.readLine();
                        clientMsg += " "+String.valueOf(clientID);
                        serverMsgList.add(clientMsg);
                    }

                    if(!msgList.isEmpty()){

                        out.write(msgList.remove(0));
                        out.flush();
                    }

                }

            }catch(Exception e){
                e.printStackTrace();
            }finally{
                try{
                    in.close();
                    out.close();
                    myClientSocket.close();
                    fScheduler.shutdown();
                    System.out.println("...Stopped");
                }
                catch(IOException ioe){
                    ioe.printStackTrace();
                }
            }
        }

    }

    /**
     * Class Controller
     *
     * It is in charge of handling event and managing Treasure.
     *
     * **/
     class Controller implements Runnable{

        private ArrayList<Treasure> treasure = null;


        public Controller(){
            treasure = new ArrayList<Treasure>();
            treasure.add(new Treasure("A", 0));
            treasure.add(new Treasure("B", 0));
            treasure.add(new Treasure("C", 0));
        }

        private Treasure findItem(String target){

            for (Treasure item : treasure) {
                if (item.getName().equals(target)) {
                    return item;
                }
            }
            return null;
        }

        private void releaseItem(String target, int client){

            Treasure item = findItem(target);

            if(item.getOwner() == client){
                item.setOwner(0);
//                System.out.println("Client: " + client + "release item " + target);
            }else{
//                System.out.println("Client: " + client + "do not has item " + target);
            }

        }

        private void getItem(String target, int client){

            Treasure item = findItem(target);

            if(item.getOwner() == 0){
                System.out.println("GETITEM: " + target + "  " + client);
                item.setOwner(client);
                sendMsgToWorker("YES " + target + "\n", client);
            }else{
                sendMsgToWorker("NO " + target + "\n", client);
            }

        }

        private void sendMsgToWorker(String msg, int Client){

            for (ClientService worker : memberList) {
                if (worker.clientID == Client) {
                    worker.putMsgInQueue(msg);
                }
            }

        }

        private void printTreasureState(){

            for (Treasure item : treasure) {
                String msg = item.getName() + " ";
                if (item.getOwner() != 0) {
                    msg += "YES " + item.getOwner();
                } else {
                    msg += "NO " + item.getOwner();
                }
                System.out.println(msg);

            }
        }

        @Override
        public void run() {


            final ScheduledFuture<?> soundAlarmFuture = fScheduler.scheduleWithFixedDelay(
                    new Runnable() {
                        @Override
                        public void run() {
                            printTreasureState();
                        }
                    }, 0, 3, TimeUnit.SECONDS
            );

            while(serverOn){

                if(!serverMsgList.isEmpty()){
                    String msg = serverMsgList.remove(0);
                    System.out.println(msg);

                    String command = msg.split(" ")[0];
                    String target = msg.split(" ")[1];
                    int currentClient = Integer.parseInt(msg.split(" ")[2]);

//                    System.out.printf("commmand : %s, target: %s, currentClient: %s\n", command, target, currentClient);

                    if(command.equals("GET")){

                        getItem(target, currentClient);

                    }else if(command.equals("RELEASE")){

                        releaseItem(target, currentClient);

                    }else{

                        System.out.println("Invalid command!!!");

                    }

                }

            }

        }
    }

    class Treasure {

        private String name = null;
        private int owner = 0;

        public Treasure(String name, int owner){
            this.name = name;
            this.owner = owner;
        }

        public String getName(){
            return name;
        }

        public void setOwner(int owner){
            this.owner=owner;
        }

        public int getOwner(){
            return owner;
        }

    }

}

