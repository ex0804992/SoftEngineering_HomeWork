import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Client implements Runnable, clientOperation{

    private String serverIp;
    static final int PORT_NUMBER = 11111;
    private BufferedReader inFromUser;
    PrintWriter outToServer;
    BufferedReader inFromServer;
    Socket clientSocket = null;
    private static ScheduledExecutorService fScheduler = null;
    private static final int NUM_THREADS = 1;

    private ArrayList<Item> treasure = null;
    int clientID = 0;

    private enum MoveCode {
        TURNEAST, TURNSOUTH, TURNNORTH, TURNWEST, GET
    }

    public Client(){
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
        treasure = new ArrayList<Item>();   //Initialize treasure list.
        treasure.add(new Item("A"));
        treasure.add(new Item("B"));
        treasure.add(new Item("C"));
    }

    @Override
    public boolean connectServer(String serverip){
        try {


            this.serverIp = serverip;
            clientSocket = new Socket(serverip, PORT_NUMBER);
            outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            if(clientSocket.isConnected()) {

                runClient();
                return true;    //If it is here, it means work is finished
            }

        }catch(IOException IOE){
            System.out.println(IOE.toString());
        }

        return false;   //It means connection fail.
    }
    /**
     * Class inputMoves
     *
     * UIM or DOM call this method to send moveCode to sever.
     *
     * **/
     @Override
     public void inputMoves(String moveCode) {

        //Wrap moveCode into Gson object

        //Send it to server
//        System.out.println("out to server");
        outToServer.print(moveCode + "\n");
        outToServer.flush();

    }

    private Item findItem(String target){

        for (Item item : treasure) {
            if (item.name.equals(target)) {
                return item;
            }
        }
        return null;
    }

    private void getTreasure(String target){

        String msgToServer = "GET " + target + "\n";
        outToServer.write(msgToServer);
        outToServer.flush();

//        System.out.println("Client: " + clientID + " GET " + findItem(target).name);
    }

    private void releaseTreasure(String target){

        String msgToServer = "RELEASE " + target + "\n";
        outToServer.write(msgToServer);
        outToServer.flush();
//        System.out.println("Client: " + clientID + " RELEASE " + findItem(target).name);

    }

    private void updateItemTimeLeft(){

        for (Item item : treasure) {
            if (item.timeLeft != 0) {
                item.timeLeft = item.timeLeft - 1;
                if(item.timeLeft == 0){
                    item.setOwn(false);
                    releaseTreasure(item.name);
                }
            }
        }
    }

    private void printTreasureState(){

        String msg = "Client " + clientID + "\n";
        for (Item item : treasure) {
            if (item.isOwn()) {
                msg += item.name + " YES " + item.timeLeft + "\n";
            } else {
                msg += item.name + " NO" + "\n";
            }
        }

        System.out.println(msg);
    }

    private void runClient(){

        //Server will send initialized id to this client.
//        String initialMsg = inFromServer.readLine();
//        clientID = Integer.parseInt(initialMsg.split(" ")[1]);

        //Start periodic tasks.
//        final ScheduledFuture<?> alarmFuture = fScheduler.scheduleWithFixedDelay(new ScheduledTask(), 0, 1, TimeUnit.SECONDS);
//        for(MoveCode m : MoveCode.values()) {
//            inputMoves(m.toString());
//        }

//        while(true){
//            if (inFromServer.ready()) {
//                //Read message and divide it to be "target" and "response".
//                String message = inFromServer.readLine();
//                String target = message.split(" ")[1];
//                String response = message.split(" ")[0];
//                Item item = findItem(target);
//
//                if (response.equals("YES")) {
//
//                    item.setOwn(true);
//                    item.setTimeLeft(5);
//
////                    System.out.println("Client: " + clientID + " Item: " + target + item.isOwn());
//
//                }else if(response.equals("NO")){
//
//                    item.setOwn(false);
////                    System.out.println("Client: " + clientID + " Item: " + target + item.isOwn());
//                }else{
//
//                    System.out.println("Invalid Response!!!");
//                }
//
//            }
//        }

    }

    @Override
    public void run() {
        try {
            if(!connectServer(serverIp)){

                System.out.printf("Connection fail!!!");

            }

        }catch(Exception e){
            System.out.println(e);
        }

    }


    /**
     * Inner Class ScheduledTask
     *
     * ScheduledTask is in charge of periodic task.
     *
     * **/
    class ScheduledTask implements Runnable{

        private int counter = 0;
        private String target = null;
        private Item currentGettedItem = null;
        private int itemIndex = 0;

        @Override
        public void run() {

            //Update item's time and release item if its left time is 0.
            updateItemTimeLeft();

            //Get treasure every seconds.
            itemIndex = counter % 3;
            currentGettedItem = treasure.get(itemIndex);    //Question: if I have current item, then skip it or get next?
            if(!currentGettedItem.isOwn()){
                target = currentGettedItem.getName();
                getTreasure(target);
            }

            //Print state every 3 seconds.
            if((counter % 3) == 0){
                printTreasureState();
            }

            counter++;
        }
    }

//    public static void main(String[] args) throws Exception{
//        Client clientA = new Client("127.0.0.1");
//        Thread clientThreadA = new Thread(clientA);
//        clientThreadA.start();
//
//        Client clientB = new Client("127.0.0.1");
//        Thread clientThreadB = new Thread(clientB);
//        clientThreadB.start();
//
//    }

    /**
     * Inner Class Item
     *
     * Client's item
     *
     * **/
    class Item {

        String name = null;
        boolean isOwn = false;
        int timeLeft = 0;

        public Item(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }

        public void setTimeLeft(int time){
            timeLeft = time;
        }

        public void setOwn(boolean state){
            this.isOwn = state;
        }

        public boolean isOwn(){
            return isOwn;
        }

    }

}

