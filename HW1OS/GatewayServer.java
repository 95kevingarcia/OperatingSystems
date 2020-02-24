
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
/* Used to store thread ID's */
import java.util.ArrayList;
/* used to store thread ID's */
import java.util.HashMap;
/* used to send intruder alert date and time */
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * This class is responsible for handling all GatewayServer functions. This is a
 * multithreaded server for handling smart device clients.
 * 
 * @author Kierstin Matsuda
 * @author Kevin Garcia
 */
public class GatewayServer {

    /**
     * The port that the gateway server listens to
     */
    private static final int PORT = 8080;
    // Seconds conversion rate
    private static final double SECONDS = 1000000000.0;
    // Seconds to query
    private static final int QUERYTIME = 5;
    // Rate at which we turn off bulb if no motion is detected
    private static final int BULBTIMER = 300;

    /**
     * The set of all names of the clients connected to this server. The array list
     * stores integer ID's assigned to clients by this server The hashmaps hold the
     * device information of the clients connected to this server
     */
    private static ArrayList<Integer> ids = new ArrayList<Integer>();
    private static HashMap<Integer, String> names = new HashMap<Integer, String>();
    private static HashMap<Integer, String> types = new HashMap<Integer, String>();

    /*
     * bulbUser and outletUser will be true if the user has manually turned the bulb
     * or outlet on via the client program User.java
     */
    private static Boolean bulbUser = false;
    private static Boolean outletUser = false;
    /*
     * mode is set to away=true when the user is on "away" mode and false when user
     * is on "home" mode
     */
    private static Boolean away = false;

    /*
     * outletOn and bulbOn are set to true when they are on, and false when they are
     * off
     */
    private static Boolean outletOn = false;
    private static Boolean bulbOn = false;
    /*
     * motion detected is set to true when motion is detected, and false when there
     * is none
     */
    private static Boolean motionDetected = false;
    private static Double temp = 0.0; // the current temperature of smart thermometer

    private static final int MIN_TEMP = 1; // temperature cannot go below 1
    private static final int MAX_TEMP = 2; // heater can go off over 2

    /**
     * in main the server listens on the designated port and creats new handler
     * threads when a new device registers with the Gateway Server.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The gateway server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * This handler thread class is used to manage an individual client thread for
     * the gateway server. It can handle one client input and output via the
     * buffered reader and pritnwriter to broadcast messages. Each client thread has
     * access to shared memory.
     */
    private static class Handler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructor for the handler thread
         * 
         * @param socket
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Registers the clients information with the global set of ids, names and
         * types. each thread will run a different method based on their registration
         * type. Run contains an infinite loop that will terminate when a this server
         * reads in a null value from the client.
         */
        @Override
        public void run() {
            int id;
            String type = "";
            String name = "";
            Boolean error = false;

            /**
             * this try catch statement attempts to set up streams for conversing with the
             * client.
             */
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                type = in.readLine();
                name = in.readLine();
                // if error=true, this thread will not run further IO processes, and terminate
                if (type == null || name == null) {
                    error = true;
                }
            } catch (IOException e) {
                error = true;
                System.out.println("Error occured while creating character streams");
            }
            /**
             * begin assigning integer id's at 0, add them to the global id arraylist
             */
            if (ids.isEmpty()) {
                id = 0;
                ids.add(id);
            } else {
                id = ids.get(ids.size() - 1) + 1;
                ids.add(id);
            }
            // put the device name and type in the global hashmaps
            names.put(id, name);
            types.put(id, type);
            out.println("" + id);

