import ClientModule.clientOperation;
import ServerModule.CDCOperation;
import ServerModule.TCPServer;
import junit.framework.TestCase;
import org.junit.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class TCPServerTest extends TestCase {

     TCPServer tcpServer = null;
    static boolean isServerUp = false;
     MockCDC mockCDC = null;
     Thread thread = null;

    private enum MoveCode {
        TURNEAST, TURNSOUTH, TURNNORTH, TURNWEST, GET
    }


    @Before
    public void setUp() throws Exception {


//        if(!isServerUp) {
        mockCDC = new MockCDC();
        thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        tcpServer = new TCPServer(mockCDC);
                        isServerUp = true;
                        tcpServer.initTCPServer();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
        thread.start();
//        }
    }

    @After
    public void tearDown() throws Exception {

        mockCDC = null;
        tcpServer.stop();
        thread = null;

//        Thread.sleep(3000);
    }

    @Test
    public void testInitTCPServer() throws Exception {
        MockClient mockClient = new MockClient("127.0.0.1");
        assertTrue(mockClient.isconnected());
        mockClient.close();
    }
//
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
        mockClient.inputMoves(MoveCode.TURNEAST.toString());
        Thread.sleep(2000);
        assertTrue(mockCDC.getIsUpdateDirection());
        mockClient.close();

    }

    @Test
    public void testUpdateGet() throws Exception{
        MockClient mockClient = new MockClient("127.0.0.1");
        mockClient.inputMoves("GET");
        Thread.sleep(2000);
        assertTrue(mockCDC.getIsGetItem());
        mockClient.close();
    }

    class MockClient implements clientOperation {

        Socket clientSocket;
        PrintWriter outToServer;

        public MockClient(String serverip) throws Exception{

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
            if(outToServer != null){outToServer.close();}
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

    class MockCDC implements CDCOperation {

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
            isUpdateDirection = true;
            System.out.println("update direction!");

        }

        @Override
        public void getItem(int clientno) {
            System.out.println("get something!");
            isGetItem = true;
        }
    }

}