package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;
import java.util.List;
import static pt.ulisboa.tecnico.tuplespaces.frontend.FrontEndDebug.debug;
public class ResponseCollector<T> {
    private final int expectedResponses;
    private final List<T> responses = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();

    public ResponseCollector(int expectedResponses) {
        this.expectedResponses = expectedResponses; //utilizei mas agora serve para nada
    }

    public synchronized void addResponse(T response) {
        responses.add(response);
        notifyAll();
    }

    public synchronized void addError(Throwable t) {
        errors.add(t);
        notifyAll();
    }

    public synchronized void waitFor(int Replicas) throws InterruptedException {
        debug("\n[Collector] Aguardando por " + Replicas + " respostas (ou erros)...");
        while (responses.size() + errors.size() < Replicas) {
            wait(); 
        }
    
        debug("[Collector] Coleta finalizada. Total respostas: " + responses.size() + ", erros: " + errors.size());
        if (!errors.isEmpty()) {
            for (Throwable err : errors) {
                debug("[Collector] Erro registrado: " + err);
            }
        }
    }
    
    // espera ate liberar os votos
    public static <T> void waitForMultipleCollectors(List<ResponseCollector<T>> collectors) throws InterruptedException {
        for (ResponseCollector<T> collector : collectors) {
            collector.waitFor(1); // Cada releaseVote() espera apenas 1 resposta da réplica
        }
    }

    public synchronized List<T> getResponses() {
        return new ArrayList<>(responses);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Throwable getFirstError() {
        return errors.get(0);
    }
}
