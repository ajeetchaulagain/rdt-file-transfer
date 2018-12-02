import java.io.*;
import java.net.*;




class UDPClient {

   public static DatagramSocket clientSocket;

   public static DatagramPacket sendPacket, receivePacket;

   public static BufferedReader bufferReader;



   public static byte[] data;

   // Number codes for diff

    public static final int totalBytes= 104;
    public static final int dataOnlyBytes = 100;
    public static final int fileNotFoundCode = 24;
    public static  final int bridgePort = 4000;
    public static final int ackCode=6;
    public static final int etxCode = 3;
    public static final int nakCode = 22;



    public static void main(String args[]) throws IOException{


        System.out.println("Enter a file name:");

        // initializing Client Socket
        clientSocket = new DatagramSocket();


        //Getting IP Address of client machine
        InetAddress ipAddress = InetAddress.getByName("localhost");

        // byte array to create a packet
        data = new byte[totalBytes];


        // Get a packet to send
        sendPacket = new DatagramPacket(data, data.length,ipAddress,bridgePort);


        // Packet to receive
        receivePacket = new DatagramPacket(data, data.length);


        bufferReader = new BufferedReader(new InputStreamReader(System.in));


        while(true){
            String userInput = bufferReader.readLine();

            if(userInput.equals("exit"))
                break;

            sendDataPacketsToServer(userInput);

            String fileName = userInput.trim();
            if(!fileName.equals("")){
                receiveFileFromServer(fileName);
            }
            else{
                receiveDataPacketFromServer();
            }
        }
         clientSocket.close();
    }



    private static void sendDataPacketsToServer(String userInput) {

        // creating byte array of 104 bytes to send and receive data packets
        data = new byte[104];
        data = userInput.getBytes();

        sendPacket.setData(data);

        try {
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void receiveDataPacketFromServer() {

        data = new byte[104];

        receivePacket.setData(data);

        try {
            clientSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverResponseString = new String(data);

        System.out.println("Response From Server: "+ serverResponseString);
    }

    private static void receiveFileFromServer(String fileName) throws IOException {

        data = new byte[totalBytes];

        receivePacket.setData(data);
        try {
            clientSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] dataFrame;

        if(receivePacket.getData()[0]== fileNotFoundCode){
            System.out.println("Given File doesnot exist in server");
            return;
        }

        FileWriter fileWriter = new FileWriter("clientDirectory/"+fileName+".txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);

        byte frameNumber = 1;

        while(true){
            System.out.println();

            dataFrame = data;
            System.out.println("---------------------------");
            System.out.println("Received Frame Details ");
            System.out.println("----------------------------");
            String frameMessage=
                    "Start Flag: " + dataFrame[0] + "\nFrame Block: "+ dataFrame + "\nFCS:" + dataFrame[102] +"\nEnd Flag: "+ dataFrame[103];

            System.out.println(frameMessage);

            if (CheckSum.verifyChecksum(dataFrame)) {
                if (frameNumber == dataFrame[1]) {
                    System.out.println("Receiving Data..");

                    sendAckCode(dataFrame, ackCode);
                    data = new byte[totalBytes];
                    receivePacket.setData(data);
                    clientSocket.receive(receivePacket);
                    continue;

                }

                byte[] dataOnlyFrame = new byte[dataOnlyBytes];
                for(int i=2, j=0; i<=101;i++,j++)
                    dataOnlyFrame[j] = dataFrame[i];

                String dataString = new String(dataOnlyFrame).trim();


                System.out.println("Content in a Frame: "+ dataString);

                printWriter.print(dataString);

                printWriter.flush();

                sendAckCode(dataFrame,ackCode);

                frameNumber = dataFrame[1];

                if(dataFrame[103]==etxCode){
                    System.out.println("File successfully transmitted!!");
                    break;
                }

            }
            else{
                // sending negative acknowledgement if file sending fails.
                sendAckCode(dataFrame, nakCode);
            }

            System.out.println("\n \n Receiving next Data");
            data = new byte[totalBytes];
            receivePacket.setData(data);
            clientSocket.receive(receivePacket);

        }

        printWriter.close();
        fileWriter.close();
    }


    private static void sendAckCode(byte[] dataFrame, int ack){
        System.out.println();
        System.out.println("ACK Code Received: "+ ack);
        data = new byte[totalBytes];

        data[0] = (byte) ack;

        // setting data block
        for(int i=2, j=0; i<=101; i++,j++){
            dataFrame[i] = data[j];
        }


        sendPacket.setData(dataFrame);

        try {
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }



}