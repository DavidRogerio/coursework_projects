package pt.ulisboa.tecnico.tuplespaces.frontend;

public final class FrontEndDebug {

    private static boolean debug_flag=false;

    private FrontEndDebug() {        
    }
    
    public static void setDebugFlag(boolean val){
        debug_flag=val;
    }

    public static void debug(String debugMessage){
        if(debug_flag)
            System.err.println(debugMessage);
    }
}