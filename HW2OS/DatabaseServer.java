/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
//error handling
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Scanner;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
//used to keep track of the output
import java.util.HashMap;
/* used to send intruder alert date and time */
import java.text.SimpleDateFormat;
import java.util.Calendar;
/* the following imports are for 
communicating with gateway server */
import java.net.ServerSocket;

/**
 * This is the backend tier of the smarthome. It communicates with the gateway
 * server to keep a log of all of the smart home activities. This database
 * server communicates with the gateway to record a searchable log of the events
 * that happen to the sensors and devices connected to the gateway server. This
 * database server is the back-end tier of the smart home system. It only
 * communicates with the other part of this tier, the smarthome's gateway
 * server.
 * 
 * @author Kevin Garcia
 * @author Kierstin Matsuda
 * @author Eric Cao
 */
public class DatabaseServer {

    // the IP address of the gateway server
    // private static final String GATEWAY_IP = "10.10.8.109.5"; // google
    private static final String GATEWAY_IP = "10.108.109.5"; // Kevins at FIU
    private static final double SECONDS = 1000000000.0;
    private static final int DBRATE = 1;
    private static final int QUIT = 60;

    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;
    // used to store wether the file creation is successful
    private static Boolean fileCreated = false;
    private static BufferedWriter writer;
    // private static PrintWriter writer;
    private static File file;
    // arraylist to store what will be printed to the log file by bufferedwriter
    private static ArrayList<String> output = new ArrayList<String>();
    // integer to determine the delay of the database file in seconds
    private static int delay = 10;

    /**
     * This database server communicates with the gateway to record a searchable log
     * of the events that happen to the sensors and devices connected to the gateway
     * server. This database server is the back-end tier of the smart home system.
     * It only communicates with the other part of this tier, the gateway server.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws IOException {
        // connecting to the gateway server
        startConnection(GATEWAY_IP, PORT);
        // registering as a database with the gateway server
        out.println("database");
        out.println("database");
        System.out.println("Successfully connected to Gateway Server");
        // variables to store the input and timer informatin
        String input = "";
        String[] receivedSplit;
        long start, end, quit;
        quit = start = System.nanoTime();

        /*
         * infinite while loop, designed to run indefinitely unless the gateway server
         * cancels the connection
         */
        while (true) {
            /*
             * if (in.IOException() != null) { stopConnection(); break; }
             */
            try {
                // if gateway server is sending something, read in info
                if (in.ready()) {
                    input = in.readLine();
                    // if input is not null then split the string into two
                    if (input != null) {
                        quit = System.nanoTime();
                        receivedSplit = input.split(", ");
                        /*
                         * check if message was requesting a db search (contains DATABASE) else it was
                         * sending information for the log file, update log file
                         */
                        if (receivedSplit[0].equalsIgnoreCase("DATABASE")) {
                            database(receivedSplit[1]); // search database
                        } else {
                            update(input); // append to the output array list
                        }
                    } else {
                        break;
                    }
                }
            } // terminate this program if the server has cancelled the connection
            catch (IOException e) {
                System.out.println(e.toString());
                System.out.println("Server reset connection ");
                break;
            }
            end = System.nanoTime(); // end timer to wait to write to file
            if (((end - start) / SECONDS) > DBRATE) {
                // try writing contents of the output array list to db log file
                try {
                    dbWrite();
                } catch (IOException e) {
                    System.out.println("There was an error writing to log file.");
                    System.out.println("Error occured: " + e);
                }
                start = System.nanoTime(); // restart the timer
            }
            if (((end - quit) / SECONDS) > QUIT) {
                break;
            }
        }
        stopConnection();
        System.out.println("Disconnected from Gateway Server");
        while (output.size() > 0) {
            end = System.nanoTime(); // end timer to wait to write to file
            if (((end - start) / SECONDS) > DBRATE) {
                try {
                    dbWrite();
                } catch (IOException e) {
                    System.out.println("There was an error writing to log file.");
                    System.out.println("Error occured: " + e);
                }
                start = System.nanoTime(); // restart the timer
            }
        }
        System.out.println("Finished Writing to file.");
    }

    /**
     * This method creates a text log file with the name being when the file was
     * created. It will write all correct lines in the arraylist to the file via a
     * buffered writer. It will append to the end until there are no more lines to
     * write from the arraylist titled "output"
     */
    private static void dbWrite() throws IOException {
        /*
         * If a file has not already been created then create one with the first time
         * stamp as the name of the file
         */
        if (!fileCreated) {
            fileCreated = true;
            String date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
            date = date.replace("/", "-");
            file = new File(date + ".txt");
        }
        // try to create a buffered writer with the log file
        try {
            writer = new BufferedWriter(new FileWriter(file.getName(), true));
        }
        // catch a file not found exeption rerun the database server again
        catch (FileNotFoundException ex) {
            System.out.println("Please run this program again.\nError occured: " + ex + "\n file could not be found");
        }
        // compute the delay for the database log file
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -delay);

        // for all items in the output arraylist
        for (int i = (output.size() - 1); i >= 0; i--) {
            // if this item in the output arraylist contains the correct timestamp and data,
            // print it to the log file
            if (output.get(i).contains(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(calendar.getTime()))) {
                writer.append(output.get(i) + "\n");
                output.remove(output.indexOf(output.get(i)));
            }
        }
        // close the buffered writer
        writer.close();
    }

    /**
     * This method searches the database for a line that contains the string input.
     * If the input is found in the line, then the line is printed to the gateway
     * server which sends it to the user thread
     * 
     * @param String Input
     */
    private static void database(String input) throws FileNotFoundException {

        Boolean found = false;
        Scanner scan = new Scanner(file);

        // searching the current text log file for a line that contains the input
        while (scan.hasNext()) {
            String line = scan.nextLine().toLowerCase().toString();
            // sending the line found to match out to the gateway server
            if (line.contains(input)) {
                out.println(line);
                found = true;
            }
        }
        /*
         * if a line is never found to match that input then send to gateway server that
         * it was not found, exit method
         */
        if (!found) {
            out.println("The Database record " + input + " was not found");
        }
        scan.close();
    }

    /**
     * This method adds a string to the output array list
     * 
     * @param String input
     */
    private static void update(String input) {
        output.add(input);
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