
/* the following imports are for 
communicating with gateway server */
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
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
import java.util.Date;

/**
 * This class is responsible for handling all GatewayServer functions. This is a
 * multi threaded server for handling smart device clients.
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
    private static final int BULBTIMER = 1;
    /**
     * The set of all names of the clients connected to this server. The array list
     * stores integer ID's assigned to clients by this server The hash maps hold the
     * device information of the clients connected to this server
     */
    private static String dbQuery = "";
    private static ArrayList<String> dbLogs = new ArrayList<String>();
    private static String userQuery = "";
    private static ArrayList<Integer> ids = new ArrayList<Integer>();
    private static HashMap<Integer, String> names = new HashMap<Integer, String>();
    private static HashMap<Integer, String> types = new HashMap<Integer, String>();
    private static HashMap<Integer, Boolean> offSetWasSet = new HashMap<Integer, Boolean>();
    private static HashMap<Integer, Integer> allOffSet = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Boolean> sendOffset = new HashMap<Integer, Boolean>();
    private static ArrayList<Integer> IDS = new ArrayList<Integer>();
    private static HashMap<Integer, String> IdTrue = new HashMap<Integer, String>();
    private static HashMap<Integer, String> IdFalse = new HashMap<Integer, String>();
    private static HashMap<Integer, String> moniker = new HashMap<Integer, String>();
    private static HashMap<Integer, Boolean> status = new HashMap<Integer, Boolean>();
    private static Boolean newDevice = false;
    private static Boolean newSensor = false;
    private static long globalOffSet = 0;
    private static ArrayList<Boolean> userDeviceStatuses = new ArrayList<Boolean>();

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
     * shared memory for personEntered and personLeft, so presence and door can
     * communicate with each other
     */
    private static Boolean personLeft = false;
    private static Boolean personEntered = false;
    /*
     * shared memory for userEntered and userLeft, so user and presence can
     * communicate with each other
     */
    private static Boolean userLeft = false;
    private static Boolean userEntered = false;
    /*
     * shared memory for unknownpersonEntered and unknownpersonLeft, so user can
     * send alert messages
     */
    private static Boolean unknownPersonLeft = false;
    private static Boolean unknownPersonEntered = false;
    /*
     * outletOn and bulbOn are set to true when they are on, and false when they are
     * off
     */
    private static Boolean outletOn = false;
    private static Boolean bulbOn = false;
    /*
     * door open and beacon present are set to true when door is open and beacon is
     * sensed. they are set to false when the door is shut and the beacon cannot be
     * sensed.
     */
    private static Boolean doorOpen = false;
    private static Boolean beaconDetected = false;

    // true if motion is detected and false if there is none detected
    private static Boolean motionDetected = false;
    // the current temperature of smart thermometer
    private static Double temp = 0.0;

    private static final int MIN_TEMP = 1; // temperature cannot go below 1
    private static final int MAX_TEMP = 2; // heater can go off over 2 degrees

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
     * buffered reader and print writer to broadcast messages. Each client thread
     * has access to shared memory.
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
            if (!error && type.equals("database")) {
                id = -1;
                // try running the database thread commands
                try {
                    System.out.println("DATABASSEYY");
                    database();
                } catch (IOException e) {
                    System.out.println("problem occured in " + name + " thread.");
                }
            } else {
                // if no ID's assigned yet, start with 0, then add by 1
                if (ids.isEmpty()) {
                    id = 0;
                    ids.add(id);
                    userDeviceStatuses.add(false);
                } else {
                    id = ids.get(ids.size() - 1) + 1;
                    ids.add(id);
                    userDeviceStatuses.add(false);
                }
                // put the device name and type in the global hashmaps
                out.println("" + id);
                names.put(id, name);
                types.put(id, type);
                // if this is user thread print that they have registered
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
                        } else if (name.equals("door")) {
                            door(id);
                        } else if (name.equals("presence")) {
                            presence(id);
                        } else if (name.equals("userDevice")) {
                            if (type.equals("deviceServer")) {
                                deviceServer(id);
                            } else {
                                device(id);
                            }
                        } else if (name.equals("userSensor")) {
                            if (type.equals("sensorServer")) {
                                sensorServer(id);
                            } else {
                                sensor(id);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("problem occured in " + name + " thread.");
                    }
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
                if (type.equalsIgnoreCase("user") && id != -1) {
                    System.out.println(name + " with id " + id + " has disconnected");
                } else if (type.equals("database")) {
                    System.out.println("Database has disconnected");
                } else if (id != -1) {
                    System.out.println(type + " " + name + " with id " + id + " has disconnected");
                }
            }
        }

        /**
         * This method contains all the activities that the database thread needs to
         * perform while it is connected to the system. It contains an infinite while
         * loop that will terminate when this db server reads a null from the buffered
         * reader for this socket
         * 
         * @throws IOException
         */
        public void database() throws IOException {
            long timer;
            boolean queried = false;
            String input;

            /*
             * infinite while loop that will terminate when the client severs the connection
             */

            while (true) {
                // do search database and get stuff from database
                System.out.print("");

                // if the user has asked for something from the database
                if (dbQuery.length() > 0) {
                    // send this query string to the database
                    out.println(dbQuery);
                    // set the dbQuery to empty string so the length is 0
                    dbQuery = "";
                    // read in the next line
                    do {
                        input = in.readLine();
                        while (!userQuery.equals("")) {
                            System.out.print("");
                        }
                        userQuery = input;
                        System.out.println(userQuery + "DATA");
                    } while (!input.equals("-1"));
                }
                // get the current time
                timer = System.nanoTime();
                // wait for the timer
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) {
                    // if we have not queried in the time gap
                    if (!queried) {
                        sendAllOffSets();
                        // go over the entirety of this hasmap
                        for (int i = (dbLogs.size() - 1); i >= 0; i--) {
                            /* once printed to the database, remove what has been printed */
                            out.println(dbLogs.get(i));
                            dbLogs.remove(dbLogs.indexOf(dbLogs.get(i)));
                        }
                        // we have queried in the time gap
                        queried = true;
                    }
                    // we are not in the time gap
                } else {
                    // allow us to query the next time we are in time gap
                    queried = false;
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
            long timer;
            boolean queried = false;
            Boolean bulb = bulbOn;
            Boolean bulbChange = false;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);

            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                timer = System.nanoTime();
                // wait for the timer to query the bulb
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
                        /* read in client status, could be saved if there is a physical light switch */
                        input = in.readLine();
                        // if buffered reader reads null, client has quit connection
                        if (input == null) {
                            break;
                        }
                        queried = true;
                        input = in.readLine();
                        if (bulbChange) {
                            dbLogs.add(input);
                            bulbChange = false;
                        }
                        // calculate the offset for the timestamp of this node
                        calculateOffSet(input, id);
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
                    bulbChange = true;
                }
            }
        }

        /**
         * This method contains all the activities that the motion thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket. The motion detector in the living room
         * should not be able to sense the motion from the door opening and closing
         * 
         * @param id
         * @throws IOException
         */
        public void motion(int id) throws IOException {
            long start, end, timer;
            String input;
            start = System.nanoTime();
            boolean queried = false;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);

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
                        input = in.readLine();
                        // calculate the time offset for this node
                        calculateOffSet(input, id);
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

                    input = in.readLine();
                    calculateOffSet(input, id);
                    // print to the database log
                    dbLogs.add(input);
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
            Boolean outletChange = false;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);
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
                        input = in.readLine();
                        // calculate the time offset for this node
                        calculateOffSet(input, id);

                        // print the info to the database log
                        if (outletChange) {
                            dbLogs.add(input);
                            outletChange = false;
                        }
                    }
                } else {
                    queried = false;
                }
                /* force the outlet to always be set to what is stored in shared memory */
                if (outlet != outletOn) {
                    out.println(id + "," + outletOn);
                    outlet = outletOn;
                    outletChange = true;
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
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);
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
                        input = in.readLine();
                        dbLogs.add(input);
                        calculateOffSet(input, id);
                    }
                } else {
                    // reset for next query
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
            int size = IDS.size();
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
                System.out.print("");
                if (!userQuery.equals("")) {
                    System.out.println(userQuery + "USER");
                    out.println(userQuery);
                    userQuery = "";
                }
                /*
                 * if User is away and there was motion detected and unknown person entered the
                 * house
                 */
                if (away && motionDetected && unknownPersonEntered) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    }
                    /* if intruder message has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("INTRUDER ALERT! Someone entered front door at " + input
                                + ". You are currently set to AWAY mode and your user keychain was not detected.\n");
                    }
                    unknownPersonEntered = false; // reset for next alert
                } else if (unknownPersonLeft) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    }
                    /* if intruder message has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("INTRUDER ALERT! Someone left through front door at " + input
                                + ". You are currently set to AWAY mode and your user keychain was not detected.\n");
                    }
                    unknownPersonLeft = false; // reset for next alert
                } else if (userEntered) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    }
                    /* if welcomemessage has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("Welcome Home! You entered the front door at " + input
                                + " and you are now set to HOME mode.\n");
                    }
                    userEntered = false; // reset for next time
                } else if (userLeft) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    }
                    /* if goodbye message has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("Goodbye! You left the house at " + input + " you are now set to AWAY mode.\n");
                    }
                    userLeft = false; // reset for next time
                } else if (away && motionDetected) {
                    /* if message already sent, wait timer amount of seconds */
                    if (intruderMessageSent && ((System.nanoTime() / SECONDS) > ((timeOfIntrude / SECONDS) + 3))) {
                        intruderMessageSent = false;
                    }
                    /* if intruder message has not been sent, send message with time date stamp */
                    else if (!intruderMessageSent) {
                        intruderMessageSent = true;
                        timeOfIntrude = System.nanoTime();
                        input = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
                        out.println("INTRUDER ALERT! Motion was detected at " + input
                                + " and you are currently set to AWAY mode.\n");
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
                    System.out.println(input);
                    // if buffered reader reads null, client has quit connection
                    if (input == null) {
                        break;
                    } /*
                       * if user sent message with comma, split message into two pieces the device
                       * name and status
                       */
                    else if (input.contains(",")) {
                        // split the input on the comma, location 1 contains name
                        helper = input.split(",");
                        // if name is bulb, set bulb to requested status
                        if (helper[0].equalsIgnoreCase("bulb")) {
                            bulbOn = Boolean.parseBoolean(helper[1]);
                            bulbUser = bulbOn;
                        }
                        // if name is outlet, set outlet to requested status
                        else if (helper[0].equalsIgnoreCase("outlet")) {
                            outletOn = Boolean.parseBoolean(helper[1]);
                            outletUser = outletOn;
                        } else if (input.contains(":")) {
                            dbQuery = input;
                            System.out.println("dbQuery in user thread: " + dbQuery);
                        } else {
                            if (helper[1].equals("true")) {
                                userDeviceStatuses.set(Integer.parseInt(helper[0]), true);
                            } else {
                                userDeviceStatuses.set(Integer.parseInt(helper[0]), false);
                            }
                        }
                    } // user wants to exit
                    else if (input.equalsIgnoreCase("exit")) {
                        break;
                    } /* send status of devices to user or change mode to home/away */
                    else {
                        if (input.equalsIgnoreCase("HOME")) {
                            // user has set the home to home mode
                            away = false;
                        } else if (input.equalsIgnoreCase("AWAY")) {
                            // allow the user to set the mode of the home
                            away = true;
                        } else if (input.equalsIgnoreCase("bulb")) {
                            // send the status of the bulb
                            out.println(bulbOn);
                        } else if (input.equalsIgnoreCase("motion")) {
                            // send the status of the motion detector
                            out.println(motionDetected);
                        } else if (input.equalsIgnoreCase("outlet")) {
                            // send the status of the outlet
                            out.println(outletOn);
                        } else if (input.equalsIgnoreCase("temperature")) {
                            // send the status of the thermometer
                            out.println("" + temp);
                        } else if (input.equalsIgnoreCase("door")) {
                            // send the status of the door sensor
                            out.println(doorOpen);
                        } else if (input.equalsIgnoreCase("beacon")) {
                            // send the status of the presence sensor
                            out.println(beaconDetected);
                        } else if (input.equalsIgnoreCase("new")) {
                            // ********************************************/
                            if (in.readLine().equals("sensor")) {
                                newSensor = true;
                            } else {
                                newDevice = true;
                            }
                            System.out.println("TRY TO MAKE THIS");
                            while (size == IDS.size()) {
                                System.out.print("");
                            }
                            System.out.println("MADE IT!");
                            out.println(IDS.get(size));
                            System.out.println("I SENT IT TO YOU");
                            moniker.put(IDS.get(size), in.readLine());
                            IdTrue.put(IDS.get(size), in.readLine());
                            IdFalse.put(IDS.get(size), in.readLine());
                            size++;
                        } else if (input.contains("~")) {
                            helper = input.split("~");
                            int temporary = Integer.parseInt(helper[1]);
                            out.println(userDeviceStatuses.get(temporary));
                        }
                    }
                }
            }
        }

        /**
         * This method contains all the activities that the door thread needs to perform
         * while its client is connected to the system. It contains an infinite while
         * loop that will terminate when this server reads a null from the buffered
         * reader for this socket.
         * 
         * @param id
         * @throws IOException
         */
        public void door(int id) throws IOException {
            String input = "";
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);

            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
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
                input = in.readLine();
                calculateOffSet(input, id);
                // if input = true then door has been opened
                if (input.equalsIgnoreCase("true")) {
                    doorOpen = true;
                    /*
                     * motion happened after door was opened a person entered the house, if not
                     * someone left
                     */
                    if (motionDetected) {
                        personEntered = true;
                    } else {
                        personLeft = true;
                    }
                } else if (input.equalsIgnoreCase("false")) {
                    doorOpen = false;
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
                    // if input = true then door has been opened
                    if (input.equalsIgnoreCase("true")) {
                        // door is open
                        doorOpen = true;
                        /*
                         * motion happened after door was opened a person entered the house, if not
                         * someone left
                         */
                        if (motionDetected) {
                            personEntered = true;
                            // someone has entered but unknown if it is user
                        } else {
                            personLeft = true;
                            // someone has left but unknown if it is user
                        }
                    } else if (input.equalsIgnoreCase("false")) {
                        doorOpen = false;
                        // door is closed
                    }
                    input = in.readLine();
                    dbLogs.add(input);
                    // calculate the time offset for this node
                    calculateOffSet(input, id);
                }
                // update time offsets for devices
            }
        }// end door

        /**
         * This method contains all the activities that the presences thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket.
         * 
         * @param id
         * @throws IOException
         */
        public void presence(int id) throws IOException {
            String input;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);
            /*
             * infinite while loop that will terminate when the client severs the connection
             */
            while (true) {
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
                // beacon is present
                if (input.equalsIgnoreCase("true")) {

                    // if a person has entered and beacon is present
                    if (personEntered) {
                        personEntered = false; // reset to next time person enters
                        userEntered = true; // so user can print a message then set this to false
                        // set the away mode to home mode
                        away = false;
                    }
                    // If a person has left and beacon is present
                    else if (personLeft) {
                        personLeft = true; // reset to next time someone leaves
                        userLeft = true; // so user can print a message then set this to false
                        // set the away mode to away
                        away = true;
                    }
                    beaconDetected = true;

                    // beacon is not present
                } else if (input.equalsIgnoreCase("false")) {

                    // if a person has entered but beacon is not present
                    if (personEntered) {
                        /*
                         * alert in user thread will change this back to false when it is finished with
                         * it
                         */
                        unknownPersonEntered = true;
                    }
                    // if a person has left and beacon is not present
                    else if (personLeft) {
                        /*
                         * alert in user thread will change this back to false when it is finished with
                         * it
                         */
                        unknownPersonLeft = true;
                    }
                    beaconDetected = false;
                }
                input = in.readLine();
                // calculate the time offset for that node
                calculateOffSet(input, id);

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

                    // beacon is present
                    if (input.equalsIgnoreCase("true")) {

                        // if a person has entered and beacon is present
                        if (personEntered) {
                            personEntered = false; // reset for next entrance
                            // set the away mode to home mode
                            away = false;
                        }
                        // If a person has left and beacon is present
                        else if (personLeft) {
                            personLeft = false; // reset for when someone leaves again
                            // set the away mode to away
                            away = true;
                        }
                        beaconDetected = true;

                        // beacon is not present
                    } else if (input.equalsIgnoreCase("false")) {
                        // if a person has entered but beacon is not present
                        if (personEntered) {
                            /*
                             * alert in user thread will change this back to false when it is finished with
                             * it
                             */
                            unknownPersonEntered = true;
                        }
                        // if a person has left and beacon is not present
                        else if (personLeft) {
                            /*
                             * alert in user thread will change this back to false when it is finished with
                             * it
                             */
                            unknownPersonLeft = true;
                        }
                        beaconDetected = false;
                    }
                    input = in.readLine();
                    dbLogs.add(input);
                    calculateOffSet(input, id);
                }
            }
        }// end presence method

        /**
         * In this method the client device connected to this server submits their
         * timestamp, the difference between the client clock and the gateway clock are
         * stored to be sent so each client can reset their clock If an offset needs to
         * be sent to the client it is sent from here
         * 
         * @param String input
         * @param int    id
         */
        private void calculateOffSet(String input, int id) {
            input = input.substring(0, input.indexOf(","));
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = null;
            try {
                date = format.parse(input);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            // puts the currentOffset into the hashmap allOffSet according to id
            allOffSet.put(id, (int) ((int) ((int) System.currentTimeMillis() - (int) date.getTime()) / ((int) 3000)));
            // set that the offset for this id was set
            offSetWasSet.put(id, true);
            // if we need to send the offset to the client
            if (sendOffset.get(id)) {
                // set that we set the offset
                sendOffset.put(id, false);
                // send the offset to client
                out.println(id + ". " + (int) (globalOffSet - allOffSet.get(id)));
            }
        }

        /**
         * This method will send each of the clients the offset they need to reset their
         * clock or their timestamp. Copies of hashmaps are used to iterate through
         * values while changing other values. This method can be called once every
         * second or longer to maintain offset accuracy
         * 
         * @param int id
         */
        private void sendAllOffSets() {
            Boolean offset = true;
            double temp = 0.0;
            /*
             * temporary copy of the offsetWasSet hashmap so one copy can be read while the
             * original is being updated
             */
            HashMap<Integer, Boolean> copyOffsetWasSet = offSetWasSet;

            // for all values in the hashmap check if they are all true
            for (Boolean boo : copyOffsetWasSet.values()) {
                if (!boo) {
                    // if a false value is found then seet offset to false
                    offset = false;
                    break;
                }
            }
            // if offset is still set to true here
            if (offset) {
                // in the original offSetWasSet set all to false
                for (int i : copyOffsetWasSet.keySet()) {
                    offSetWasSet.put(i, false);
                }
                // make a copy of the offsets
                HashMap<Integer, Integer> copyAllOffSet = allOffSet;
                int counter = 0;
                /*
                 * for all values in the copy of all offsets, calculate the offset
                 */
                for (int i : copyAllOffSet.values()) {
                    temp = (temp * counter + (int) (i)) / (counter + 1.0);
                    counter++;
                }
                // Set global offset to offset
                globalOffSet = (long) temp;
                /*
                 * make a copy of send offset so this one can be used while the other is being
                 * edited
                 */
                HashMap<Integer, Boolean> copySendOffSet = sendOffset;
                // set all values in sendOffset original hashmap to true
                for (int i : copySendOffSet.keySet()) {
                    sendOffset.put(i, true);
                }
            }
        }

        /**
         * This method contains all the activities that the sensor thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket.
         * 
         * @param id
         * @throws IOException
         */
        public void sensor(int id) throws IOException {
            long start, end, timer;
            String input;
            start = System.nanoTime();
            boolean queried = false;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);
            IDS.add(id);
            // status.put(id, false);

            System.out.println("NEW SENSOR WAS CREATED");
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
                            userDeviceStatuses.set(id, true);
                        } else {
                            userDeviceStatuses.set(id, false);
                        }
                        /* completed query for this timer, set to true to wait for next query cycle */
                        queried = true;
                        input = in.readLine();
                        // calculate the time offset for this node
                        // calculateOffSet(input, id);
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
                        userDeviceStatuses.set(id, true);
                    } else {
                        userDeviceStatuses.set(id, false);
                    }

                    input = in.readLine();
                    // calculateOffSet(input, id);
                    // print to the database log
                    System.out.println(input);
                    String helper1[] = input.split(", ");
                    if (helper1[1].substring(helper1[1].lastIndexOf(" ") + 1).equals("true")) {
                        input = helper1[0] + ", " + helper1[1].substring(0, helper1[1].indexOf(" ")) + " "
                                + moniker.get(id)
                                + helper1[1].substring(helper1[1].indexOf(" ") + 11, helper1[1].lastIndexOf(" ")) + " "
                                + IdTrue.get(id);
                    } else {
                        input = helper1[0] + ", " + helper1[1].substring(0, helper1[1].indexOf(" ")) + " "
                                + moniker.get(id)
                                + helper1[1].substring(helper1[1].indexOf(" ") + 11, helper1[1].lastIndexOf(" ")) + " "
                                + IdFalse.get(id);
                    }
                    dbLogs.add(input);
                }
                /* if motion is detected start the timer */
                if (userDeviceStatuses.get(id)) {
                    start = System.nanoTime();
                }
                end = System.nanoTime();
                // if the user is adding a new device, print to device server
            }
        }

        /**
         * 
         * @param id
         * @throws IOException
         */
        public void sensorServer(int id) throws IOException {

            while (true) {
                System.out.print("");
                if (newSensor) {
                    out.println("sensor");
                    System.out.println("TRY TO MAKE A NEW SENSOR");
                    newSensor = false;
                }
            }
        }

        /**
         * 
         * 
         * @param id
         * @throws IOException
         */
        public void deviceServer(int id) throws IOException {

            while (true) {
                System.out.print("");
                if (newDevice) {
                    out.println("device");
                    System.out.println("Try to make a new device");
                    newDevice = false;
                }
            }
        }

        /**
         * This method contains all the activities that the device thread needs to
         * perform while its client is connected to the system. It contains an infinite
         * while loop that will terminate when this server reads a null from the
         * buffered reader for this socket
         * 
         * @param id
         * @throws IOException
         */
        public void device(int id) throws IOException {
            String input = "";
            long timer;
            Boolean status = userDeviceStatuses.get(id);
            Boolean queried = false;
            Boolean statusChange = false;
            // the following variables are for timer offset operations
            offSetWasSet.put(id, false);
            allOffSet.put(id, 0);
            sendOffset.put(id, false);
            System.out.println("BEFORE THE ADD");
            IDS.add(id);
            System.out.println("AFTER IT");
            while (true) {
                timer = System.nanoTime();
                if ((timer / SECONDS) % QUERYTIME + 2 > QUERYTIME) { // query once only within the timer window, if
                                                                     // queried already, skip.
                    if (!queried) {
                        out.println("" + id);
                        input = in.readLine();

                        if (input == null || !input.equals("" + id)) {
                            break;
                        }
                        input = in.readLine();
                        if (input == null) {
                            break;
                        }
                        queried = true;
                        input = in.readLine();
                        // calculateOffSet(input, id);

                        // print the info to the database log
                        if (statusChange) {
                            statusChange = false;
                            System.out.println(input);
                            String helper1[] = input.split(", ");
                            if (helper1[1].substring(helper1[1].lastIndexOf(" ") + 1).equals("true")) {
                                input = helper1[0] + ", " + helper1[1].substring(0, helper1[1].indexOf(" ")) + " "
                                        + moniker.get(id) + helper1[1].substring(helper1[1].indexOf(" ") + 11,
                                                helper1[1].lastIndexOf(" "))
                                        + " " + IdTrue.get(id);
                            } else {
                                input = helper1[0] + ", " + helper1[1].substring(0, helper1[1].indexOf(" ")) + " "
                                        + moniker.get(id) + helper1[1].substring(helper1[1].indexOf(" ") + 11,
                                                helper1[1].lastIndexOf(" "))
                                        + " " + IdFalse.get(id);
                            }
                            dbLogs.add(input);
                        }
                    }
                } else {
                    queried = false;
                }
                /* force the outlet to always be set to what is stored in shared memory */
                if (status != userDeviceStatuses.get(id)) {
                    out.println(id + "," + userDeviceStatuses.get(id));
                    status = userDeviceStatuses.get(id);
                    statusChange = true;
                }
                // if the user is adding a new device, print to device server
            }
        }// end device thread method

    } // end thread handler
}// end Gateway Server Class
