package ServerModule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class TCPServer implements serverOperation{

    private static ScheduledExecutorService fScheduler = null;
    private static final int NUM_THREADS = 4;
    private ArrayList<ClientService> memberList = null;
    private ArrayList<String> clientIPTable = null;
    private List<String> serverMsgQueue = null;
    private ServerSocket serverSocket;
//    private int serverPort = 0;
    private boolean serverOn = false;
    private int totalClient = 0;
    CDCOperation cdcOperation;

    private enum MoveCode {
        TURNEAST, TURNSOUTH, TURNNORTH, TURNWEST, GET
    }

    public TCPServer(CDCOperation cdcOperation) throws Exception{
        this.cdcOperation = cdcOperation;
//        this.serverPort = serverPort;
        memberList = new ArrayList<ClientService>();    //Collect client's socket
        clientIPTable = new ArrayList<String>();    //Collect client's IP
//        serverMsgQueue = Collections.synchronizedList(new LinkedList<String>()); //Wrap linkedlist in synchronizedList to handle race condition.
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);

    }

    @Override
    public void initTCPServer() throws IOException{

        try
        {
            //Create server and execute Controller to handle event.
            serverSocket = new ServerSocket(11111);
            serverOn = true;

            //Wait for connections and keep connection in the member list.
            while(serverOn){

                // Accept incoming connections.
                Socket clientSocket = serverSocket.accept();
                totalClient++;

                // Print out and collect IP of this connection
                String clientIP = clientSocket.getInetAddress().getHostName();
                System.out.println("Accepted ClientModule.Client Address - " + clientIP);
                clientIPTable.add(clientIP);

                //Create worker thread to keep socket and handle I/O.
                ClientService clientWorkerService = new ClientService(clientSocket, totalClient);
                memberList.add(clientWorkerService);
                fScheduler.execute(clientWorkerService);

            }



        }catch(Exception ee)
        {
            ee.printStackTrace();
            System.out.println("Could not create server. Quitting.");
        }
//        finally{
//            serverSocket.close();
////            fScheduler.shutdown();
//            System.out.println("Server Stopped");
//        }
    }

    public void stop() {

        try {
            serverSocket.close();
            fScheduler.shutdown();
            System.out.println("Server Stopped");
            for(ClientService client : memberList){
                client.stop();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }


    /**
     * getClientIPTable()
     *
     * give client's IP table to be used to broadcast.
     * **/
    @Override
    public ArrayList<String> getClientIPTable() {
        return clientIPTable;
    }

    /**
    * Inner Class ClientService
    *
    * ClientService is the worker thread to keep and monitor connection status, and help handle I/O.
    *
    * **/
    class ClientService implements Runnable{

        private Socket myClientSocket;
        private boolean workerOn = true;
        private int clientID = 0;
        private List<String> workerMsgQueue = null;
        private BufferedReader in = null;
        private PrintWriter out = null;

        public ClientService(Socket myClientSocket, int id) throws Exception{
            workerMsgQueue = Collections.synchronizedList(new LinkedList<String>());
            clientID = id;
            in = new BufferedReader(new InputStreamReader(myClientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(myClientSocket.getOutputStream()));
        }

        public void putMsgInWorkerQueue(String msg){
            workerMsgQueue.add(msg);
        }

        public void stop() {

            try {

                workerOn = false;
                if (in != null) in.close();
                if (out != null) out.close();
                if(myClientSocket != null) myClientSocket.close();
//                    fScheduler.shutdown();
                System.out.println("...Stopped");
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public void run(){

            try{


//                sendInitialMsg();

                // Run in a loop until workerOn is set to false
                while(workerOn){

                    //Get input from client and put it in server's message queue.
                    if(in.ready()){
//get Gson object from client, then decode it before put it in server message queue.
                        //According to moveCode, finding which action to do.
                        String msg = in.readLine();
                        MoveCode moveCode = MoveCode.valueOf(msg);
                        switch (moveCode){
                            case TURNEAST:
                                cdcOperation.updateDirection(0, moveCode.ordinal());
                                break;
                            case TURNNORTH:
                                cdcOperation.updateDirection(0, moveCode.ordinal());
                                break;
                            case TURNSOUTH:
                                cdcOperation.updateDirection(0, moveCode.ordinal());
                                break;
                            case TURNWEST:
                                cdcOperation.updateDirection(0, moveCode.ordinal());
                                break;
                            case GET:
                                cdcOperation.getItem(0);
                                break;
                        }

                    }

                }

            }catch(Exception e){
                e.printStackTrace();
            }
//            finally{
//                try{
//                    workerOn = false;
//                    if(in != null) in.close();
//                    if(out != null) out.close();
//                    myClientSocket.close();
////                    fScheduler.shutdown();
//                    System.out.println("...Stopped");
//                }
//                catch(IOException ioe){
//                    ioe.printStackTrace();
//                }
//            }
        }

    }

//    /**
//    * Inner Class Controller
//    *
//    * It is in charge of handling event and managing Treasure.
//    *
//    * **/
//    class Controller implements Runnable{
//
//        private ArrayList<Treasure> treasure = null;
//
//
//        public Controller(){
//            treasure = new ArrayList<Treasure>();
//            treasure.add(new Treasure("A", 0));
//            treasure.add(new Treasure("B", 0));
//            treasure.add(new Treasure("C", 0));
//        }
//
//        private Treasure findItem(String target){
//
//            for (Treasure item : treasure) {
//                if (item.getName().equals(target)) {
//                    return item;
//                }
//            }
//            return null;
//        }
//
//        private void releaseItem(String target, int client){
//
//            Treasure item = findItem(target);
//
//            if(item.getOwner() == client){
//                item.setOwner(0);
////                System.out.println("ClientModule.Client: " + client + "release item " + target);
//            }else{
//                System.out.println("ClientModule.Client: " + client + "do not has item " + target);
//            }
//
//        }
//
//        private void getItem(String target, int client){
//
//            Treasure item = findItem(target);
//
//            if(item.getOwner() == 0){
////                System.out.println("GETITEM: " + target + "  " + client);
//                item.setOwner(client);
//                sendMsgToWorker("YES " + target + "\n", client);
//            }else{
//                sendMsgToWorker("NO " + target + "\n", client);
//            }
//
//        }
//
//        private void sendMsgToWorker(String msg, int ClientModule.Client){
//
//            for (ClientService worker : memberList) {
//                if (worker.clientID == ClientModule.Client) {
//                    worker.putMsgInWorkerQueue(msg);
//                }
//            }
//
//        }
//
//        private void printTreasureState(){
//
//            for (Treasure item : treasure) {
//                String msg = item.getName() + " ";
//                if (item.getOwner() != 0) {
//                    msg += "YES " + item.getOwner();
//                } else {
//                    msg += "NO " + item.getOwner();
//                }
//
//                System.out.println(msg);
//
//            }
//        }
//
//        @Override
//        public void run() {
//
//            //Every 3 seconds, print out treasure state.
//            final ScheduledFuture<?> soundAlarmFuture = fScheduler.scheduleWithFixedDelay(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            System.out.println("============================");
//                            printTreasureState();
//                        }
//                    }, 0, 3, TimeUnit.SECONDS
//            );
//
//
//
//            while(serverOn){
//                //If there is something in server's message queue, then handle it.
//                if(!serverMsgQueue.isEmpty()){
//                    String msg = serverMsgQueue.remove(0);
//                    System.out.println(msg);
//
//                    String command = msg.split(" ")[0];
//                    String target = msg.split(" ")[1];
//                    int currentClient = Integer.parseInt(msg.split(" ")[2]);
//
//                    if(command.equals("GET")){
//
//                        getItem(target, currentClient);
//
//                    }else if(command.equals("RELEASE")){
//
//                        releaseItem(target, currentClient);
//
//                    }else{
//
//                        System.out.println("Invalid command!!!");
//                    }
//                }
//            }
//        }
//    }

//    /**
//     * Inner Class Treasure
//     *
//     * Server's treasure
//     *
//     * **/
//    class Treasure {
//
//        private String name = null;
//        private int owner = 0;
//
//        public Treasure(String name, int owner){
//            this.name = name;
//            this.owner = owner;
//        }
//
//        public String getName(){
//            return name;
//        }
//
//        public void setOwner(int owner){
//            this.owner=owner;
//        }
//
//        public int getOwner(){
//            return owner;
//        }
//
//    }
//
////    public static void main(String[] args) throws Exception {
////        ServerModule.TCPServer server = new ServerModule.TCPServer(11111);
////        server.initTCPServer();
////    }

}

