import junit.framework.TestCase;
import org.junit.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class TCPServerTest extends TestCase {

    TCPServer tcpServer = null;
    static boolean isServerUp = false;
    private static ScheduledExecutorService fScheduler;
    private static final int NUM_THREADS = 4;
    MockCDC mockCDC = null;
    Thread thread = null;

    private enum MoveCode {
        TURNEAST, TURNSOUTH, TURNNORTH, TURNWEST, GET
    }

    @BeforeClass
    public static void setUpOnce(){
        fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    @Before
    public void setUp() throws Exception {

        mockCDC = new MockCDC();

//        if(!isServerUp) {
        thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        tcpServer = new TCPServer(11111, mockCDC);
                        isServerUp = true;
                        tcpServer.initTCPServer();
                    }catch(Exception e){}
                }
            });
        thread.start();
//        }
    }

    @After
    public void tearDown() throws Exception {

        mockCDC = null;
        tcpServer = null;
        thread = null;

    }

    @Test
    public void testInitTCPServer() throws Exception {

        MockClient mockClient = new MockClient("127.0.0.1");
        assertTrue(mockClient.isconnected());
        mockClient.close();

    }

//    @Rule
//    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetClientIPTable() throws Exception {

        MockClient mockClient[] = new MockClient[10];

        for(int i  = 0 ; i< 10 ; i++){
            mockClient[i] = new MockClient("127.0.0.1");
        }

        Thread.sleep(2000);

        ArrayList<String> testList =  tcpServer.getClientIPTable();
        assertEquals(10, testList.size());
        for(int i  = 0 ; i< 10 ; i++){
            assertEquals(testList.get(i), mockClient[i].getIP());
        }
        for(int i  = 0 ; i< 10 ; i++){
            mockClient[i].close();
        }
    }

    @Test
    public void testUpdateDirection() throws Exception{

        MockClient mockClient = new MockClient("127.0.0.1");
        Thread.sleep(2000);
        mockClient.inputMoves(MoveCode.TURNEAST.toString());
        assertTrue(mockCDC.getIsUpdateDirection());
        mockClient.close();
    }
//
//    @Test
//    public void testUpdateGet() throws Exception{
//
//        MockClient mockClient = new MockClient("127.0.0.1");
//        mockClient.inputMoves("GET");
//        assertTrue(mockCDC.getIsGetItem());
//        mockClient.close();
//    }

    class MockClient implements clientOperation{

        Socket clientSocket;
        PrintWriter outToServer;

        public MockClient(String serverip){

            try {
                clientSocket = new Socket(serverip, 11111);

                outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            }catch(IOException e){
            }
        }



        public String getIP(){

            return clientSocket.getInetAddress().getHostName();

        }

        public void close() throws Exception{

            clientSocket.close();
        }

        public boolean isconnected(){

            return clientSocket.isConnected();

        }

        @Override
        public boolean connectServer(String serverip) {
            return false;
        }

        @Override
        public void inputMoves(String moveCode) {
            outToServer.print(moveCode + "\n");
            outToServer.flush();

        }
    }

    class MockCDC implements CDCOperation{

        private boolean isUpdateDirection;
        private boolean isGetItem;

        public MockCDC(){

            isUpdateDirection = false;
            isGetItem = false;

        }

        public boolean getIsUpdateDirection(){
            return isUpdateDirection;
        }

        public boolean getIsGetItem(){
            return isGetItem;
        }

        @Override
        public void updateDirection(int clientno, int MoveCode) {
            System.out.println("update direction!");
            isUpdateDirection = true;
        }

        @Override
        public void getItem(int clientno) {
            System.out.println("get something!");
            isGetItem = true;
        }
    }

}