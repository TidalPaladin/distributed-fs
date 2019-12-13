import java.io.Serializable;
import java.util.concurrent.Callable;

public abstract class Job<T> implements Serializable, Callable<T> {
}
