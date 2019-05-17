package cn.edu.seu;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.nio.ByteBuffer;

public class TestDisruptor {

    public static void main(String[] args){
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(LongEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);

        // Connect the handler
        disruptor.handleEventsWith(new LongEventHandler());
        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        ByteBuffer bb = ByteBuffer.allocate(8);
        for (long l = 0; true; l++)
        {
            bb.putLong(0, l);
            ringBuffer.publishEvent(((longEvent, sequence) -> longEvent.set(bb.getLong(0))));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static class LongEvent
    {
        private long value;

        public void set(long value)
        {
            this.value = value;
        }
    }

    public static class LongEventFactory implements EventFactory<LongEvent>
    {
        public LongEvent newInstance()
        {
            return new LongEvent();
        }
    }

    public static class LongEventHandler implements EventHandler<LongEvent>
    {
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch)
        {
            System.out.println("Event: " + event);
        }
    }
}
