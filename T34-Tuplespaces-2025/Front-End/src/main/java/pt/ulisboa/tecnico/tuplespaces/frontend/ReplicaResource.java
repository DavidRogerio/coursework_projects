package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2Grpc;

public class ReplicaResource {
    public final int serverId;
    public final String target;
    public final TupleSpacesB2Grpc.TupleSpacesB2Stub asyncStub;
    private final ManagedChannel channel;

    public ReplicaResource(int serverId, String target) {
        this.serverId = serverId;
        this.target = target;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.asyncStub = TupleSpacesB2Grpc.newStub(channel);
    }

    public void shutdown() {
        channel.shutdownNow();
    }

    public int getServerId() {
        return this.serverId;
    }
}
