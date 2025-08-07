package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import static pt.ulisboa.tecnico.tuplespaces.frontend.FrontEndDebug.*;

import java.util.ArrayList;
import java.util.List;


public class FrontEndMain {

	static boolean debug_flag;

    public static void main(String[] args) throws Exception {
      System.out.println(FrontEndMain.class.getSimpleName());

	  	// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 4 || args.length > 5) {
            System.err.println("Bad Argument(s)!");
			System.err.printf("Usage: mvn exec:java -Dexec.args=\" 2001 <server1_host_port> <server2_host_port> <server3_host_port> [-debug]\"");
			return;
		}

		if (args.length==5 && args[4].equals("-debug")){
			setDebugFlag(true);
		}

		// receive and print arguments
		debug(String.format("Received %d arguments%n", args.length));
		for (int i = 0; i < args.length; i++) {
			debug(String.format("arg[%d] = %s%n", i, args[i]));
		}

		final int port = Integer.parseInt(args[0]);

		List<ReplicaResource> replicasInfo = new ArrayList<>();
		replicasInfo.add(new ReplicaResource(0, args[1]));
		replicasInfo.add(new ReplicaResource(1, args[2]));
		replicasInfo.add(new ReplicaResource(2, args[3]));

		final BindableService impl = new FrontEndServiceImp(replicasInfo);

		Server server = ServerBuilder.forPort(port)
				.addService(impl)
				.intercept(new MetadataInterceptor())
				.build();

		server.start();
		debug("FrontEndServer started");

		Runtime.getRuntime().addShutdownHook(
			new Thread(() -> {
				server.shutdown();
				debug(String.format(System.lineSeparator() + "Shutdown"));
			})
		);
		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();
    }

}