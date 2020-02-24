
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/*
 * the following are for getting sstem time and sending
 * it in the proper format.
 */
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This is the smart bulb class The bulb can be on or off, when queried if it is
 * on or off it will send its device ID and it's status.
 * 
 * @author Kierstin Matsuda
 */
public class SmartBulb {
    // the IP address of the gateway server
    // private static final String GATEWAY_IP = "35.231.34.83"; // googles
    // private static final String GATEWAY_IP = "192.168.1.125"; // Kierstins at
    // Kierstins
    // private static final String GATEWAY_IP = "10.108.109.5"; // Kevins at FIU
    private static final String GATEWAY_IP = "10.108.155.180"; // Eric at FIU
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;
    /* keep track of the bulb hardwares state, on=true, off=false */
    private static boolean bulbOn = true;
    // the amount that this slave node's time is offset from the master nodes time
    // in seconds
    private static int timeOffset = 0;

    /**
     * The smart bulb will connect to the gateway server. It will then run an
     * infinite while loop that can be cancelled by the gateway server.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Boolean bulbChange = false;
        startConnection(GATEWAY_IP, PORT);

        // Registering with the server as a device of type bulb
        String type = "device";
        String name = "bulb";
        out.println(type);
        out.println(name);

        // parseInt message from the server and print to Standard Out
        String message = in.readLine();
        int myID = Integer.parseInt(message);
        System.out.println("Successfully connected to Gateway Server with ID " + myID);
        String input = "";
        int receivedID = 0;
        String[] receivedSplit; // to split input string into two parts
        int adjustOffset = 0; // to adjust time offset of timestamps

        // this loop will end when the server terminates the connection
        while (true) {
            try {
                input = in.readLine();
                if (input != null) {
                    // server will send "id,status" to change bulb
                    if (input.contains(",")) {
                        // split int id from status
                        receivedSplit = input.split(",");
                        // id will be in string [0]
                        receivedID = Integer.parseInt(receivedSplit[0]);

                        if (myID == receivedID) {
                            // status to switch to will be in string [1]
                            if (receivedSplit[1].equalsIgnoreCase("true")) {
                                if (!bulbOn) {
                                    bulbChange = true;
                                    bulbOn = true;
                                }
                            } else {
                                if (bulbOn) {
                                    bulbChange = true;
                                    bulbOn = false;
                                }
                            }
                        }
                    } else if (input.contains(".")) {
                        // if input contains a . then the server is sending a clock/timestamp adjustment
                        receivedSplit = input.split(". ");

                        if (myID == Integer.parseInt(receivedSplit[0])) {
                            // adjustment to current clock offset is in receivedSplit[1] as an int
                            adjustOffset = Integer.parseInt(receivedSplit[1]);
                            timeOffset += adjustOffset;
                        }
                    } else {
                        /*
                         * no status change sent with the id means this device is being queried, send
                         * myID and bulb status
                         */
                        receivedID = Integer.parseInt(input);
                        if (myID == receivedID) {
                            out.println(myID);
                            out.println(bulbOn);
                            out.println(getTime(timeOffset) + ", " + type + " " + name + " with ID: " + myID
                                    + " has status " + bulbOn);
                            bulbChange = false;
                        }
                    }
                } else {
                    // end infinite while loop
                    stopConnection();
                    break;
                }
            } catch (IOException e) {
                // catch connection exception
                System.out.println(e.toString());
                System.out.println("Server reset connection");
                stopConnection();
                break;
            }
        }
        System.out.println("Disconnected from Gateway Server");
    }

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
     * This method stops a connection with a server It also closes its input/output
     * streams with the server
     * 
     * @throws IOException
     */
    public static void stopConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

    /**
     * This method starts a connection with an ip address at a designated port
     * 
     * @param ip   - a string in ip format "0.0.0.0"
     * @param port - the port number to connect to
     * @throws IOException
     */
    public static void startConnection(String ip, int port) throws IOException {

        socket = new Socket(ip, port);
        // Streams for communicating with the gateway server
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
}