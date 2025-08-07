package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.stub.StreamObserver;
import static pt.ulisboa.tecnico.tuplespaces.frontend.FrontEndDebug.debug;

public class Observer<T> implements StreamObserver<T> {

    private final ResponseCollector<T> collector;
    private final Runnable onResponseCallback;

    // Construtor simples, sem callback
    public Observer(ResponseCollector<T> collector) {
        this(collector, null);
    }

    // Construtor com callback
    public Observer(ResponseCollector<T> collector, Runnable onResponseCallback) {
        this.collector = collector;
        this.onResponseCallback = onResponseCallback;
    }

    @Override
    public void onNext(T response) {
        collector.addResponse(response);
        //debug("[Observer] onNext() recebido: " + response.toString());
        if (onResponseCallback != null) {
            //debug("[Observer] ******* Executando callback após onNext() *******");
            onResponseCallback.run();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        collector.addError(throwable); 
        //debug("[Observer] onError() recebido: " + throwable.toString());
        if (onResponseCallback != null) {
            //debug("[Observer] ******* Executando callback após onError() *******");
            onResponseCallback.run();
        }
    }

    @Override
    public void onCompleted() {
        //debug("[Observer] onCompleted() chamado.");
    }
}
/**
    "Quando algo acontecer, executa isto."
 * 
 * Observer<T> é um wrapper genérico para lidar com respostas assíncronas do gRPC.
 * Ele coleta respostas ou erros usando um ResponseCollector<T>, e opcionalmente
 * executa um callback toda vez que uma resposta (ou erro) chega.
 *
 * Isso é especialmente útil no contexto de operações como `take()`, onde o FrontEnd
 * precisa rastrear quantas réplicas já responderam para um dado cliente e seqId.
 *
 * Assim, toda vez que uma réplica responder, o estado do cliente é atualizado automaticamente.

 */
