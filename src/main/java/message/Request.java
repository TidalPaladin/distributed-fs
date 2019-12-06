import java.net.InetSocketAddress;

public class Request<I extends Job, O> extends Message<I> {

	public Request(I job, InetSocketAddress to, InetSocketAddress from) {
		super(job, to, from );
	}

	public Request(I job, InetSocketAddress to) {
		this(job, to, null);
	}

	public Request(Request<I, O> other, InetSocketAddress from) {
		this(other.job, other.sendTo, from);
	}
}
