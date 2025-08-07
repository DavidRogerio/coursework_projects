package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2Grpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2OuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import static pt.ulisboa.tecnico.tuplespaces.server.ServerDebug.debug;

import java.util.regex.PatternSyntaxException;
import java.util.List;
    
public class TupleSpacesServiceImpl extends TupleSpacesB2Grpc.TupleSpacesB2ImplBase {


    private ServerState server_state = new ServerState();

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver){
        int client_id=-1;
        try {
            client_id = Integer.parseInt(ServerMetadataInterceptor.getClientIdFromContext());
            int seqId = Integer.parseInt(ServerMetadataInterceptor.getSeqIdFromContext());
            int delay = Integer.parseInt(ServerMetadataInterceptor.getDelayFromContext());
            debug(String.format("[ServerImpl] Client %d -> PUT: %s | Delay: %d", client_id, request.getNewTuple(), delay));
            if (delay > 0) Thread.sleep(delay * 1000);

            server_state.put(request.getNewTuple(), client_id, seqId);

            debug(String.format("[ServerImpl] Client %d -> PUT completed: %s", client_id, request.getNewTuple()));
            debug("\n -------------------------------  FIM   PUT  -----------------------------\n");


            responseObserver.onNext(PutResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch(Exception exc){
            debug(String.format("[ServerImpl] ERROR on PUT | Client %d | Tuple: %s | %s", client_id, request.getNewTuple(), exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver){
        int client_id=-1;
        try {
            client_id = Integer.parseInt(ServerMetadataInterceptor.getClientIdFromContext());
            int seqId = Integer.parseInt(ServerMetadataInterceptor.getSeqIdFromContext());
            int delay = Integer.parseInt(ServerMetadataInterceptor.getDelayFromContext());
            debug(String.format("[ServerImpl] Client %d -> READ pattern: %s | Delay: %d", client_id, request.getSearchPattern(), delay));
            if (delay > 0) Thread.sleep(delay * 1000);

            String match = server_state.read(request.getSearchPattern(), client_id, seqId);

            
            debug("\n ------------------------------- FIM   READ -------------------------------\n");

            responseObserver.onNext(ReadResponse.newBuilder().setResult(match).build());
            responseObserver.onCompleted();
        } catch(PatternSyntaxException exc){
            debug(String.format("[ServerImpl] ERROR on READ | Client %d | Invalid pattern: %s | %s", client_id, request.getSearchPattern(), exc));
            responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
        } catch(Exception exc){
            debug(String.format("[ServerImpl] ERROR on READ | Client %d | Pattern: %s | %s", client_id, request.getSearchPattern(), exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver){
        int client_id=-1;
        try {
            client_id = Integer.parseInt(ServerMetadataInterceptor.getClientIdFromContext());
            int seqId = Integer.parseInt(ServerMetadataInterceptor.getSeqIdFromContext());
            int delay = Integer.parseInt(ServerMetadataInterceptor.getDelayFromContext());
            debug(String.format("[ServerImpl] Client %d -> TAKE pattern: %s | Delay: %d", client_id, request.getSearchPattern(), delay));
            if (delay > 0) Thread.sleep(delay * 1000);

            String match = server_state.take(request.getSearchPattern(), client_id,seqId);
            debug("\n ------------------------------- FIM   TAKE -------------------------------\n");

            responseObserver.onNext(TakeResponse.newBuilder().setResult(match).build());
            responseObserver.onCompleted();
        } catch(PatternSyntaxException exc){
            debug(String.format("[ServerImpl] ERROR on TAKE | Client %d | Invalid pattern: %s | %s", client_id, request.getSearchPattern(), exc));
            responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
        } catch(Exception exc){
            debug(String.format("[ServerImpl] ERROR on TAKE | Client %d | Pattern: %s | %s", client_id, request.getSearchPattern(), exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        int client_id=-1;
        try {
            client_id = Integer.parseInt(ServerMetadataInterceptor.getClientIdFromContext());
            int seqId = Integer.parseInt(ServerMetadataInterceptor.getSeqIdFromContext());
            debug(String.format("[ServerImpl] Client %d -> GET STATE", client_id));

            getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder()
                    .addAllTuple(server_state.getTupleSpacesState(client_id, seqId))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(Exception exc){
            debug(String.format("[ServerImpl] ERROR on GET STATE | Client %d | %s", client_id, exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        int client_id = request.getClientId();
        String regex = request.getRegex();

        try {
            debug(String.format("[ServerImpl] Received requestVote() from Client %d | Regex: %s", client_id, regex));
            
            List<String> matches = server_state.requestVote(client_id, regex);
            boolean granted = !matches.isEmpty();

            RequestVoteResponse response = RequestVoteResponse.newBuilder().setGranted(granted).addAllMatches(matches).build();

            debug("\n ---------------------------- FIM RESQUEST VOTE --------------------------- \n");

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (PatternSyntaxException e) {
            debug(String.format("[ServerImpl] ERROR on requestVote() | Client %d | Invalid pattern: %s", client_id, regex));
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Invalid regex pattern").asRuntimeException());
        } catch (Exception exc) {
            debug(String.format("[ServerImpl] ERROR on requestVote() | Client %d | %s", client_id, exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }


    @Override
    public void releaseVote(ReleaseVoteRequest request, StreamObserver<ReleaseVoteResponse> responseObserver) {
        int client_id = request.getClientId();
        try {
            debug(String.format("[ServerImpl] Received releaseVote() from Client %d", client_id));
            server_state.releaseVote(client_id);

            debug("\n ---------------------------- FIM RELEASE VOTE ---------------------------- \n");

            responseObserver.onNext(ReleaseVoteResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception exc) {
            debug(String.format("[ServerImpl] ERROR on releaseVote() | Client %d | %s", client_id, exc));
            responseObserver.onError(Status.UNKNOWN.asRuntimeException());
        }
    }
}