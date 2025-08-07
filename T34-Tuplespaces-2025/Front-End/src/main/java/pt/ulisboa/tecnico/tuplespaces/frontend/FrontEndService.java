package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2Grpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2Grpc.TupleSpacesB2Stub;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*; 
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesB2OuterClass.*; 
import static pt.ulisboa.tecnico.tuplespaces.frontend.FrontEndDebug.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class FrontEndService {
    // Lista que guarda a informação geral de cada replica
    private final List<ReplicaResource> replicas;

    // Front-end apenas mantem e incrementa o seqId, o servidor verifica a ordem e executa!
    // Mapa que guarda, para cada client_id, o respectivo ClientSeqState, este que contem seqId
    private final Map<Integer, ClientSeqState> clientStates = new ConcurrentHashMap<>();

    public FrontEndService(List<ReplicaResource> replicas) {
        this.replicas = replicas;
    }

    /*  
        Devolve o state do cliente, onde é quardado o seqId e o nº atual de takes em execução.
        Detem todos os metodos para a logica de impedir um put de ser feito antes de terminar op takes
    */
    private ClientSeqState getClientState(int client_id) {
        return clientStates.computeIfAbsent(client_id, k -> new ClientSeqState());
    }
    
    // Adquere já incrementado o seqId, para execução de um comando, dado um clientId
    private int getAndIncrementSeqId(int client_id) {
        return getClientState(client_id).getAndIncrementSeqId();
    }

    
    // Cria um stub que injeta metadados (clientId, seqId, delay etc.) em cada requisição.
    private TupleSpacesB2Stub attachMetadata(TupleSpacesB2Stub stub, int client_id, int delay, int serverId, int seqId) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(client_id));
        metadata.put(Metadata.Key.of("seq_id", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(seqId));
        metadata.put(Metadata.Key.of("delay" + serverId, Metadata.ASCII_STRING_MARSHALLER), String.valueOf(delay));
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    /**
     * Envia um pedido de PUT para todas as réplicas.
     * 
     * Antes de enviar o pedido, o FrontEnd verifica se existem operações de `take` pendentes
     * para este cliente (usando `ClientSeqState`). Caso existam, bloqueia até que todas terminem.
     * 
     * Cada réplica recebe o `PUT` com o `seqId` correspondente, garantindo a ordem das operações
     * por cliente nas réplicas. A resposta só é considerada bem-sucedida se todas as réplica
     * confirmar o `PUT`.
     */
    public void put(PutRequest request, int client_id, int[] delays) throws InterruptedException {
        ClientSeqState state = getClientState(client_id);
        state.waitForNoPendingTakes(); // Esperar por fim de takes pendentes
    
        int seqId = getAndIncrementSeqId(client_id);
        ResponseCollector<PutResponse> collector = new ResponseCollector<>(replicas.size());
    
        for (ReplicaResource replicaInfo : replicas) {
            int serverId = replicaInfo.serverId;
            TupleSpacesB2Stub stub = attachMetadata(replicaInfo.asyncStub, client_id, delays[serverId], serverId, seqId);
    
            debug(String.format("[FrontEnd] Client %d -> PUT(%s) | seqId=%d | réplica=%d", client_id, request.getNewTuple(), seqId, serverId));
    
            stub.put(request, new Observer<>(collector));
        }
    
        collector.waitFor(replicas.size());
    
        if (collector.hasErrors() && collector.getResponses().isEmpty()) {
            throw new RuntimeException("! ! ! PUT falhou em todas as réplicas", collector.getFirstError());
        }
    
        debug("[FrontEnd] PUT confirmado por todas as réplicas");
        debug("\n -------------------------------  FIM   PUT  -----------------------------\n");


    }

    /**
     * Executa um pedido de READ em todas as réplicas, usando `seqId` para manter a ordem.
     * 
     * Este método envia o mesmo `READ` para todas as réplicas, mas espera apenas pela
     * primeira resposta bem-sucedida. Isso permite que o cliente obtenha uma resposta
     * rápida mesmo que algumas réplicas estejam lentas ou indisponíveis.
     */
    public ReadResponse read(ReadRequest request, int client_id, int[] delays) throws InterruptedException {
        int seqId = getAndIncrementSeqId(client_id);

        ResponseCollector<ReadResponse> collector = new ResponseCollector<>(replicas.size());

        for (ReplicaResource replicaInfo : replicas) {
            int serverId = replicaInfo.serverId;
            TupleSpacesB2Stub stub = attachMetadata(replicaInfo.asyncStub, client_id, delays[serverId], serverId, seqId);

            debug(String.format("[FrontEnd] Client %d -> READ(%s) | seqId=%d | réplica=%d", client_id, request.getSearchPattern(), seqId, serverId));

            stub.read(request, new Observer<>(collector));
        }

        collector.waitFor(1);

        if (collector.hasErrors() && collector.getResponses().isEmpty()) {
            throw new RuntimeException("! ! ! READ falhou em todas as réplicas", collector.getFirstError());
        }

        debug("[FrontEnd] READ confirmado por pelo menos uma réplica");
        debug("\n ------------------------------- FIM   READ -------------------------------\n");

        return collector.getResponses().get(0); // primeira resposta
    }

    /**
     * Coleta o estado atual do espaço de tuplos de todas as réplicas.
     * 
     * Cada réplica responde com sua lista local de tuplos. O FrontEnd
     * espera por todas as respostas e formata o resultado para o cliente.
     * 
     *  Se alguma réplica falhar, a resposta ainda pode ser formada.
     */
    public getTupleSpacesStateResponse getTupleSpacesState(getTupleSpacesStateRequest request, int client_id, int[] delays) throws InterruptedException {
        ResponseCollector<getTupleSpacesStateResponse> collector = new ResponseCollector<>(replicas.size());

        for (ReplicaResource replicaInfo : replicas) {
            int serverId = replicaInfo.serverId;
            TupleSpacesB2Stub stub = attachMetadata(replicaInfo.asyncStub, client_id, delays[serverId], serverId, 0);
            stub.getTupleSpacesState(request, new Observer<>(collector));
        }

        collector.waitFor(replicas.size());

        if (collector.hasErrors()) {
            throw new RuntimeException("! ! ! getTupleSpacesState failed", collector.getFirstError());
        }

        // Formatar a resposta ao cliente
        List<String> allTuples = new ArrayList<>();
        for (getTupleSpacesStateResponse response : collector.getResponses()) {
            allTuples.add("[" + String.join(", ", response.getTupleList()) + "]");
        }
        debug("\n ------------------------------- FIM  STATE -------------------------------\n");

        return getTupleSpacesStateResponse.newBuilder().addAllTuple(allTuples).build();
    }

    /**
     * Fase 1 do algoritmo de take.
     * 
     * O objetivo desta fase é obter os votos do quorum (2 réplicas), executar o `requestVote()` com regex,
     * calcular a interseção dos tuplos possíveis e escolher um que esteja presente em todas as réplicas.
     * 
     * ! Este método inclui dois possíveis momentos de backoff:
     *   1. Se não conseguir obter os votos de todas as réplicas do voter set (feito internamente por `acquireVotesWithRetry`)
     *   2. Se conseguir os votos, mas a interseção dos tuplos retornados for vazia (nenhum tuplo está em todas)
     * 
     * Retorna imediatamente um `TakeResponse` com o tuplo selecionado (sem o remover ainda).
     */
    public TakeResponse takePhase1(TakeRequest request, int client_id, int[] delays) throws InterruptedException {
        Set<Integer> voterSet = Set.of(client_id % 3, (client_id + 1) % 3);
        String pattern = request.getSearchPattern(); 
        int attempt = 0; // numero de tentativas backoff

        while (true) {
            ResponseCollector<RequestVoteResponse> voteCollector = acquireVotesWithRetry(client_id, pattern, voterSet, delays);

            Set<String> intersecao = computeIntersection(voteCollector);
            if (intersecao.isEmpty()) {
                debug("[FrontEnd] takePhase1 -> Intersecção fazia ou não existe o tuplo! Tentando tudo novamente ...");

                releaseVotes(client_id, voterSet, delays); // intersecção é vazia, ou seja, não existe o tuplo -> tente novamente
                backoff(attempt, 100, 5000);
                attempt++;
                continue;
            }

            String selectedTuple = intersecao.iterator().next();
            debug("\n ---------------------------- FIM TAKE PHASE 1 ---------------------------- \n");
            return TakeResponse.newBuilder().setResult(selectedTuple).build();
        }
    }

    /**
     * Fase 2 do algoritmo de take.
     * 
     * Esta fase é chamada após a fase 1 retornar um tuplo concreto que pode ser removido.
     * Envia o `take()` concreto para as 3 réplicas com o `selectedTuple`, mantendo o `seqId`
     * e controlando a ordem por cliente através do estado `ClientSeqState`.
     * 
     * Para cada resposta (ou erro) de uma réplica, o contador de respostas pendentes é decrementado.
     * Só depois de todas as réplicas responderem e votos serem libertados, o take é considerado bem-sucedido.
     */

    public void takePhase2(String selectedTuple, int client_id, int[] delays) throws InterruptedException {
        Set<Integer> voterSet = Set.of(client_id % 3, (client_id + 1) % 3);
        ClientSeqState state = getClientState(client_id);
        int seqId = getAndIncrementSeqId(client_id);
    
        state.initTakeResponses(seqId, replicas.size()); // Inicializa o número de respostas esperadas (para controle de ordem no put)
    
        ResponseCollector<TakeResponse> collector = new ResponseCollector<>(replicas.size());
        TakeRequest concreteTake = TakeRequest.newBuilder().setSearchPattern(selectedTuple).build();
    
        for (ReplicaResource replica : replicas) {
            int serverId = replica.serverId;
            TupleSpacesB2Stub stub = attachMetadata(replica.asyncStub, client_id, delays[serverId], serverId, seqId);
    
            stub.take(concreteTake, new Observer<>(collector, () -> {
                state.markTakeResponse(seqId);
            }));
        }
    
        collector.waitFor(replicas.size());
        releaseVotes(client_id, voterSet, delays);
    
        if (collector.hasErrors() || collector.getResponses().size() < replicas.size() || !state.isTakeComplete(seqId)) {
            throw new RuntimeException("Failed take on all replicas", collector.getFirstError());
        }
        debug("\n ------------------------- FIM TAKE PHASE 2 ---------------------------- \n");

    }


    // ----------------------------------------------------------
    // ----------------- METODOS AUXILIARES  --------------------
    // ----------------------------------------------------------

    private ResponseCollector<RequestVoteResponse> acquireVotesWithRetry(int client_id, String regex, Set<Integer> voterSet, int[] delays) throws InterruptedException {
        int attempt = 0;

        while (true) {
            ResponseCollector<RequestVoteResponse> voteCollector = new ResponseCollector<>(voterSet.size());

            for (ReplicaResource replicaInfo : replicas) {
                int serverId = replicaInfo.serverId;
                if (voterSet.contains(serverId)) {
                    TupleSpacesB2Stub stub = attachMetadata(replicaInfo.asyncStub, client_id, delays[serverId], serverId, 0); //seqId não é aplicado a metodos auxiliares

                    debug(String.format("[FrontEnd] Client %d -> Server %d: requestVote(regex=%s)", client_id, serverId, regex));

                    RequestVoteRequest voteReq = RequestVoteRequest.newBuilder().setClientId(client_id).setRegex(regex).build();
                    stub.requestVote(voteReq, new Observer<>(voteCollector));
                }
            }

            voteCollector.waitFor(voterSet.size());

            boolean allGranted = !voteCollector.hasErrors() && voteCollector.getResponses().stream().allMatch(RequestVoteResponse::getGranted); 

            if (allGranted) {
                debug(String.format("[FrontEnd] Client %d: Adquirido todos os votos ! ! !", client_id));
                return voteCollector;
            } else {
                debug(String.format("[FrontEnd] Client %d: Falhei ao adquirir todos os votos, tentando novamente ...", client_id));
                backoff(attempt, 100, 10000); // max de espera entre tentativas, 10s
                attempt++;
            }
        }

    }

    private void releaseVotes(int client_id, Set<Integer> voterSet, int[] delays) throws InterruptedException {
        List<ResponseCollector<ReleaseVoteResponse>> releaseCollectors = new ArrayList<>();
        for (ReplicaResource replicaInfo : replicas) {
            int serverId = replicaInfo.serverId;
            if (voterSet.contains(serverId)) {
                ResponseCollector<ReleaseVoteResponse> releaseCollector = new ResponseCollector<>(1);
                releaseCollectors.add(releaseCollector);
                TupleSpacesB2Stub stub = attachMetadata(replicaInfo.asyncStub, client_id, delays[serverId], serverId, 0);

                debug(String.format("[FrontEnd] Client %d -> Server %d: request releaseVote(seqId=%d)", client_id, serverId, 0));

                stub.releaseVote(
                    ReleaseVoteRequest.newBuilder().setClientId(client_id).build(),
                    new Observer<>(releaseCollector)
                );
            }
        } debug("\n----------> Esperando as respostaas dos release <---------- ");

        ResponseCollector.waitForMultipleCollectors(releaseCollectors);         // Espera a resposta de cada release
        debug(String.format("[FrontEnd] Client %d -> releaseVotes done.", client_id));
    }


    private Set<String> computeIntersection(ResponseCollector<RequestVoteResponse> voteCollector) {
        List<Set<String>> matchesPorReplica = new ArrayList<>(); // um set para cada replica do voterSet
        for (RequestVoteResponse response : voteCollector.getResponses()) {
            matchesPorReplica.add(new HashSet<>(response.getMatchesList()));
        }

        int expectedQuorumSize = 2;
        if (matchesPorReplica.isEmpty()) return Set.of();
        if (matchesPorReplica.size() < expectedQuorumSize) return Set.of(); // força retry

        Set<String> intersection = new HashSet<>(matchesPorReplica.get(0)); 
        // mantém apenas os tuplos que aparecem em todos os sets.
        for (int i = 1; i < matchesPorReplica.size(); i++) {
            intersection.retainAll(matchesPorReplica.get(i)); 
        }

        debug("[FrontEnd] Interseção contém " + intersection.size() + " tuplo(s): " + intersection);
        return intersection;
    }


    private void backoff(int attempt, int baseMillis, int maxMillis) {
        Random random = new Random();

        // Calcula o delay exponencial: (2^attempt * baseMillis), com limite superior
        int delay = Math.min((1 << attempt) * baseMillis, maxMillis);

        int bound = Math.max(delay / 2, 1); //  garante que nunca passe um valor inválido
        int jitter = bound + random.nextInt(bound);

        try {
            Thread.sleep(jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during exponential backoff");
        }
    }


    public void shutdown() {
        for (ReplicaResource replicaInfo : replicas) {
            replicaInfo.shutdown();
        }
    }
}
