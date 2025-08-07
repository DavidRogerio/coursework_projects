package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import static pt.ulisboa.tecnico.tuplespaces.server.ServerDebug.debug;
import static pt.ulisboa.tecnico.tuplespaces.server.ServerDebug.setDebugFlag;

public class ServerMain {
    static boolean debug_flag;

    public static void main(String[] args) throws Exception {
        System.out.println(ServerMain.class.getSimpleName());

        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        if (args.length < 1 || args.length > 2) {
            System.err.println("Bad Argument(s)!");
            System.err.printf("Usage: mvn exec:java -Dexec.args=\"3001\" [-debug]");
            return;
        }

        if (args.length == 2 && args[1].equals("-debug")) {
            setDebugFlag(true);
        }

        final int port = Integer.parseInt(args[0]);
        final int serverId = (port - 3001); //  3001 -> id 0

        final BindableService impl = new TupleSpacesServiceImpl();

        Server server = ServerBuilder.forPort(port)
                .addService(impl)
                .intercept(new ServerMetadataInterceptor(serverId)) 
                .build();

        server.start();

        debug("Server started on port " + port + " with serverId " + serverId);

        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                server.shutdown();
                debug(System.lineSeparator() + "Shutdown");
            })
        );

        server.awaitTermination();
    }
}
