// A stateless bridge between a client and a server simulating UDP packet loss and error.
// Author: Dr. Mohammad Rezaeian

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class Bridge {
    public static byte[] data;
    public static final int BRIDGE_PORT = 4000;
    public static final int SERVER_PORT = 5000;
    public static final int TOTAL_TRANSFER_BYTE= 104;
    public static final int DATA_ONLY_BYTES = 100;

    public static void main(String[] args) throws Exception {
        boolean perfectNetwork=false;
        boolean noLoss=false; //Packet loss is not possible (only error)
        boolean noAckLoss=false;//Packets from client to server impossible to be lost
        boolean s2c=false;
        int N=4;//statisticaly a loss or error event happens in every N packets
        byte[] dataBuffer = new byte[TOTAL_TRANSFER_BYTE];
        int bridgePort=4000;
        int serverPort=5000;
        InetAddress IPAddress = InetAddress.getByName("localhost");
        DatagramPacket sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length, IPAddress, serverPort);
        DatagramPacket receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
        do{try{ //for avoiding rare binding exceptions
            DatagramSocket routerSocket = new DatagramSocket(bridgePort);
            routerSocket.receive(receivePacket);
            int clientPort = receivePacket.getPort();
            int downlink=0,uplink=1;
            boolean lost=false;
            while(true)
            {
                if(!lost||noLoss)routerSocket.send(sendPacket);
                lost=false;
                dataBuffer = new byte[TOTAL_TRANSFER_BYTE];
                receivePacket.setData(dataBuffer);
                sendPacket.setData(dataBuffer);
                System.out.println("Uplink="+uplink+"    Downlink="+downlink);
                routerSocket.receive(receivePacket);
                if (receivePacket.getPort()== clientPort)
                {sendPacket.setPort(serverPort);uplink++;s2c=false;}
                else {sendPacket.setPort(clientPort);downlink++;s2c=true;}
                if(!perfectNetwork){
                    if(!noAckLoss||(noAckLoss && s2c))
                        if( (int)(Math.random()*N) == 1 )lost=true;
                    if( (int)(Math.random()*N) == 1 )dataBuffer[2]='*';
                }
            }
        }catch(SocketException e){bridgePort++;System.out.println("Note to client: Bridge port is "+bridgePort);}
        }while(true);
    }
}