package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import static pt.ulisboa.tecnico.tuplespaces.client.ClientDebug.debug;

public class ClientMain {

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        if (3 < args.length || args.length < 2) {
            System.err.println("Bad Argument(s)!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<server.host>:<server.port> <client_id> [-debug]");
            return;
        }

        if (args.length==3 && args[2].equals("-debug"))
            ClientDebug.setDebugFlag(true);

        // receive and print arguments
        debug(String.format("Received %d arguments%n", args.length));
        for (int i = 0; i < args.length; i++) {
             debug(String.format("arg[%d] = %s%n", i, args[i]));
        }
        // get the host and the port of the server or front-end
        final String host_port = args[0];
        final int client_id = Integer.parseInt(args[1]) ;     

        CommandProcessor parser = new CommandProcessor(new ClientService(host_port, client_id));
        parser.parseInput();

    }
}