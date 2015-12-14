import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientTest extends TestCase {

    Client client;

    public ClientTest(){

        client = new Client();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        client = null;
    }

    @Test
    public void testConnectServerSuccess() throws Exception {

        assertTrue(client.connectServer("127.0.0.1"));

    }

    @Test
    public void testConnectServerFail() throws Exception {

        assertFalse(client.connectServer("192.177.1.1"));

    }

    @Test
    public void testInputMoves() throws Exception {

    }
}