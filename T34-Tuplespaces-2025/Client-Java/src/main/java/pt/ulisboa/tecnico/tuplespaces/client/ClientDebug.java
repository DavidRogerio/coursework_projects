package pt.ulisboa.tecnico.tuplespaces.client;

public final class ClientDebug {

    private static boolean debug_flag;

    private ClientDebug() {        
    }
    
    public static void setDebugFlag(boolean val){
        debug_flag=val;
    }

    public static void debug(String debugMessage){
        if(debug_flag)
            System.err.println(debugMessage);
    }
}