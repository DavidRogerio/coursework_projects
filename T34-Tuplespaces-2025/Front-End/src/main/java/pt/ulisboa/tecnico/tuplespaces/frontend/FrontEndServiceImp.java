package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;


import java.util.List;
public class FrontEndServiceImp extends TupleSpacesGrpc.TupleSpacesImplBase {

    private final FrontEndService service;

    public FrontEndServiceImp(List<ReplicaResource> replicas){
        super();
        service = new FrontEndService(replicas);
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        int client_id = Integer.parseInt(MetadataInterceptor.getClientIdFromContext());
        int delay1 = Integer.parseInt(MetadataInterceptor.getDelay1FromContext());
        int delay2 = Integer.parseInt(MetadataInterceptor.getDelay2FromContext());
        int delay3 = Integer.parseInt(MetadataInterceptor.getDelay3FromContext());
        int[] delays = {delay1, delay2, delay3};

        try {
            responseObserver.onNext(PutResponse.newBuilder().build()); //responde logo ao cliente
            responseObserver.onCompleted();
            // "na back-end" processamos o pedido
            service.put(request, client_id, delays);
        } catch (StatusRuntimeException sre) {
            responseObserver.onError(sre.getStatus().withDescription(sre.getMessage()).asRuntimeException());
        } catch (Exception exc) {
            responseObserver.onError(Status.UNKNOWN.withDescription(exc.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        int client_id = Integer.parseInt(MetadataInterceptor.getClientIdFromContext());
        int delay1 = Integer.parseInt(MetadataInterceptor.getDelay1FromContext());
        int delay2 = Integer.parseInt(MetadataInterceptor.getDelay2FromContext());
        int delay3 = Integer.parseInt(MetadataInterceptor.getDelay3FromContext());
        int[] delays = {delay1, delay2, delay3};
    
        try {
            ReadResponse response = service.read(request, client_id, delays);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
        }
    }
    

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        int client_id = Integer.parseInt(MetadataInterceptor.getClientIdFromContext());
        int delay1 = Integer.parseInt(MetadataInterceptor.getDelay1FromContext());
        int delay2 = Integer.parseInt(MetadataInterceptor.getDelay2FromContext());
        int delay3 = Integer.parseInt(MetadataInterceptor.getDelay3FromContext());
        int[] delays = {delay1, delay2, delay3};
    
        try {
            getTupleSpacesStateResponse response = service.getTupleSpacesState(request, client_id, delays);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
        }
    }
    

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        int client_id = Integer.parseInt(MetadataInterceptor.getClientIdFromContext());
        int delay1 = Integer.parseInt(MetadataInterceptor.getDelay1FromContext());
        int delay2 = Integer.parseInt(MetadataInterceptor.getDelay2FromContext());
        int delay3 = Integer.parseInt(MetadataInterceptor.getDelay3FromContext());
        int[] delays = {delay1, delay2, delay3};
    
        try {
            TakeResponse response = service.takePhase1(request, client_id, delays);
            responseObserver.onNext(response); // resposta da primeira fase
            responseObserver.onCompleted();
            service.takePhase2(response.getResult(), client_id, delays); // execução dos takes em cada replica
            

        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
        }
    }
    

}