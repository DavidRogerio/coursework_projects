package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class MetadataInterceptor implements ServerInterceptor {


    private static final Metadata.Key<String> client_id_key =
            Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> delay1_key =
            Metadata.Key.of("delay1", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> delay2_key =
            Metadata.Key.of("delay2", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> delay3_key =
            Metadata.Key.of("delay3", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<String> client_id_context_key = Context.key("client-id");
    public static final Context.Key<String> delay1_context_key = Context.key("delay1");
    public static final Context.Key<String> delay2_context_key = Context.key("delay2");
    public static final Context.Key<String> delay3_context_key = Context.key("delay3");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String client_id = headers.get(client_id_key);
        String delay1 = headers.get(delay1_key);
        String delay2 = headers.get(delay2_key);
        String delay3 = headers.get(delay3_key);

        Context ctx = Context.current()
                .withValue(client_id_context_key, client_id)
                .withValue(delay1_context_key, delay1)
                .withValue(delay2_context_key, delay2)
                .withValue(delay3_context_key, delay3);

        return Contexts.interceptCall(ctx, call, headers, next);
    }

    public static String getClientIdFromContext() {
        return client_id_context_key.get();
    }

    public static String getDelay1FromContext() {
        return delay1_context_key.get();
    }

    public static String getDelay2FromContext() {
        return delay2_context_key.get();
    }

    public static String getDelay3FromContext() {
        return delay3_context_key.get();
    }
}
