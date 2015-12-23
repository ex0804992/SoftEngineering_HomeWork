package main.CDCModule;

import main.ServerModule.TCPServer;

import javax.swing.*;
import javax.swing.tree.ExpandVetoException;
import java.net.InetAddress;

/**
 * Created by dblab2015 on 2015/12/21.
 */
public class ServerMain {
    CentralizedDataCenter cdc;
    UDPBC udpbc;
    RoomSettingThread roomSettingThread;

    public static void main(String[] args) {
        new ServerMain();
    }

    public ServerMain() {
        try {
            JFrame window = new JFrame();
            cdc = new CentralizedDataCenter();
            udpbc = new UDPBC(cdc);
            roomSettingThread = new RoomSettingThread(cdc, udpbc);
            roomSettingThread.startRoomSettingThread();

            TCPServer tcpServer = new TCPServer(cdc);
            tcpServer.initTCPServer();

//        cdc.addCharacter(0,10,81,3);
//        cdc.addCharacter(1,21,71,3);
//        cdc.addCharacter(2,31,61,3);
//        cdc.addCharacter(3,41,51,3);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
