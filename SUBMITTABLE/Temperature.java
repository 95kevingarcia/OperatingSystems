
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
//used to simulate changing temperature
import java.util.Random;
/*
 * the following are for getting sstem time and sending
 * it in the proper format.
 */
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This is the Temperature class. This is a temperature sensor when queried if
 * it is it will send its device ID and the current temperature. Temperature is
 * simulated.
 * 
 * @author Kierstin Matsuda
 */
public class Temperature {
    // the IP address of the gateway server
    // private static final String GATEWAY_IP = "35.231.34.83"; // googles
    // private static final String GATEWAY_IP = "10.108.109.5"; // Kevins at FIU
    private static final String GATEWAY_IP = "192.168.1.107";
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;
    // the amount that this slave node's time is offset from the master nodes time
    // in seconds
    private static int timeOffset = 0;

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
        String type = "sensor";
        String name = "temperature";
        out.println(type);
        out.println(name);

        // parseInt message from the server
        String message = in.readLine();

        // record myID, a positive integer assigned by server and print to Standard Out
        int myID = Integer.parseInt(message);
        System.out.println("Successfully connected to Gateway Server with ID " + myID);
        String input = "";
        /* to store id's sent by server to compare with myID */
        int receivedID = -1; // Ids are positive numbers
        double currentTemp = 0.0;
        int adjustOffset = 0; // to adjust time offset of timestamps
        String[] receivedSplit; // to split input string into two parts

        // this loop will end when the server terminates the connection
        while (true) {
            try {
                // read in message from the gateway server
                input = in.readLine();
                if (input != null) {
                    // if this device is being queried, send back status
                    if (input.contains(".")) {
                        // if input contains a . then the server is sending a clock/timestamp adjustment
                        receivedSplit = input.split(". ");

                        if (myID == Integer.parseInt(receivedSplit[0])) {
                            // adjustment to current clock offset is in receivedSplit[1] as an int
                            adjustOffset = Integer.parseInt(receivedSplit[1]);
                            timeOffset += adjustOffset;
                        }
                    } else if (myID == (receivedID = Integer.parseInt(input))) {
                        currentTemp = getTemp();
                        out.println(myID);
                        out.println(currentTemp);
                        out.println(getTime(timeOffset) + ", " + type + " " + name + " with ID: " + myID
                                + " has status " + currentTemp);
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