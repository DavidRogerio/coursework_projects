package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesBlockingStub;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import static pt.ulisboa.tecnico.tuplespaces.client.ClientDebug.debug;

import java.util.List;

public class ClientService {
    private final int client_id;
    private final ManagedChannel channel;

    public ClientService(String host_port, int client_id) {
        this.client_id = client_id;
        this.channel = ManagedChannelBuilder.forTarget(host_port)
                .usePlaintext()
                .build();
    }

    /*
     * Sends a put() request to the front-end to insert the specified tuple.
     * This operation adds the tuple to the replicated tuple space.
     * 
     * If a delays array is provided, it will apply artificial delays (in seconds)
     * to each replica before processing the request.
     * 
     * @param tuple The tuple to be inserted, in the format "<field1,field2,...>".
     * @param delays An array of 3 integers representing delays for replicas 0, 1, and 2.
     * @return true if the operation was successful, false if an error occurred at the front-end or any replica.
     */
    public boolean put(String tuple, Integer[] delays) {
        debug(String.format("Client %d: put(%s) delays=%s", client_id, tuple, java.util.Arrays.toString(delays)));
    
        try {
            getBlockingStub(delays).put(PutRequest.newBuilder().setNewTuple(tuple).build());
            return true;
        } catch (StatusRuntimeException e) {
            System.err.println(e.getStatus().getDescription() + System.lineSeparator());
            return false;
        }
    }

    // Overload with default delays of 0s.
    public boolean put(String tuple) {
        return put(tuple, new Integer[]{0, 0, 0});
    }

    /**
     * Sends a read() request to the front-end to search for a tuple matching the provided pattern.
     * This operation will return one matching tuple, if available, without removing it from the tuple space.
     * The client will block until a matching tuple is found.
     * 
     * Optional delays (in seconds) can be applied to replicas during processing.
     * 
     * @param pattern The search pattern (can be a regular expression) used to match tuples.
     * @param delays An array of 3 integers representing delays for replicas 0, 1, and 2.
     * @return The first matching tuple found, or null if an error occurred.
     */
    public String read(String pattern, Integer[] delays) {
        debug(String.format("Client %d: read(%s) delays=%s", client_id, pattern, java.util.Arrays.toString(delays)));
        try {
            ReadResponse response = getBlockingStub(delays).read(ReadRequest.newBuilder().setSearchPattern(pattern).build());
            return response.getResult();
        } catch (StatusRuntimeException e) {
            System.err.println(e.getStatus().getDescription() + System.lineSeparator());
            return null;
        }
    }

    public String read(String pattern) {
        return read(pattern, new Integer[]{0, 0, 0});
    }


    /**
     * Sends a take() request to the front-end to search and remove a tuple matching the provided pattern. 
     * Optional delays (in seconds) can be applied to replicas during processing.
     * 
     * @param pattern The search pattern 
     * @param delays An array of 3 integers representing delays for replicas 0, 1, and 2.
     * @return The tuple that was removed, or null if an error occurred.
     */
    public String take(String pattern, Integer[] delays) {
        debug(String.format("Client %d: take(%s) delays=%s", client_id, pattern, java.util.Arrays.toString(delays)));
        try {
            TakeResponse response = getBlockingStub(delays).take(TakeRequest.newBuilder().setSearchPattern(pattern).build());
            return response.getResult();
        } catch (StatusRuntimeException e) {
            System.err.println(e.getStatus().getDescription() + System.lineSeparator());
            return null;
        }
    }

    public String take(String pattern) {
        return take(pattern, new Integer[]{0, 0, 0});
    }


    /**
     * Sends a getTupleSpacesState() request to retrieve the current state (all tuples)
     * from the replicated tuple space servers.
     * 
     * Optional delays (in seconds) can be applied to replicas during processing.
     * 
     * @param delays An array of 3 integers representing delays for replicas 0, 1, and 2.
     * @return A list of all tuples from the tuple space, or null if an error occurred.
     */
    public List<String> getTupleSpacesState(Integer[] delays) {
        debug(String.format("Client %d: getTupleSpacesState() delays=%s", client_id, java.util.Arrays.toString(delays)));
        try {
            getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();

            getTupleSpacesStateResponse response = getBlockingStub(delays)
                .getTupleSpacesState(request);

            return response.getTupleList();
        } catch (StatusRuntimeException e) {
            System.err.println(e.getStatus().getDescription() + System.lineSeparator());
            return null;
        }
    }

    public List<String> getTupleSpacesState() {
        return getTupleSpacesState(new Integer[]{0, 0, 0});
    }

    
    
    public void shutdown() {
        channel.shutdown();
    }

    /**
     * Creates a new blocking stub for communicating with the front-end,
     * attaching gRPC metadata containing the client ID and delays for each replica.
     * 
     * The delays array should contain 3 elements, each representing the artificial delay (in seconds)
     * to be applied by replica 0, 1, and 2, respectively.
     * 
     * @param delays An array of 3 integers for delays (replica0, replica1, replica2).
     * @return A blocking stub with metadata attached.
     */
    private TupleSpacesBlockingStub getBlockingStub(Integer[] delays) {
        Metadata metadata = new Metadata();
        Metadata.Key<String> clientIdKey = Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(clientIdKey, String.valueOf(client_id));
    
        for (int i = 0; i < delays.length; i++) {
            Metadata.Key<String> delayKey = Metadata.Key.of("delay" + (i + 1), Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(delayKey, String.valueOf(delays[i]));
        }
    
        return TupleSpacesGrpc.newBlockingStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }
    
}