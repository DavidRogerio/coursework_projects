package pt.ulisboa.tecnico.tuplespaces.server;

public final class ServerDebug {

    private static boolean debug_flag=false;

    private ServerDebug() {        
    }
    
    public static void setDebugFlag(boolean val){
        debug_flag=val;
    }

    public static void debug(String debugMessage){
        if(debug_flag)
            System.err.println(debugMessage);
    }
}