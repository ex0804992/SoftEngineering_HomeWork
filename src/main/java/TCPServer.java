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


public class TCPServer implements Runnable{

    private static ScheduledExecutorService fScheduler = null;
    private static final int NUM_THREADS = 4;
    private ServerSocket serverSocket;
    private int serverPort = 0;
    private boolean serverOn = false;

    private int totalClient = 0;
    private ArrayList<ClientService> memberList = null;
    //private LinkedList<String> serverMsgQueue = null;
    List<String> serverMsgList = null;

    public TCPServer(int serverPort){
        this.serverPort = serverPort;
        memberList = new ArrayList<ClientService>();
        //serverMsgQueue = new LinkedList<String>();
        serverMsgList = Collections.synchronizedList(new LinkedList<String>());
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void setServerOn(){

        try
        {
            serverSocket = new ServerSocket(serverPort);
            serverOn = true;
            fScheduler.execute(new Controller());
//            new Thread(new Controller()).start();
            System.out.println("Server On!");
        }
        catch(IOException ioe)
        {
            System.out.println("Could not create server socket. Quitting.");
            System.exit(-1);
        }

//        Calendar now = Calendar.getInstance();
//        SimpleDateFormat formatter = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
//        System.out.println("It is now : " + formatter.format(now.getTime()));

        // Successfully created Server Socket. Now wait for connections.
        while(serverOn)
        {
            try
            {
                // Accept incoming connections.
                Socket clientSocket = serverSocket.accept();
                totalClient++;
                System.out.printf("Client %d connecting.\n", totalClient);
                // accept() will block until a client connects to the server.
                // If execution reaches this point, then it means that a client
                // socket has been accepted.

                // For each client, we will start a service thread to
                // service the client requests. This is to demonstrate a
                // Multi-Threaded server. Starting a thread also lets our
                // MultiThreadedSocketServer accept multiple connections simultaneously.

                // Start a Service thread

                ClientService cliThread = new ClientService(clientSocket, totalClient);
                memberList.add(cliThread);
                fScheduler.execute(cliThread);
//                new Thread(cliThread).start();

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

    @Override
    public void run() {
        setServerOn();

    }

    class ClientService implements Runnable{

         // Obtain the input stream and the output stream for the socket
         // A good practice is to encapsulate them with a BufferedReader
         // and a PrintWriter as shown below.
        BufferedReader in = null;
        PrintWriter out = null;

        Socket myClientSocket;
        boolean m_bRunThread = true;
        private int clientID = 0;
        //private LinkedList<String> msgQueue = null;
        List<String> msgList = null;

        public ClientService(){

            super();
        }

        public ClientService(Socket s, int id){
            msgList = Collections.synchronizedList(new LinkedList<String>());
            clientID = id;
            myClientSocket = s;

        }

        public void putInMsg(String msg){
            msgList.add(msg);
        }

        public void run(){

            // Print out details of this connection 
            System.out.println("Accepted Client Address - " + myClientSocket.getInetAddress().getHostName());

            try{

                in = new BufferedReader(new InputStreamReader(myClientSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(myClientSocket.getOutputStream()));

                // At this point, we can read for input and reply with appropriate output.


                String initialMsg = "initialization " + String.valueOf(clientID) + "\n";
                System.out.println(initialMsg);
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
            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally{
                // Clean up 
                try{
                    in.close();
                    out.close();
                    myClientSocket.close();
                    fScheduler.shutdown();
//                    fScheduler.awaitTermination();
                    System.out.println("...Stopped");
                }
                catch(IOException ioe){
                    ioe.printStackTrace();
                }
            }
        }


    }

    class Controller implements Runnable{

        ArrayList<Treasure> treasure = null;
        int currentClient = 0;
        String target = null;
        String command = null;



        public Controller(){

            treasure = new ArrayList<Treasure>();
            treasure.add(new Treasure("A", 0));
            treasure.add(new Treasure("B", 0));
            treasure.add(new Treasure("C", 0));

        }

        private void printTreasureState(){

            Treasure item;
            for (Object aTreasure : treasure) {
                item = (Treasure)aTreasure;
                String msg = item.name + " ";
                if (item.owner != 0) {
                    msg += "YES " + item.owner;
                } else {
                    msg += "NO " + item.owner;
                }
                System.out.println(msg);

            }

        }

        private void releaseItem(String target, int client){

            Treasure item = null;

            //Find target
            for (Object aTreasure : treasure) {
                item = (Treasure)aTreasure;
                if (item.name.equals(target)) {
                    break;
                }
            }

            //Check if the client own it.
            if(item.owner == client){
                item.owner = 0;
                System.out.println("Client: " + client + "release item " + target);
            }else{
                System.out.println("Client: " + client + "do not has item " + target);
            }

        }

        private void getItem(String target, int client){

            Treasure item = null;

            //Find target
            for (Object aTreasure : treasure) {
                item = (Treasure)aTreasure;
                if (item.name.equals(target)) {
                    break;
                }
            }

            //Check Target available
            if(item.owner == 0){
                item.owner = client;
                sendMsgToWorker("YES " + target + "\n");
            }else{
                sendMsgToWorker("NO " + target + "\n");
            }

        }

        private void sendMsgToWorker(String msg){

            ClientService client = null;

            for (Object aMember : memberList) {
                client = (ClientService)aMember;
                if (client.clientID == currentClient) {
                    client.putInMsg(msg);
                }
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

                    command = msg.split(" ")[0];
                    target = msg.split(" ")[1];
                    currentClient = Integer.parseInt(msg.split(" ")[2]);

                    System.out.printf("commmand : %s, target: %s, currentClient: %s\n", command, target, currentClient);

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

        String name = null;
        int owner = 0;

        public Treasure(String name, int owner){
            this.name = name;
            this.owner = owner;
        }

        public void setOwner(int owner){
            this.owner=owner;
        }

        public int getOwner(){
            return owner;
        }

    }

}

