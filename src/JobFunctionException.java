public class JobFunctionException extends Exception {
    private static final int CODE = 138;

    public JobFunctionException(String var1, Throwable var2) {
        super(var1, var2);
    }

    public int getCode() {
        return 138;
    }
}
