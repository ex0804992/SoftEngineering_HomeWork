import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class Client implements Runnable{

    String serverName = "localhost";
    static final int PORT_NUMBER = 11111;
    BufferedReader inFromUser;
    PrintWriter outToServer;
    BufferedReader inFromServer;
    Socket clientSocket = null;
    private static ScheduledExecutorService fScheduler = null;
    private static final int NUM_THREADS = 1;

    private static ArrayList<Item> treasure = null;
    int clientID = 0;

    public Client(){

        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
        treasure = new ArrayList<Item>();
        treasure.add(new Item("A"));
        treasure.add(new Item("B"));
        treasure.add(new Item("C"));
    }

    public void setClientOn() throws Exception{

        clientSocket = new Socket(serverName, PORT_NUMBER);
        outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        if(clientSocket != null) {
            final ScheduledFuture<?> alarmFuture = fScheduler.scheduleWithFixedDelay(new scheduledTask(), 0, 1, TimeUnit.SECONDS);
            runClient();
        }
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
    }

    private void releaseTreasure(String target){

        String msgToServer = "RELEASE " + target + "\n";
        outToServer.write(msgToServer);
        outToServer.flush();

        findItem(target).setOwn(false);
//        System.out.println("Client: " + clientID + " Item: " + target + findItem(target).isOwn());

    }

    private void updateItemTimeLeft(){

//        System.out.println("In updateItemTimeLeft: update item!");

        for (Item item : treasure) {
            if (item.timeLeft != 0) {
                --item.timeLeft;
                if(item.timeLeft == 0){
                    System.out.println("RELEASE " + item.name);
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

    public void runClient() throws Exception{

        //Server will send id to this client.
        String initialMsg = inFromServer.readLine();
        clientID = Integer.parseInt(initialMsg.split(" ")[1]);
//        System.out.println("Client: " + clientID);

        while(true){
            if (inFromServer.ready()) {
                String response = inFromServer.readLine();
//                System.out.println("Client: " + clientID + response);
                String target = response.split(" ")[1];
                response = response.split(" ")[0];

                Item item = findItem(target);

                if (response.equals("YES")) {

                    item.setOwn(true);
                    item.setTimeLeft(5);

                    System.out.println("Client: " + clientID + " Item: " + target + findItem(target).isOwn());

                }else if(response.equals("NO")){

                    item.setOwn(false);
                    System.out.println("Client: " + clientID + " Item: " + target + findItem(target).isOwn());
                }else{

                    System.out.println("Invalid Response!!!");
                }

            }
        }

    }

    @Override
    public void run() {
        try {
            setClientOn();

        }catch(Exception e){
            System.out.println(e);
        }

    }

    class scheduledTask implements Runnable{

        private int counter = 0;
        private String target = null;
        private Item currentGetItem = null;

        @Override
        public void run() {

            updateItemTimeLeft();

            //Get treasure every seconds.
            currentGetItem = treasure.get((counter%3));
            if(!currentGetItem.isOwn()){
                System.out.println("In scheduledTask: get item!");
                target = currentGetItem.name;
                getTreasure(target);
            }

            if((counter % 3) == 0){
                printTreasureState();
            }

            counter++;
        }
    }

    public static void main(String[] args) throws Exception{
        Client clientA = new Client();
        Thread clientThreadA = new Thread(clientA);
        clientThreadA.start();

        Client clientB = new Client();
        Thread clientThreadB = new Thread(clientB);
        clientThreadB.start();

    }

}

class Item {

    String name = null;
    boolean isOwn = false;
    int timeLeft = 0;

    public Item(String name){
        this.name = name;
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