package server;

import common.requestresponse.*;
import server.database.DBManager;
import common.IO.IOManager;
import common.commands.Command;
import common.databaseexceptions.DatabaseException;
import common.collectionexceptions.CollectionException;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class ServerService {
    private static String host;

    private static DatagramSocket socket = null;
    private static DatagramPacket packet = null;
    private static Application application;
    private static DBManager dbManager;
    private static final IOManager ioManager = new IOManager();

    public static void main(String[] callArgs) {
        System.out.println(duck);
        System.out.println(please);

//        try {
//            Class.forName("org.postgresql.Driver");
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

        runServerMainLoop();
    }

    private static void runServerMainLoop() {
        boolean isRunning = true;
        try {
            startServer();
            application = new Application(dbManager);
            application.loadCollection();
            ioManager.printlnStatus("Collection has been loaded");
            while (isRunning) {
                try {
                    packet = receivePacket();
                } catch (SocketTimeoutException ignored) {}

                if (packet == null || packet.getPort() < 0) {
                    continue;
                }

                InetSocketAddress clientSocketAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                Request request = getRequest(packet);
                ioManager.printlnStatus("\n" + request.toString() + "\n");

                if (request.getType() == RequestType.CONNECT) {
                    confirmConnect(clientSocketAddress);
                    continue;
                }
                if (request.getType() == RequestType.REGISTER) {
                    Session session = request.getSession();
                    registerUser(
                            session.getUsername(),
                            session.getPassword(),
                            clientSocketAddress
                    );
                    continue;
                }
                if (request.getType() == RequestType.LOGIN) {
                    Session session = request.getSession();
                    loginUser(
                            session.getUsername(),
                            session.getPassword(),
                            clientSocketAddress
                    );
                    continue;
                }
                if (request.getType() == RequestType.EXECUTE_COMMAND) {
                    Session session = request.getSession();
                    executeCommand(
                            request.getCommand(),
                            request.getArgs(),
                            session.getUsername(),
                            session.getPassword(),
                            clientSocketAddress
                    );
                }

            }
        } catch (NoSuchElementException e) {
            System.exit(1337);
        } catch (Exception e) {
            e.printStackTrace();
            ioManager.printlnOut(fatalError);
            System.exit(exitCode);
        }
    }

    private static void confirmConnect(InetSocketAddress clientSocketAddress) throws IOException {
        Response response = new Response(ResponseType.SUCCESS, "Connection established");
        sendResponse(response, clientSocketAddress);
    }

    /*______________________________________________________________________________________________________________*/


    private static void registerUser(String user, String password, InetSocketAddress clientSocketAddress) throws IOException {
        try {
            dbManager.register(user, password);
            Response response = new Response(ResponseType.SUCCESS, "User '" + user + "' has been registered");
            sendResponse(response, clientSocketAddress);
        } catch (DatabaseException e) {
            Response response = new Response(ResponseType.ERROR, e.getMessage());
            sendResponse(response, clientSocketAddress);
        } catch (SQLException e) {
            e.printStackTrace();
            Response response = new Response(ResponseType.ERROR, "Could not access database");
            sendResponse(response, clientSocketAddress);
        }
    }

    private static void loginUser(String user, String password, InetSocketAddress clientSocketAddress) throws IOException {
        try {
            dbManager.login(user, password);
            Response response = new Response(ResponseType.SUCCESS, "Logged in successfully");
            sendResponse(response, clientSocketAddress);
        } catch (DatabaseException e) {
            Response response = new Response(ResponseType.ERROR, e.getMessage());
            sendResponse(response, clientSocketAddress);
        } catch (SQLException e) {
            e.printStackTrace();
            Response response = new Response(ResponseType.ERROR, "Could not access database");
            sendResponse(response, clientSocketAddress);
        }
    }

    private static void executeCommand(
            Command command, Object[] cmdArgs, String username, String password, InetSocketAddress clientSocketAddress)
            throws IOException {

        if (!validateLogin(username, password)) {
            Response response = new Response(ResponseType.ERROR, "Access denied: user must be logged in to execute commands");
            sendResponse(response, clientSocketAddress);
            return;
        }
        try {
            String exitMessage = application.executeCommand(command, cmdArgs, username);
            Response response = new Response(ResponseType.SUCCESS, exitMessage);
            sendResponse(response, clientSocketAddress);
        } catch (CollectionException e) {
            Response response = new Response(ResponseType.ERROR, e.getMessage(), ExceptionType.EXECUTE_COMMAND_EXCEPTION);
            sendResponse(response, clientSocketAddress);
        }
    }

    private static void startServer() {
        int port;
        while (true) {
            port = ioManager.getNextInteger("Specify port: ", false);
            if (port < 0) {
                ioManager.printlnErr("Incorrect input. Port must be positive integer");
                continue;
            }
            try {
                socket = new DatagramSocket(port);
//                socket.setSoTimeout(5000);
                ioManager.printlnSuccess("Server has been started on port " + port);
                break;
            } catch (SocketException e) {
                ioManager.printlnErr("Incorrect input. Try again");
            }
        }
        int hostNum;
        String host;
        String user;
        String password;
        while (true) {
            hostNum = ioManager.getNextInteger(
                    "Choose database to connect to ([1] helios [2] localhost): ", false);
            switch (hostNum) {
                case 1:
                    host = "jdbc:postgresql://pg:5432/studs";
                    user = "s335151";
                    password = ioManager.getNextPassword();
                    break;
                case 2:
                    host = "localhost";
                    user = "postgres";
                    password = "gfhjkm";
                    break;
                default:
                    continue;
            }
            try {
                dbManager = new DBManager(host, user, password);
                break;
            } catch (SQLException e) {
                e.printStackTrace();
                ioManager.printlnErr("Error while connecting database. Try again");
            }
        }

    }

    private static DatagramPacket receivePacket() throws IOException {
        if (socket == null) throw new SocketException("Socket and/or packet has not been initialised yet");
        byte[] bytes = new byte[4096];
        packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);
        return packet;
    }

    private static boolean validateLogin(String user, String password) {
        try {
            dbManager.login(user, password);
            return true;
        } catch (SQLException | DatabaseException e) {
            return false;
        }
    }

    private static Request getRequest(DatagramPacket packet) throws IOException, ClassNotFoundException {
        ObjectInputStream bytesToRequestStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
        return (Request) bytesToRequestStream.readObject();
    }

    private static void sendResponse(Response response, InetSocketAddress clientSocketAddress) throws IOException {
        ioManager.printlnYellow(response.toString());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream responseToBytes = new ObjectOutputStream(byteArrayOutputStream);
        responseToBytes.writeObject(response);
        packet = new DatagramPacket(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size(), clientSocketAddress);
        socket.send(packet);
        packet = null;
    }


    /* _________________________________________________END_________________________________________________ */


    /* _________________________________You are not supposed to be here...__________________________________ */


    private static final String duck =
            "\n\n\n\n" +
                    "                                    ██████        \n" +
                    "                                  ██      ██      \n" +
                    "                                ██          ██    \n" +
                    "                                ██      ██  ██    \n" +
                    "                                ██        ░░░░██  \n" +
                    "                                  ██      ████    \n" +
                    "                    ██              ██  ██        \n" +
                    "                  ██  ██        ████    ██        \n" +
                    "                  ██    ████████          ██      \n" +
                    "                  ██                        ██    \n" +
                    "                    ██                      ██    \n" +
                    "                    ██    ██      ████      ██    \n" +
                    "                     ██    ████████      ██       \n" +
                    "                      ██                  ██      \n" +
                    "                        ████          ████        \n" +
                    "                            ██████████            \n" +
                    "\n\n";
    private static final String please =
            "\n\n\n\n" +
                    "██████╗░███████╗░░░░░░░░░██████╗░███████╗███╗░░██╗████████╗██╗░░░░░███████╗\n" +
                    "██╔══██╗██╔════╝░░░░░░░░██╔════╝░██╔════╝████╗░██║╚══██╔══╝██║░░░░░██╔════╝\n" +
                    "██████╦╝█████╗░░░░░░░░░░██║░░██╗░█████╗░░██╔██╗██║░░░██║░░░██║░░░░░█████╗░░\n" +
                    "██╔══██╗██╔══╝░░░░░░░░░░██║░░╚██╗██╔══╝░░██║╚████║░░░██║░░░██║░░░░░██╔══╝░░\n" +
                    "██████╦╝███████╗░░░░░░░░╚██████╔╝███████╗██║░╚███║░░░██║░░░███████╗███████╗\n" +
                    "╚═════╝░╚══════╝░░░░░░░░░╚═════╝░╚══════╝╚═╝░░╚══╝░░░╚═╝░░░╚══════╝╚══════╝\n" +
                    "\n" +
                    "██████╗░██╗░░░░░███████╗░█████╗░░██████╗███████╗░░░░░░░░░\n" +
                    "██╔══██╗██║░░░░░██╔════╝██╔══██╗██╔════╝██╔════╝░░░░░░░░░\n" +
                    "██████╔╝██║░░░░░█████╗░░███████║╚█████╗░█████╗░░░░░░░░░░░\n" +
                    "██╔═══╝░██║░░░░░██╔══╝░░██╔══██║░╚═══██╗██╔══╝░░░░░░░░░░░\n" +
                    "██║░░░░░███████╗███████╗██║░░██║██████╔╝███████╗██╗██╗██╗\n" +
                    "╚═╝░░░░░╚══════╝╚══════╝╚═╝░░╚═╝╚═════╝░╚══════╝╚═╝╚═╝╚═╝" +
                    "\n\n\n\n";
    private static final String fatalError =
                    "████████████████████████████████████████\n" +
                    "████████████████████████████████████████\n" +
                    "██████▀░░░░░░░░▀████████▀▀░░░░░░░▀██████\n" +
                    "████▀░░░░░░░░░░░░▀████▀░░░░░░░░░░░░▀████\n" +
                    "██▀░░░░░░░░░░░░░░░░▀▀░░░░░░░░░░░░░░░░▀██\n" +
                    "██░░░░░░░░░░░░░░░░░░░▄▄░░░░░░░░░░░░░░░██\n" +
                    "██░░░░░░░░░░░░░░░░░░█░█░░░░░░░░░░░░░░░██\n" +
                    "██░░░░░░░░░░░░░░░░░▄▀░█░░░░░░░░░░░░░░░██\n" +
                    "██░░░░░░░░░░████▄▄▄▀░░▀▀▀▀▄░░░░░░░░░░░██\n" +
                    "██▄░░░░░░░░░████░░░░░░░░░░█░░░░░░░░░░▄██\n" +
                    "████▄░░░░░░░████░░░░░░░░░░█░░░░░░░░▄████\n" +
                    "██████▄░░░░░████▄▄▄░░░░░░░█░░░░░░▄██████\n" +
                    "████████▄░░░▀▀▀▀░░░▀▀▀▀▀▀▀░░░░░▄████████\n" +
                    "██████████▄░░░░░░░░░░░░░░░░░░▄██████████\n" +
                    "████████████▄░░░░░░░░░░░░░░▄████████████\n" +
                    "██████████████▄░░░░░░░░░░▄██████████████\n" +
                    "████████████████▄░░░░░░▄████████████████\n" +
                    "██████████████████▄▄▄▄██████████████████\n" +
                    "████████████████████████████████████████\n" +
                    "████████████████████████████████████████\n" +
                    "Congrats! Server has died.";
    private static final int exitCode = 314159265; //zdec


}