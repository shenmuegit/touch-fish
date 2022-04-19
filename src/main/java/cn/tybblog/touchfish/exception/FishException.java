package cn.tybblog.touchfish.exception;

/**
 * @author ly
 */
public class FishException extends Exception {

    private String message;

    public FishException(String message){
        super(message);
        this.message = message;
    }


    public static void throwFishException(String msg) throws FishException {
        throw new FishException(msg);
    }
}
