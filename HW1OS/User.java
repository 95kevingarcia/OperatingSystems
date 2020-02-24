
//used to communicate with gateway server
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * This class allows the user to use command line prompts to control the smart
 * home from anywhere through the gateway server.
 *
 * @author Eric Cao
 * @author Kierstin Matsuda
 * @author Kevin Garcia
 */
public class User {

    // the IP address of the gateway server
    private static final String GATEWAY_IP = "35.231.34.83"; // googles
    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket clientSocket;
    // Streams for communicating with the gateway server
    private static PrintWriter out;
    private static BufferedReader in;

    /**
     * The user can type in an integer to select an option on a menu
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Scanner keyboard = new Scanner(System.in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        int id;
        int inputInt;
        String message;
        Boolean run = true;
        Boolean output = true;

        startConnection(GATEWAY_IP, PORT);
        // registering with the gateway server as user
        out.println("user");
        out.println("user");
        // reading and storing the id assigned by the gateway for future use and print
        // to Standard Out
        message = in.readLine();
        id = Integer.parseInt(message);
        System.out.println("Successffully connected to Gateway Server with ID " + id);

        System.out.println("Welcome! Enter one of the following commands:");
        while (run) {
            if (out.checkError()) {
                System.out.println("Server reset connection");
                stopConnection();
                break;
            }
            if (output) {
                System.out.println("(1) Check heater status");
                System.out.println("(2) Turn heater on");
                System.out.println("(3) Turn heater off");
                System.out.println("(4) Check temperature");
                System.out.println("(5) Check motion sensor status");
                System.out.println("(6) Check lightbulb status");
                System.out.println("(7) Turn lightbulb on");
                System.out.println("(8) Turn lightbulb off");
                System.out.println("(9) Change mode: HOME");
                System.out.println("(0) Change mode: AWAY");
                System.out.println("(11) Exit");
                output = false;
            }
            if (reader.ready()) {
                output = true;
                try {
                    inputInt = keyboard.nextInt();
                } catch (Exception e) {
                    inputInt = -1;
                }
                switch (inputInt) {
                case 1:
                    checkHeater(); // checks heater's status
                    break;
                case 2:
                    changeHeater("true"); // user wants to turn on the outlet
                    break;
                case 3:
                    changeHeater("false"); // user wants to turn off the outlet
                    break;
                case 4:
                    checkTemperature(); // user wants to check the temperature
                    break;
                case 5:
                    checkMotion(); // user wants to check the motion sensor's status
                    break;
                case 6:
                    checkBulb(); // user wants to check the bulb's status
                    break;
                case 7:
                    changeBulb("true"); // user wants to turn on the bulb
                    break;
                case 8:
                    changeBulb("false"); // user wants to turn off the bulb
                    break;
                case 9:
                    changeMode("HOME"); // user is home
                    break;
                case 0:
                    changeMode("AWAY"); // user is away
                    break;
                case 11:
                    run = false; // EXIT
                    out.println("exit");
                    break;
                default:
                    System.out.println("ERROR: Unknown command");
                    System.out.println(inputInt);
                    break;
                }
            }
            // receive = false;
            textMessage();
            // receive = true;
        }
        stopConnection();
        keyboard.close();
        System.out.println("Disconnected from Gateway Server");
    }

    /**
     * Check heater will send a request to gateway server to get the status of
     * wether the heater plugged into the outlet is on or off.
     * 
     * @throws IOException
     */
    public static void checkHeater() throws IOException {
        String input;
        out.println("outlet");
        input = in.readLine();

        if (input.equalsIgnoreCase("true")) {
            System.out.println("The heater is on.");
        } else {
            System.out.println("The heater is off.");
        }
    }

    /**
     * change heater will send a message to server telling it to change the status
     * of the heater to true(on) or false(off). It will print the status to the
     * user.
     * 
     * @param status
     */
    public static void changeHeater(String status) {
        out.println("outlet," + status);

        if (status.equalsIgnoreCase("true")) {
            System.out.println("The heater is now on.");
        } else {
            System.out.println("The heater is now off.");
        }
    }

    /**
     * check temperature will send a request to the server to see the current
     * temperature from the temperature sensor. It will print the temperature to the
     * user.
     * 
     * @throws IOException
     */
    public static void checkTemperature() throws IOException {
        out.println("temperature");
        System.out.println("The temperature is currently " + in.readLine() + " degrees celcius.");
    }

    /**
     * Check bulb will request from the gateway server the status of the smart light
     * bulb. It is printed to the user.
     * 
     * @throws IOException
     */
    public static void checkBulb() throws IOException {
        String input;
        out.println("bulb");
        input = in.readLine();

        if (input.equalsIgnoreCase("true")) {
            System.out.println("The bulb is on.");
        } else {
            System.out.println("The bulb is off.");
        }
    }

    /**
     * check Motion will ask the gateway server for the status of the motion
     * detector. This information is printed out to the user.
     * 
     * @throws IOException
     */
    public static void checkMotion() throws IOException {
        String input;
        out.println("motion");
        input = in.readLine();

        if (input.equalsIgnoreCase("true")) {
            System.out.println("Motion detected.");
        } else {
            System.out.println("No motion detected.");
        }
    }

    /**
     * change bulb will send true(on) or false(off) to the gateway server to turn
     * the smartbulb on or off.
     * 
     * @param status
     */
    public static void changeBulb(String status) {
        out.println("bulb," + status);

        if (status.equalsIgnoreCase("true")) {
            System.out.println("The bulb is now on.");
        } else {
            System.out.println("The bulb is now off.");
        }
    }

    /**
     * changeMode will change the user's mode to either "home" or "away" It prints
     * the status to the user.
     * 
     * @param mode
     */
    public static void changeMode(String mode) {
        out.println(mode);
        System.out.println("Mode is now set to " + mode + ".");
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
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    /**
     * This method stops a connection with a server It also closes its input/output
     * streams with the server.
     * 
     * @throws IOException
     */
    public static void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    /**
     * This method will print out a message to the user from the gateway server
     * 
     * @throws IOException
     */
    public static void textMessage() throws IOException {
        if (in.ready()) {
            System.out.println(in.readLine());
        }
    }
}