
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
//used to simulate changing temperature
import java.util.Random;

/**
 * This is the Temperature class. This is a temperature sensor when queried if
 * it is it will send its device ID and the current temperature. Temperature is
 * simulated.
 * 
 * @author Kierstin Matsuda
 */
public class Temperature {
    // the IP address of the gateway server
    private static final String GATEWAY_IP = "35.231.34.83"; // googles
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;

    /**
     * The sensor will connect with the gateway server and save it's assigned id
     * When the temperature sensor receives a query it will push the temperature to
     * the gateway server. Temperature is simulated by a getTemperature method.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        startConnection(GATEWAY_IP, PORT);

        // Registering with the server as a sensor and temperature sensor
        out.println("sensor");
        out.println("temperature");

        // parseInt message from the server
        String message = in.readLine();

        // record myID, a positive integer assigned by server and print to Standard Out
        int myID = Integer.parseInt(message);
        System.out.println("Successffully connected to Gateway Server with ID " + myID);
        String input = "";
        /* to store id's sent by server to compare with myID */
        int receivedID = -1; // Ids are positive numbers

        // this loop will end when the server terminates the connection
        while (true) {
            try {
                // read in message from the gateway server
                input = in.readLine();
                if (input != null) {
                    // read in the ID sent by the server
                    receivedID = Integer.parseInt(input);
                    // if this device is being queried, send back status
                    if (myID == receivedID) {
                        out.println(myID);
                        out.println(getTemp());
                    }
                } else {
                    // exit the infinite while loop
                    stopConnection();
                    break;
                }
            } catch (IOException e) {
                // catch buffered reader exception
                System.out.println(e.toString());
                System.out.println("Server reset connection ");
                stopConnection();
                break;
            }
        }
        System.out.println("Disconnected from Gateway Server");
    }

    /**
     * This is a test function to simulate temperature readings that are sometimes
     * below 1, above 2, or between 1 and 2. This simulates fluctuating temperatures
     * in Celcius for a thermometer.
     * 
     * @return a double between 0 and 3
     */
    private static double getTemp() {

        Random ranDouble = new Random();

        return ranDouble.nextDouble() + ranDouble.nextDouble() + ranDouble.nextDouble();
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