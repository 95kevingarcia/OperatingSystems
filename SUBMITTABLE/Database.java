package database;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author kingkev
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Database {

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("FieldMayBeFinal")
    private static Boolean firstSheetDeleted = false;
    private static String name = "hi";
    private static String outputFilePath = "";
    private static int delay = 5;
    private static boolean fileCreated = false;
    private static ArrayList<String> output = new ArrayList<String>();
    // private static final String GATEWAY_IP = "192.168.1.242"; // Kierstin at
    // Kevin's
    // private static final String GATEWAY_IP = "192.168.1.239"; // Eric at Kevin's
    private static final String GATEWAY_IP = "10.108.155.180"; // Kierstin at Kierstin's
    private static final double SECONDS = 1000000000.0;
    private static final int DBRATE = 1;
    private static final int QUIT = 6000;

    // socket constructor takes port as int
    private static final int PORT = 8080;
    private static Socket socket;
    // Streams for communicating with the gateway server
    private static BufferedReader in;
    private static PrintWriter out;
    // used to store wether the file creation is successful
    private static BufferedWriter writer;
    // private static PrintWriter writer;
    private static File file;
    // arraylist to store what will be printed to the log file by bufferedwriter

    public static void main(String[] args) throws FileNotFoundException, InvalidFormatException, InvalidFormatException,
            InterruptedException, InterruptedException, InterruptedException, InterruptedException, IOException,
            InterruptedException, InterruptedException {
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
            // System.out.println("LOOP");
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
                            read(receivedSplit[1]); // search database
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
                    // System.out.println("MAIN");
                    write();
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
                    write();
                } catch (IOException e) {
                    System.out.println("There was an error writing to log file.");
                    System.out.println("Error occured: " + e);
                }
                start = System.nanoTime(); // restart the timer
            }
        }
        System.out.println("Finished Writing to file.");
    }

    @SuppressWarnings("ConvertToTryWithResources")
    private static void write()
            throws IOException, FileNotFoundException, InvalidFormatException, InterruptedException {
        // System.out.println("WRITE");
        if (!fileCreated) {
            System.out.println("CREATE");
            fileCreated = true;
            createFile();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -delay);
        InputStream databaseFile = new FileInputStream(outputFilePath);
        Workbook wb = WorkbookFactory.create(databaseFile);
        Sheet sheet;
        String helper[];
        String time, type, info, id;
        Row row;
        for (int i = (output.size() - 1); i >= 0; i--) {
            System.out.println("i=" + i);
            if (output.get(i).contains(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(calendar.getTime()))) {
                // System.out.println("INSIDE");
                System.out.println(output.get(i));
                helper = output.get(i).split(", ");
                time = helper[0];
                helper = helper[1].split(": ");
                id = helper[1].substring(0, helper[1].indexOf(" "));
                type = helper[0] + " " + id;
                info = helper[1].substring(helper[1].lastIndexOf(" ") + 1);

                if (!firstSheetDeleted) {
                    wb.removeSheetAt(0);
                    firstSheetDeleted = true;
                }
                sheet = wb.getSheet(type);
                if (sheet == null) {
                    sheet = wb.createSheet(type);
                    Row r = sheet.createRow(0);
                    Cell c = r.createCell(0);
                    c.setCellValue("Time");
                    c = r.createCell(1);
                    c.setCellValue("ID");
                    c = r.createCell(2);
                    c.setCellValue("Value");
                }
                row = sheet.createRow(sheet.getPhysicalNumberOfRows());
                row.createCell(0).setCellValue(time);
                row.createCell(1).setCellValue(id);
                row.createCell(2).setCellValue(info);
                output.remove(i);
            }
        }
        try (FileOutputStream fos = new FileOutputStream(new File(outputFilePath))) {
            wb.write(fos);
        } catch (IOException e) {
        }
        wb.close();
        databaseFile.close();
    }

    private static void read(String input) throws FileNotFoundException, IOException, InvalidFormatException {
        System.out.println("READ");
        Boolean found = false;
        InputStream databaseFile = new FileInputStream(outputFilePath);
        Workbook wb = WorkbookFactory.create(databaseFile);
        Sheet sheet;
        Row row;
        Cell cell;
        DataFormatter formatter = new DataFormatter();
        for (int j = 0; j < wb.getNumberOfSheets(); j++) {
            sheet = wb.getSheetAt(j);
            System.out.println("SHEETS " + j);
            if (sheet != null) {
                for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                    System.out.println("ROW " + i);
                    row = sheet.getRow(i);
                    cell = row.getCell(0);
                    if (formatter.formatCellValue(cell).contains(input)) {
                        if (!found) {
                            found = true;
                        }
                        cell = row.getCell(2);
                        out.println("u");
                        out.println(wb.getSheetName(j) + " with value " + formatter.formatCellValue(cell));
                        System.out.println(wb.getSheetName(j) + " with value " + formatter.formatCellValue(cell));
                    }
                }
            }
        }
        if (!found) {
            out.println(input + " Was not found");
        }
        out.println("-1");
        wb.close();
    }

    private static void createFile()
            throws FileNotFoundException, IOException, InvalidFormatException, InterruptedException {
        String date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        date = date.replace("/", "-");
        date = date.replace(" ", "");
        date = date.substring(0, date.lastIndexOf("-") + 5) + " " + date.substring(date.lastIndexOf("-") + 5);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(name);
        outputFilePath = date + ".xlsx";
        System.out.println(outputFilePath);
        try (FileOutputStream fos = new FileOutputStream(new File(outputFilePath))) {
            System.out.println("ERIC");
            wb.write(fos);
        } catch (IOException e) {
        }
        wb.close();
        System.out.println("FILE WAS CREATED");
    }

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
