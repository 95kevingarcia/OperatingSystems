
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This is the smart bulb class The bulb can be on or off, when queried if it is
 * on or off it will send its device ID and it's status.
 * 
 * @author Kierstin Matsuda
 */
public class SmartBulb {
    // the IP address of the gateway server
    private static final String GATEWAY_IP = "35.231.34.83"; // googles
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;
    /* keep track of the bulb hardwares state, on=true, off=false */
    private static boolean bulbOn = true;

    /**
     * The smart bulb will connect to the gateway server. It will then run an
     * infinite while loop that can be cancelled by the gateway server.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        startConnection(GATEWAY_IP, PORT);

        // Registering with the server as a device of type bulb
        out.println("device");
        out.println("bulb");

        // parseInt message from the server and print to Standard Out
        String message = in.readLine();
        int myID = Integer.parseInt(message);
        System.out.println("Successffully connected to Gateway Server with ID " + myID);
        String input = "";
        int receivedID = 0;
        String[] receivedSplit;

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
                                bulbOn = true;
                            } else {
                                bulbOn = false;
                            }
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