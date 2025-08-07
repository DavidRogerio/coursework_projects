package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
             switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState();
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    this.printUsage();
                    break;
             }
        }
        scanner.close();
        clientService.shutdown();
    }

    private void put(String[] split) {
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
    
        String tuple = split[1];
        Integer[] delays = {0, 0, 0}; // standard delays
    
        if (split.length >= 3 && split.length <= 5) {
            boolean valid = true; 
            for (int i = 2; i < split.length; i++) {
                if (split[i].matches("\\d+")) { // só aceita números positivos
                    int delay = Integer.parseInt(split[i]);
                    delays[i - 2] = delay;
                } else {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                printUsage();
                return;
            }
        }
    
        boolean res = clientService.put(tuple, delays);
        if (!res) return;
        System.out.println("OK\n");
    }
    
    

    private void read(String[] split) {
        if (!inputIsValid(split)) {
            printUsage();
            return;
        }
    
        String pattern = split[1];
        Integer[] delays = {0, 0, 0};
    
        if (split.length >= 3) {
            for (int i = 2; i < split.length; i++) {
                if (!split[i].matches("\\d+")) {
                    printUsage();
                    return;
                }
                delays[i - 2] = Integer.parseInt(split[i]);
            }
        }
    
        String result = clientService.read(pattern, delays);
        if (result != null) {
            System.out.printf("OK%n%s%n", result);
        }
    }
    

    
    private void take(String[] split) {
        if (!inputIsValid(split)) {
            printUsage();
            return;
        }

        String tuple = split[1];

        // Verifica se a regex é válida (tenta compilar)
        String pattern = tuple.substring(1, tuple.length() - 1); // remove < >
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            System.err.println("Expressão regular inválida: " + e.getMessage());
            return;
        }

        Integer[] delays = {0, 0, 0};

        if (split.length >= 3) {
            for (int i = 2; i < split.length; i++) {
                if (!split[i].matches("\\d+")) {
                    printUsage();
                    return;
                }
                delays[i - 2] = Integer.parseInt(split[i]);
            }
        }

        String result = clientService.take(tuple, delays);
        if (result != null) {
            System.out.printf("OK%n%s%n%n", result);
        }
    }


    private void getTupleSpacesState() {

        List<String> result = clientService.getTupleSpacesState();
        
        if (result == null) 
            return;

        System.out.println("OK");

        if (result.isEmpty()){
            System.out.println("[]\n");
            return;
        }
        int size = result.size() - 1;
        System.out.print("[");
        for (int i = 0; i < size; i++) System.out.print(result.get(i) + ", ");
        System.out.println(result.get(size++) + "]\n");
    }


    private void sleep(String[] split) {
      if (split.length != 2){
        this.printUsage();
        return;
      }
      Integer time;

      // checks if input String can be parsed as an Integer
      try {
         time = Integer.parseInt(split[1]);
      } catch (NumberFormatException e) {
        this.printUsage();
        return;
      }

      try {
        Thread.sleep(time*1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState <server>\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    private boolean inputIsValid(String[] input) {
        if (input.length < 2
            || (!input[1].startsWith(BGN_TUPLE))
            || (!input[1].endsWith(END_TUPLE)))
            return false;
    
        int delayCount = input.length - 2;
        if (delayCount < 0 || delayCount > 3)
            return false;
    
        return true;
    }
}
