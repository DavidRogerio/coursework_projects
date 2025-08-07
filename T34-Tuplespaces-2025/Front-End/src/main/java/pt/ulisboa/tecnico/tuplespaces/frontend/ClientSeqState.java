package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.Map;
import java.util.HashMap;
import static pt.ulisboa.tecnico.tuplespaces.frontend.FrontEndDebug.debug;

public class ClientSeqState {
    private int nextSeqId = 0;

    // Mapeia seqId -> número de respostas pendentes (para os TAKEs)
    private final Map<Integer, Integer> takeMissingResponses = new HashMap<>();

    public synchronized int getAndIncrementSeqId() {
        return nextSeqId++; // retorna 0, depois vira 1
    }

    public synchronized void initTakeResponses(int seqId, int expectedTotal) {
        takeMissingResponses.put(seqId, expectedTotal);
    }

    public synchronized void markTakeResponse(int seqId) {
        takeMissingResponses.computeIfPresent(seqId, (k, v) -> v - 1); // Se a chave seqId existe no mapa entao atualiza o valor dela com v - 1 (decrementa 1)".
        // Notifica quem estiver esperando zero respostas pendentes (waitForPendingtakes)
        notifyAll();
    }

    /**
     * Indica se existem ainda takes pendentes para este cliente.
     * Se qualquer seqId ainda tiver valor > 0, ainda há takes pendentes.
     */
    public synchronized boolean hasPendingTakes() {
        return takeMissingResponses.values().stream().anyMatch(v -> v > 0);
    }

    /**
     * Bloqueia até que todos os takes pendentes do cliente estejam completos (==0).
     */
    public synchronized void waitForNoPendingTakes() throws InterruptedException {
        while (hasPendingTakes()) {
            debug(String.format("[FrontEnd] Waiting for pending TAKEs to finish..."));

            wait(); // Libera o lock e aguarda notificação
        }
    }

    /**
     * Verifica se para um seqId específico já chegamos a zero respostas pendentes.
     */
    public synchronized boolean isTakeComplete(int seqId) {
        return takeMissingResponses.getOrDefault(seqId, 0) <= 0;
    }
}
