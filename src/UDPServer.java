import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;


public class UDPServer {

    // Initializing serverPort, data bytes array and required transmission codes.

    public static final int serverPort = 5000;
    public static final int totalBytes= 104;
    public static final int dataOnlyBytes = 100;
    public static final int fileNotFoundCode = 24;
    public static final int ackCode=6;
    public static final int etxCode = 3;

    public static final int nakCode = 22;

    public static final int stxCode = 2;


    public static DatagramSocket serverSocket;
    public static InetAddress clientAddress;
    public static int clientPort;

    // byte array
    public static byte[] data;

    public static DatagramPacket sendPacket, receivePacket;


    public static void main(String args[]) throws Exception {

        serverSocket = new DatagramSocket(serverPort);
        data = new byte[totalBytes];

        sendPacket = new DatagramPacket(data, data.length,clientAddress,clientPort);


        // Packet to receive
        receivePacket = new DatagramPacket(data, data.length);


        while (true){
            data = new byte[totalBytes];

            receiveDataPacketFromClient(data);

            String receivedStringFromClient = new String(data);
            System.out.println("Received String From Client: " + receivedStringFromClient);
            String trimmedString = receivedStringFromClient.trim();

            writeToFile(trimmedString);

            String dataToSendInString;

            if(!trimmedString.equals("")){
              sendFile(trimmedString);
              continue;
            }
            else if(data[0]==ackCode){
                System.out.println("ACK received");
                continue;
            }
            else{
                dataToSendInString = receivedStringFromClient.toUpperCase();
            }

            data = new byte[totalBytes];
            data = dataToSendInString.getBytes();

            sendDataPacketToClient(data);

        }

    }

    private static void sendDataPacketToClient(byte[] dataToSend) throws IOException {
        System.out.println();
        System.out.println("Sending data");
        sendPacket.setPort(clientPort);
        sendPacket.setAddress(clientAddress);
        sendPacket.setData(dataToSend);
        serverSocket.send(sendPacket);
    }

    private static void receiveDataPacketFromClient(byte[] receiveData) throws IOException {
        receivePacket.setData(receiveData);
        serverSocket.receive(receivePacket);

        clientAddress = receivePacket.getAddress();
        clientPort = receivePacket.getPort();
    }


    private static void writeToFile(String stringData) {

        try (FileWriter fw = new FileWriter("text.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(stringData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(String fileName) throws IOException {
        int countValue = 0;
        int maxSize = dataOnlyBytes;

        byte[] dataToSend = new byte[maxSize];

        String filePath = "serverDirectory/" + fileName + ".txt";

        System.out.println(filePath);

        FileInputStream fis;

        File file;

        try {
            file = new File(filePath);
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("Given File Not Found");

            byte[] fileNotFoundByteCode = new byte[]{fileNotFoundCode};

            sendDataPacketToClient(fileNotFoundByteCode);
            return;
        }

        int lengthOfFile = 0;

        while ((countValue = fis.read(dataToSend)) != -1) {
            lengthOfFile += countValue;
        }

        int noOfPackets = lengthOfFile / maxSize;

        fis.close();

        FileInputStream fStream = new FileInputStream(file);

        int frameNumber = 0;

        for (int key = 0; fStream.read(dataToSend) != -1; key++) {
            System.out.println();

            byte[] dataFrame = new byte[totalBytes];


            if (key == 0)
                dataFrame[0] = stxCode;
            dataFrame[1] = (byte) frameNumber;

            for ( int i = 2, j = 0; i <= 101; i++, j++)
                dataFrame[i] = dataToSend[j];


            if (key > noOfPackets)
                break;

            if (key == noOfPackets)
                dataFrame[103] = etxCode;

            dataFrame[102] = CheckSum.createChecksum(dataFrame);
            sendDataPacketToClient(dataFrame);

            System.out.println("\n Sending a Frame");
            System.out.println(dataFrame);

            System.out.println();
            getAckCodeWhenTimeOut(dataFrame);


            System.out.println("\n ACK received");

            dataToSend = new byte[maxSize];

            frameNumber = frameNumber == 0 ? 1 : 0;

        }
    }



    private static boolean getAckCodeWhenTimeOut(byte[] dataFrame) throws IOException {
        final boolean[] ackCodeRecevied = new boolean[1];

        serverSocket.setSoTimeout(1000);

        try {
            ackCodeRecevied[0] = getAckCode();
        } catch (SocketTimeoutException e) {
//            e.printStackTrace();

            sendDataPacketToClient(dataFrame);
            System.out.println(dataFrame);

            return getAckCodeWhenTimeOut(dataFrame);
        }

        System.out.println("TimeOut!");
        if (ackCodeRecevied[0]) {
            System.out.println("ACK Received");
            return true;
        }

        else {
            sendDataPacketToClient(dataFrame);
            System.out.println(dataFrame);

            System.out.println("ACK received after NAK");
            return getAckCodeWhenTimeOut(dataFrame);
        }
//
    }

    private static boolean getAckCode() throws IOException {
        byte[] receiveData = new byte[totalBytes];
        receiveDataPacketFromClient(receiveData);
        byte[] dataFrame = receiveData;
        return ackCode == dataFrame[2];
    }





}
