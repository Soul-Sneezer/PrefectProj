package prefect;

public class Continue extends RuntimeException{
    Continue()
    {
        super(null, null, false, false);
    }
}
