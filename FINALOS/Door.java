
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
/*
 * the following are for getting sstem time and sending
 * it in the proper format.
 */
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This door class is primarily a push based sensor for wether a door is open or
 * not. Can be queried for state if desired.
 * 
 * @author Kevin Garcia
 * @author Kierstin Matsuda
 */
public class Door {
    // the time rate at which motion is generated
    private static final int DOORRATE = 2;
    // the IP address of the gateway server
    // private static final String GATEWAY_IP = "35.231.34.83"; // Google server IP
    private static final String GATEWAY_IP = "10.108.155.180"; // Eric at FIU
    // private static final String GATEWAY_IP = "10.108.109.5"; // Kevins at FIU
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket clientSocket;
    // Streams for communicating with the gateway server
    private static PrintWriter out;
    private static BufferedReader in;
    // store this ints ID assigned and sent by server
    private static int id;
    private static final double SECONDS = 1000000000.0;
    /*
     * to store wether this door has been open(true) or closed(false)
     */
    private static boolean open = false;
    // the amount that this slave node's time is offset from the master nodes time
    // in seconds
    private static int timeOffset = 0;

    /**
     * The sensor will connect with the gateway server and push its status when it
     * detects a change in door status. It will push true if door is open, and only
     * push false when the door closes. The door opening and closing is simulated by
     * choosing random booleans.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // connecting to the gateway server
        startConnection(GATEWAY_IP, PORT);
        boolean doorOpen = false;
        Random randomBoolean = new Random();
        // registering with the gateway server
        String type = "sensor";
        String name = "door";
        out.println(type);
        out.println(name);

        /*
         * receiving the integer id assigned by the gateway server and print to standard
         * out
         */
        id = Integer.parseInt(in.readLine());
        System.out.println("Successfully connected to Gateway Server with ID " + id);
        long start, end;
        start = System.nanoTime();
        String input = "";
        int adjustOffset = 0; // to adjust time offset of timestamps
        String[] receivedSplit; // to split input string into two parts

        // this loop will end when the server terminates the connection
        while (true) {
            // check if server has cancelled connection
            if (out.checkError()) {
                stopConnection();
                break;
            }
            try {
                // if the stream is ready to be read
                if (in.ready()) {
                    input = in.readLine();
                    // make sure this sensor's id is being queried
                    if (input.contains(".")) {
                        // if input contains a . then the server is sending a clock/timestamp adjustment
                        receivedSplit = input.split(". ");

                        if (id == Integer.parseInt(receivedSplit[0])) {
                            // adjustment to current clock offset is in receivedSplit[1] as an int
                            adjustOffset = Integer.parseInt(receivedSplit[1]);
                            timeOffset += adjustOffset;
                        }
                    } else if (input != null && id == Integer.parseInt(input)) {
                        // send this sensors id and status
                        out.println(id);
                        out.println(doorOpen);
                        out.println(getTime(timeOffset) + ", " + type + " " + name + " with ID: " + id + " has status "
                                + doorOpen);
                    } else {
                        // end infinite while loop
                        stopConnection();
                        break;
                    }
                } else {
                    end = System.nanoTime();
                    // delay simulating motion for a few seconds
                    if (((end - start) / SECONDS) > (DOORRATE)) {
                        // simulate motion sensor by choosing random boolean
                        doorOpen = randomBoolean.nextBoolean();
                        start = System.nanoTime();
                    }
                    // only push status to server if status changes
                    if (doorOpen != open) {

                        // send status to server
                        out.println(id);
                        out.println(doorOpen);
                        out.println(getTime(timeOffset) + ", " + type + " " + name + " with ID: " + id + " has status "
                                + doorOpen);
                        // reset motion to movement to detect change next iteration
                        open = doorOpen;
                    }
                }
            } catch (IOException e) {
                // catch if server has reset or cancelled the connection
                System.out.println(e.toString());
                in.close();
                break;
            } // end try catch
        } // end while true
        stopConnection();
        System.out.println("Disconnected from Gateway Server");
    }// end main

    /**
     * This function will get the systems current time and format it into the
     * neccesary format to send it to the gateway server.
     * 
     * @param int offset
     */
    public static String getTime(int offset) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, offset);
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(calendar.getTime());
    }

    /**
     * This method starts a connection with an ip address at a designated port
     * 
     * @param ip   - a string in ip format "0.0.0.0"
     * @param port - the port number to connect to
     * @throws IOException
     */
    public static void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        // Streams for communicating with the gateway server
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    /**
     * This method stops a connection with a server It also closes its input/output
     * streams with the server
     * 
     * @throws IOException
     */
    public static void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}