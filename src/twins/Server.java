package twins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {

    private final int port;
    private Writer writer;
    private String TCPstate;

    /**
     * Initialise a new Twins server. To start the server, call start().
     *
     * @param port the port number on which the server will listen for
     * connections
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * Start the server.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server listening on port: " + port);
                Socket conn = serverSocket.accept();
                System.out.println("Connected to " + conn.getInetAddress() + ":" + conn.getPort());
                session(conn);
            }
        }
    }

    /**
     * Run a Twins protocol session over an established network connection.
     *
     * @param connection the network connection
     * @throws IOException
     */
    public void session(Socket connection) throws IOException {
        writer = new OutputStreamWriter(connection.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        //  CW specifies a persistent database and that is platform indepedent and created during code
        File file = new File("TwinsDatabase.txt");
        String initialMessage;
        String name = null;
        String date = null;
        int day = 0;
        int month = 0;
        TCPstate = "NEW";

        while ((initialMessage = reader.readLine()) != null) {
            if (initialMessage.trim().toLowerCase().equals("hello")) {
                while (true) {
                    while (true) {
                        switch (TCPstate) {
                            case "NEW":
                                sendMessage("What is your name?");
                                TCPstate = "RECEIVE_NAME";
                                break;
                            case "RECEIVE_NAME":
                                name = reader.readLine().trim();
                                if ( (stringContainsNumber(name) == true) || name.toLowerCase().equals("begin twins") || name.toLowerCase().equals("end twins"))
                                {
                                 sendMessage("Error 1");
                                 connection.close();  
                                }else {
                                boolean found = false;
                                try {
                                    File checkEmpty = new File("TwinsDatabase.txt");
                                    // If the document is empty
                                    if (checkEmpty.length() == 0) {
                                        sendMessage("When were you born?");
                                        TCPstate = "RECEIVE_DATE";
                                    } else {
                                        // If the document is not empty
                                        String tmp[] = null;
                                        Scanner filez = new Scanner(new File("TwinsDatabase.txt"));
                                        while (filez.hasNextLine()) {
                                            final String lineFromFile = filez.nextLine();
                                            if (lineFromFile.contains(name)) {
                                                tmp = lineFromFile.split("\t");
                                                found = true;
                                                // System.out.println(Arrays.asList(tmp).contains(name));
                                            }
                                        }
                                        //Arrays.asList(tmp).contains(name)
                                        if (found == true) {
                                            date = retrieveDate(name);
                                            String[] dateChecker = date.split(":", 3);
                                            day = Integer.parseInt(dateChecker[0]);
                                            month = Integer.parseInt(dateChecker[1]);
                                            displayTwins(name, day, month);
                                            TCPstate = "RECEIVE_REQ";
                                        } else {
                                            sendMessage("When were you born?");
                                            TCPstate = "RECEIVE_DATE";
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                }
                                break;
                            case "RECEIVE_DATE":
                                // or, if <name> is not already registered:
                                date = reader.readLine();
                                boolean dateCheck = checkDateFormat(date);
                                if (dateCheck == true) {
                                    try (Writer writer = new BufferedWriter(new FileWriter(file, true))) {
                                        String[] dateChecker = date.split(":", 3);
                                        day = Integer.parseInt(dateChecker[0]);
                                        month = Integer.parseInt(dateChecker[1]);
                                        int year = Integer.parseInt(dateChecker[2]);
                                        String content = name + "\t" + day + ":" + month + ":" + year;
                                        writer.write(content + "\r\n");
                                        displayTwins(name, day, month);
                                        TCPstate = "RECEIVE_REQ";
                                        break;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    sendMessage("Error 2");
                                    connection.close();
                                }
                                break;
                            case "RECEIVE_REQ":
                                // Alternatives chosen by client:
                                options:
                                while (true) {
                                    String options = reader.readLine().trim().toLowerCase();
                                    switch (options) {
                                        case "quit":
                                            connection.close();
                                            break options;
                                        case "refresh":
                                            displayTwins(name, day, month);
                                            TCPstate = "RECEIVE_REQ";
                                            break options;
                                        case "delete me":
                                            removeLine(name);
                                            connection.close();
                                            break options;
                                        default:
                                            sendMessage("Error 0");
                                            connection.close();
                                    }
                                    break;
                                }
                                break;
                        }
                    }
                }
            }
        }
    }
    public boolean stringContainsNumber(String s) {
        Pattern p = Pattern.compile("[0-9]");
        Matcher m = p.matcher(s);
        return m.find();
    }
    private String retrieveDate(String name) throws IOException {
        String dateOfUser;
        String tmp[] = null;
        Scanner filez = new Scanner(new File("TwinsDatabase.txt"));
        while (filez.hasNextLine()) {
            final String lineFromFile = filez.nextLine();
            if (lineFromFile.contains(name)) {
                tmp = lineFromFile.split("\t");
                dateOfUser = tmp[1];
                return dateOfUser;
            }
        }
        return null;
    }
    
    private boolean checkDateFormat(String date) throws IOException {
        try {
            if (date.matches("^[0-9:]+$")) {
                String[] dateChecker = date.split(":", 3);
                int day = Integer.parseInt(dateChecker[0]);
                int month = Integer.parseInt(dateChecker[1]);
                int year = Integer.parseInt(dateChecker[2]);
                if (year >= 1900 && year <= 2019) {
                    if (month >= 1 && month <= 12) {
                        if (month % 2 == 0 && month != 2 && month != 8) {
                            if (day >= 1 && day <= 30) {
                                return true;
                            } else {
                            return false;
                            }
                        } else if (month == 2) {
                            if (day >= 1 && day <= 29) {
                                return true;
                            } else {
                             return false;
                            }
                        } else if (month == 8) { // This is necessary as August have 31 days even though it's a multiple of 2
                            if (day >= 1 && day <= 31) {
                                return true;
                            } else {
                            return false;
                            }
                        } else {
                            if (day >= 1 && day <= 31) {
                                return true;
                            } else {
                            return false;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void displayTwins(String name, int day, int month) throws IOException {
        String tmp[] = null;
        Scanner filez = new Scanner(new File("TwinsDatabase.txt"));

        sendMessage("BEGIN TWINS");
        while (filez.hasNextLine()) {
            final String lineFromFile = filez.nextLine();
            if (!lineFromFile.contains(name) && (lineFromFile.contains(String.valueOf(day)) && lineFromFile.contains(String.valueOf(month)))) {
                tmp = lineFromFile.split("\t");
                sendMessage(tmp[0]);
            }
        }
        sendMessage("END TWINS");
    }

    public void removeLine(String lineContent) throws IOException {
        File file = new File("TwinsDatabase.txt");
        List<String> out = Files.lines(file.toPath())
                .filter(line -> !line.contains(lineContent))
                .collect(Collectors.toList());
        Files.write(file.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Send a newline-terminated message on the output stream to the client.
     *
     * @param msg the message to send, not including the newline
     * @throws IOException
     */
    private void sendMessage(String msg) throws IOException {
        writer.write(msg.toString());
        writer.write("\r\n"); // Windows! Differ to Linux's \n
        // this flush() is necessary, otherwise ouput is buffered locally and
        // won't be sent to the client until it is too late 
        writer.flush();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String usage = "Usage: java twins.Server [<port-number>] ";
        if (args.length > 1) {
            throw new Error(usage);
        }
        int port = 8123;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println("Server host: " + ip.getHostAddress() + " (" + ip.getHostName() + ")");
        } catch (IOException e) {
            System.err.println("could not determine local host name");
        }
        Server server = new Server(port);
        server.start();
        System.err.println("Server loop terminated!"); // not supposed to happen
    }
}