            if (type.equalsIgnoreCase("user")) {
                System.out.println(name + " has registered with id " + id);
            } else {
                System.out.println(type + " " + name + " has registered with id " + id);
            }
            // if there was an error, do not run any client management, skip to finish run
            if (!error) {
                // each thread will run the process associated with their device type
                try {
                    if (name.equals("bulb")) {
                        bulb(id);
                    } else if (name.equals("motion")) {
                        motion(id);
                    } else if (name.equals("outlet")) {
                        outlet(id);
                    } else if (name.equals("temperature")) {
                        temperature(id);
                    } else if (name.equals("user")) {
                        user(id);
                    }
                } catch (IOException e) {
                    System.out.println("problem occured in " + name + " thread.");
                }
            }
            error = false;
            try {
                // closing the streams and socket.
                socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                error = true;
                System.out.println("Error has occured when closing a socket.");
            }
            if (!error) {
                // printing out disconnect statements
                if (type.equalsIgnoreCase("user")) {
                    System.out.println(name + " with id " + id + " has disconnected");
                } else {
                    System.out.println(type + " " + name + " with id " + id + " has disconnected");
                }
            }
        }

        /**
         * This method contains all the activities that the bulb thread needs to perform
         * while its client is connected to the system. It contains an infinite while
         * loop that will terminate when this server reads a null from the buffered
         * reader for this socket
         * 
         * @param id
         * @throws IOException
         */
        public void bulb(int id) throws IOException {
            String input = "";
            long timer, first, second, third;
            boolean queried = false;
            Boolean bulb = bulbOn;
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                timer = System.nanoTime();
                // wait for the timer to query the bulb
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) {
                    // query once only within the timer window, if queried already, skip.
                    if (!queried) {
                        // System.out.println("QUERY BULB");
                        // first = System.nanoTime();
                        out.println("" + id);
                        input = in.readLine();
                        // second = System.nanoTime();
                        /*
                         * Read in client ID and check if correct if buffered reader gets a null or it
                         * receives an incorrect ID skip query
                         */
                        if (input == null || !input.equals("" + id)) {
                            break;
                        }
                        /* read in client status, could be saved if there is a physical light switch */
                        input = in.readLine();
                        /*
                         * third = System.nanoTime(); System.out.println("It took " + (second - first) /
                         * SECONDS + " seconds to send a query and receive a response");
                         * System.out.println("It took " + (third - second) / SECONDS +
                         * " seconds to receive a second response after the first response");
                         * System.out.println();
                         */
                        // if buffered reader reads null, client has quit connection
                        if (input == null) {
                            break;
                        }
                        queried = true;
                    }
                } else {
                    queried = false;
                }
                /*
                 * make sure the bulb is always set to the status in shared memory the user can
                 * change the status of the bulb via shared memory
                 */
                if (bulb != bulbOn) {
                    out.println(id + "," + bulbOn);
                    bulb = bulbOn;
                }
            }
        }

        /**
         * This method contains all the activities that the motion thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket.
         * 
         * @param id
         * @throws IOException
         */
        public void motion(int id) throws IOException {
            long start, end, timer;
            String input;
            start = System.nanoTime();
            boolean queried = false;
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                timer = System.nanoTime();
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) {
                    // query once only within the timer window, if queried already, skip.
                    if (!queried) {
                        out.println("" + id);
                        input = in.readLine();

                        /*
                         * Read in client ID and check if correct if buffered reader gets a null or it
                         * receives an incorrect ID skip query
                         */
                        if (input == null || !input.equals("" + id)) {
                            break;
                        }
                        input = in.readLine();
                        // if buffered reader reads null, client has quit connection
                        if (input == null) {
                            break;
                        }
                        // if input = true then there is motion detected
                        if (input.equalsIgnoreCase("true")) {
                            motionDetected = true;
                            /*
                             * if the user does not have the bulb swiched on manually or mode set to away,
                             * switch the bulb on
                             */
                            if (!bulbUser && !away) {
                                bulbOn = true;
                            }
                        } else {
                            motionDetected = false;
                        }
                        /* completed query for this timer, set to true to wait for next query cycle */
                        queried = true;
                    }
                } else {
                    queried = false;
                }
                if (in.ready()) {
                    input = in.readLine();

                    /*
                     * Read in client ID and check if correct if buffered reader gets a null or it
                     * receives an incorrect ID skip query
                     */
                    if (input == null || !input.equals("" + id)) {
                        break;
                    }
                    input = in.readLine();
                    // if buffered reader reads null, client has quit connection
                    if (input == null) {
                        break;
                    }
                    /*
                     * if input is true and the user has not set the bulb on manally and they are
                     * not on away mode, set the bulb to be on
                     */
                    if (input.equals("true")) {
                        if (!bulbUser && !away) {
                            bulbOn = true;
                        }
                        motionDetected = true;
                    } else {
                        motionDetected = false;
                    }
                }
                /* if motion is detected start the timer */
                if (motionDetected) {
                    start = System.nanoTime();
                }
                end = System.nanoTime();
                /*
                 * if the timer has elapsed, and the user isnt on away mode and the user has
                 * also not set the bulb manually, turn the motion sensing bulb off.
                 */
                if (!away && !bulbUser && (((end - start) / SECONDS) > (BULBTIMER))) {
                    bulbOn = false;
                }
            }
        }

        /**
         * This method contains all the activities that the outlet thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket
         * 
         * @param id
         * @throws IOException
         */
        public void outlet(int id) throws IOException {
            String input = "";
            long timer;
            Boolean outlet = outletOn;
            Boolean queried = false;
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                timer = System.nanoTime();
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) {
                    // query once only within the timer window, if queried already, skip.
                    if (!queried) {
                        out.println("" + id);
                        input = in.readLine();

                        /*
                         * Read in client ID and check if correct if buffered reader gets a null or it
                         * receives an incorrect ID skip query
                         */
                        if (input == null || !input.equals("" + id)) {
                            break;
                        }
                        input = in.readLine();
                        // if buffered reader reads null, client has quit connection
                        if (input == null) {
                            break;
                        }
                        /* completed query for this timer, set to true to wait for next query cycle */
                        queried = true;
                    }
                } else {
                    queried = false;
                }
                /* force the outlet to always be set to what is stored in shared memory */
                if (outlet != outletOn) {
                    out.println(id + "," + outletOn);
                    outlet = outletOn;
                }
            }
        }

        /**
         * This method contains all the activities that the temperature thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket
         * 
         * @param id
         * @throws IOException
         */
        public void temperature(int id) throws IOException {
            String input = "";
            long timer;
            Boolean queried = false;
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                timer = System.nanoTime();
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) {
                    // query once only within the timer window, if queried already, skip.
                    if (!queried) {
                        out.println("" + id);
                        input = in.readLine();

                        /*
                         * Read in client ID and check if correct if buffered reader gets a null or it
                         * receives an incorrect ID skip query
                         */
                        if (input == null || !input.equals("" + id)) {
                            break;
                        }
                        input = in.readLine();
                        // if buffered reader reads null, client has quit connection
                        if (input == null) {
                            break;
                        }
                        // read in temperature as a double from client
                        try {
                            temp = Double.parseDouble(input);
                        } catch (NumberFormatException e) {
                            System.out.println("numberFormatException: " + e);
                        }
                        /* if user hasnt manually set outlet on and temp is too cold turn outlet on */
                        if (!outletUser && temp <= MIN_TEMP) {
                            outletOn = true;
                        } else if (!outletUser && temp >= MAX_TEMP) {
                            outletOn = false;
                        }
                        /* completed query for this timer, set to true to wait for next query cycle */
                        queried = true;
                    }
                } else {
                    queried = false;
                }
            }
        }

        /**
         * This method contains all the activities that the user thread needs to perform
         * while its client is connected to the system. It contains an infinite while
         * loop that will terminate when this server reads a null from the buffered
         * reader for this socket
         * 
         * @param id
         * @throws IOException
         */
        public void user(int id) throws IOException {
            Boolean outlet = outletOn;
            Boolean bulb = bulbOn;
            String[] helper;
            String input;
            Boolean intruderMessageSent = false;
            long timeOfIntrude = 0;
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                // if User is away and there was motion detected
                if (away && motionDetected) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    } /* if intruder message has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("INTRUDER ALERT! Motion was detected at " + input
                                + " and you are currently set to AWAY mode.");
                    }
                }
                /* if bulb changes value, notify user and set to correct value */
                if (bulbOn != bulb) {
                    out.println("Bulb changed from " + bulb + " to " + bulbOn);
                    bulb = bulbOn;
                }
                /* if outlet changes value, notify user and set to correct value */
                if (outletOn != outlet) {
                    out.println("Outlet changed from " + outlet + " to " + outletOn);
                    outlet = outletOn;
                }
                // if user has sent a message
                if (in.ready()) {
                    input = in.readLine();
                    // if buffered reader reads null, client has quit connection
                    if (input == null) {
                        break;
                    } /*
                       * if user sent message with comma, split message into two pieces the device
                       * name and status
                       */
                    else if (input.contains(",")) {
                        helper = input.split(",");
                        // if name is bulb, set bulb to requested status
                        if (helper[0].equalsIgnoreCase("bulb")) {
                            bulbOn = Boolean.parseBoolean(helper[1]);
                            bulbUser = bulbOn;
                        } // if name is outlet, set outlet to requested status
                        else if (helper[0].equalsIgnoreCase("outlet")) {
                            outletOn = Boolean.parseBoolean(helper[1]);
                            outletUser = outletOn;
                        }
                    } // user wants to exit
                    else if (input.equalsIgnoreCase("exit")) {
                        break;
                    } /* send status of devices to user or change mode to home/away */
                    else {
                        if (input.equalsIgnoreCase("HOME")) {
                            away = false;
                        } else if (input.equalsIgnoreCase("AWAY")) {
                            away = true;
                        } else if (input.equalsIgnoreCase("bulb")) {
                            out.println(bulbOn);
                        } else if (input.equalsIgnoreCase("motion")) {
                            out.println(motionDetected);
                        } else if (input.equalsIgnoreCase("outlet")) {
                            out.println(outletOn);
                        } else if (input.equalsIgnoreCase("temperature")) {
                            out.println("" + temp);
                        }
                    }
                }
            }
        }
    }
}