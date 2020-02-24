
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This outlet class will turn an outlet on or off based on commands from the
 * gateway server.
 * 
 * @author Eric Cao
 * @author Kierstin Matsuda
 */

public class Outlet {
    // the IP address of the gateway server
    private static final String GATEWAY_IP = "35.231.34.83"; // google
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket clientSocket;
    // Streams for communicating with the gateway server
    private static PrintWriter out;
    private static BufferedReader in;
    /* keep track of the outlet hardwares state outletOn=false means outlet off */
    private static boolean outletOn = true;
    // store ID int assigned by the gateway server
    private static int myID;

    /**
     * Main method will start connection, run connection and then stop connection
     * the infinite loop for this client is in runConnection.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        startConnection(GATEWAY_IP, PORT);
        runConnection();
        stopConnection();
    }

    /**
     * This method will run a connection with the gateway, receiving push commands
     * to turn the outlet on or off.
     * 
     * @throws IOException
     */
    public static void runConnection() throws IOException {

        // register device with gateway server as an outlet
        out.println("device");
        out.println("outlet");

        // read in and store registered ID assigned by gateway and print to Standard Out
        String message = in.readLine();
        myID = Integer.parseInt(message);
        System.out.println("Successffully connected to Gateway Server with ID " + myID);
        // all id's assigned by server are positive, receivedID to check if
        // stored ID matches with the query ID
        int receivedID = -1;

        String[] receivedSplit; // to split input string into two parts
        String input = ""; // to store input string

        // this loop will end when the server terminates the connection
        while (true) {
            try {
                input = in.readLine();
                if (input != null) {
                    // split int id from status
                    if (input.contains(",")) {
                        // split int id from status
                        receivedSplit = input.split(",");
                        // id will be in string [0]
                        receivedID = Integer.parseInt(receivedSplit[0]);

                        // check if this device id is being queried
                        if (myID == receivedID) {
                            // status to switch to will be in string [1]
                            if (receivedSplit[1].equalsIgnoreCase("true")) {
                                outletOn = true;
                            } else {
                                outletOn = false;
                            }
                        }
                    } else {
                        /*
                         * no status change sent with the id means this device is being queried, send
                         * myID and outlet status
                         */
                        receivedID = Integer.parseInt(input);
                        if (myID == receivedID) {
                            out.println(myID);
                            out.println(outletOn);
                        }
                    }
                } else {
                    // end infinte while loop
                    stopConnection();
                    break;
                }
            } catch (IOException e) {
                // catch buffered reader exception
                System.out.println(e.toString());
                System.out.println("Server reset connection");
                stopConnection();
                break;
            }
        }
        System.out.println("Disconnected from Gateway Server");
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