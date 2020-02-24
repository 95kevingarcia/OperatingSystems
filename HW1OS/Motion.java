
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * This motion class controls a motion detector which is a push based sensor.
 * 
 * @author Kevin Garcia
 * @author Kierstin Matsuda
 */
public class Motion {
    // the time rate at which motion is generated
    private static final int MOTIONRATE = 60;
    // the IP address of the gateway server
    private static final String GATEWAY_IP = "35.231.34.83"; // googles
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
     * to store wether this sensor senses motion true if motion sensed, false if no
     * motion sensed
     */
    private static boolean motion = false;

    /**
     * The sensor will connect with the gateway server and push its status when it
     * detects a change in motion It will push true if it detects motion, and only
     * push false when it stops detecting motion. The motion is simulated by
     * choosing random booleans.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // connecting to the gateway server
        startConnection(GATEWAY_IP, PORT);
        boolean movement = false;
        Random randomBoolean = new Random();
        // registering with the gateway server
        out.println("sensor");
        out.println("motion");
        // receiving the integer id assigned by the gateway server and print to Standard
        // Out
        id = Integer.parseInt(in.readLine());
        System.out.println("Successffully connected to Gateway Server with ID " + id);
        long start, end;
        start = System.nanoTime();
        String input = "";

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
                    if (input != null && id == Integer.parseInt(input)) {
                        // send this sensors id and status
                        out.println(id);
                        out.println(movement);
                    } else {
                        // end infinite while loop
                        stopConnection();
                        break;
                    }
                } else {
                    end = System.nanoTime();
                    // delay simulating motion for a few seconds
                    if (((end - start) / SECONDS) > (MOTIONRATE)) {
                        // simulate motion sensor by choosing random boolean
                        movement = randomBoolean.nextBoolean();
                        start = System.nanoTime();
                    }
                    // only push status to server if status changes
                    if (movement != motion) {

                        // send status to server
                        out.println(id);
                        out.println(movement);
                        // reset motion to movement to detect change next iteration
                        motion = movement;
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