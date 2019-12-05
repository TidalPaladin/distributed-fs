import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MessageQueue {
	private static final Logger log = LogManager.getLogger("MessageQueue");

	private BlockingQueue<Message> heartbeatQueue, replyQueue, requestQueue;
	private Set<Message> waiting;

	public MessageQueue() {
		heartbeatQueue = new PriorityBlockingQueue<>();
		replyQueue = new PriorityBlockingQueue<>();
		requestQueue = new PriorityBlockingQueue<>();
		waiting = new TreeSet<>();
	}

	public Message take(Job.Type type) throws InterruptedException {
		Message result = null;
		switch(type) {
			case REPLY:
				result = replyQueue.take();
				break;
			case HEARTBEAT:
				result = heartbeatQueue.take();
				break;
			default:
				result = requestQueue.take();
				break;
		}
		return result;
	}

	public void put(Message msg) throws InterruptedException {
		if(msg.isReply()) {
			replyQueue.put(msg);
			for(Message wait : waiting) {
				if(msg.isReplyTo(wait)) {
					synchronized(wait) {
						wait.notify();
					}
				}
			}
		}
		else if(msg.job.getType() == Job.Type.HEARTBEAT) {
			heartbeatQueue.put(msg);
		}
		else {
			requestQueue.put(msg);
		}
	}

	public Message putAndWait(Message msg) throws InterruptedException {
		put(msg);
		waiting.add(msg);
		synchronized(msg) {
			msg.wait();
		}
		Message reply = replyQueue.take();
		waiting.remove(msg);
		return reply;
	}

	@Override
	public String toString() {
		return String.format("MessageQueue(%s %s %s)",
			replyQueue.toString(),
			requestQueue.toString(),
			heartbeatQueue.toString()
		);
	}
}
