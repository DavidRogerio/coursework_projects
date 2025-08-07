package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import static pt.ulisboa.tecnico.tuplespaces.server.ServerDebug.debug;

public class ServerMetadataInterceptor implements ServerInterceptor {

    private final int serverId;

    private static final Metadata.Key<String> client_id_key =
            Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> seq_id_key =
            Metadata.Key.of("seq_id", Metadata.ASCII_STRING_MARSHALLER);


    // Context keys
    public static final Context.Key<String> client_id_context_key = Context.key("client-id");
    public static final Context.Key<String> delay_context_key = Context.key("delay");
    public static final Context.Key<String> seq_id_context_key = Context.key("seq_id");

    public ServerMetadataInterceptor(int serverId) {
        this.serverId = serverId;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String client_id = headers.get(client_id_key);
        String seq_id = headers.get(seq_id_key);

        Metadata.Key<String> delayKey =
                Metadata.Key.of("delay" + serverId, Metadata.ASCII_STRING_MARSHALLER);
        String delay = headers.get(delayKey);
        debug("\n ------------------------------- [ INICIO ] ------------------------------- \n");
        debug("[Interceptor] serverId=" + serverId + ", extracted delay=" + delay + ", seq_id=" + seq_id + "\n");

        Context ctx = Context.current()
                .withValue(client_id_context_key, client_id)
                .withValue(delay_context_key, delay)
                .withValue(seq_id_context_key, seq_id);

        return Contexts.interceptCall(ctx, call, headers, next);
    }

    public static String getClientIdFromContext() {
        return client_id_context_key.get();
    }

    public static String getDelayFromContext() {
        return delay_context_key.get();
    }
    public static String getSeqIdFromContext() {
        return seq_id_context_key.get();
    }
}
