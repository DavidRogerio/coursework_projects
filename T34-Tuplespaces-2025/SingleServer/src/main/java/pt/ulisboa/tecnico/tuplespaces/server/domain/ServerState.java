package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerDebug.debug;

public class ServerState {
  private List<String> tuples;

  // Maekawa-specific state:
  private final Map<String, Integer> lockedTuples = new HashMap<>();
  private final Map<Integer, Integer> lastSeqId = new HashMap<>();

  public ServerState() {
    this.tuples = new ArrayList<>();
  }

  public synchronized void put(String tuple, int clientId, int seqId) throws InterruptedException{
    waitForTurn(clientId, seqId);
    tuples.add(tuple);
    notifyAll(); // notificar possiveis threads que estão a espera deste seqId ser concluido
  }
// !!
  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern,int clientId, int seqId) throws InterruptedException{
    waitForTurn(clientId, seqId);
    String match = getMatchingTuple(pattern);
    while(match == null){
      wait();
      match = getMatchingTuple(pattern);
    }
    notifyAll(); 
    return match;
  }

  public synchronized String take(String pattern, int clientId, int seqId) throws InterruptedException {
    waitForTurn(clientId, seqId);
    while (true) {
      String match = getMatchingTuple(pattern);
      // Verifica se há match e se está bloqueado pelo cliente certo
      if (match != null) {
        Integer owner = lockedTuples.get(match);
        if (owner == null || owner == clientId) {
          if (tuples.remove(match)) {
            debug(String.format("[Server] Tuple %s foi removido pelo client %d", match, clientId));

            notifyAll();             
            return match;
          } else {
            debug(String.format("[warning] Tuple %s matched e locked mas não foi removido (already gone?)", match));
            }
        } else {
            debug(String.format("[Server] Tuple %s matched mas o cliente %d não detem o lock e sim o cliente %d", match, clientId, owner));
          }
      } else {
          debug("[Server] Nenhum tuplo deu match ao padrão " + pattern + ", waiting...");
        }
      wait(); 
    }
  }

  private boolean clientIdOwnsTuple(String tuple, int clientId) {
    Integer owner = lockedTuples.get(tuple);
    return owner != null && owner == clientId;
  }

  public synchronized List<String> getTupleSpacesState(int clientId, int seqId) throws InterruptedException{
    return new ArrayList<>(this.tuples);
  }

  public synchronized List<String> requestVote(int clientId, String regex) {
    debug(String.format("[Server] Client %d -> REQUEST_VOTE with pattern: %s", clientId, regex));

    List<String> granted = new ArrayList<>(); // Quardar os matched tuples com o regex

    for (String tuple : tuples) { // iterar sobre todos os tuplos
      if (tuple.matches(regex)) { 
        Integer currentOwner = lockedTuples.get(tuple);
        if (currentOwner == null || currentOwner.equals(clientId)) { // o tuplo tem lock?
          lockedTuples.put(tuple, clientId); // add lock
          granted.add(tuple);
          debug(String.format("[Server] Locked tuple %s for client %d", tuple, clientId));
        } else {
          debug(String.format("[Server] Tuple %s locked by client %d, skipping", tuple, currentOwner));
        }
      }
    }

    if (granted.isEmpty()) 
      debug(String.format("[Server] No tuples matched or could be locked by client %d", clientId));
  
    return granted;
  }


  public synchronized void releaseVote(int clientId) {
    Iterator<Map.Entry<String, Integer>> it = lockedTuples.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> entry = it.next();
      if (entry.getValue().equals(clientId)) {
        debug(String.format("[Server] Released lock on tuple %s from client %d", entry.getKey(), clientId));
        it.remove();
      }
    }
    notifyAll();
  }

    /**
   * Garante que os pedidos (requests) de um mesmo cliente sejam processados em ordem.
   * 
   * O servidor mantém o último `seqId` processado para cada `clientId` no mapa `lastSeqId`.
   * Esse método entra em espera (com `wait()`) até que o próximo `seqId` esperado seja igual ao recebido.
   */
  private void  waitForTurn(int clientId, int seqId) throws InterruptedException {
    while (lastSeqId.getOrDefault(clientId, -1) + 1 != seqId) { // primeiro pedido {id,-1}-> seqId -> -1 +1 = 0 == seqId
      debug(String.format("[Server] !! wait wait !! A espera de outro pedido ser processado !! wait wait !! "));
      wait();
    }
    lastSeqId.put(clientId, seqId);
  }
} 
