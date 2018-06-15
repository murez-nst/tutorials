package com.murez.util;
/**
 * @author Murez Nasution
 */
public abstract class Executor<T> implements Runnable {
    private final Object LOCK = new Object();
    private final Resumable R;
    private Boolean active;
    private boolean resist;

    protected Executor() {
        R = () -> {
            synchronized(this) {
                resist = false;
                synchronized(LOCK) { LOCK.notify(); }
            }
        };
        new Thread(this).start();
    }

    @Override
    public final void run() {
        for(T out = null; ; out = onActive())
            synchronized(LOCK) {
                if(active != null)
                    onFinish(out);
                active = false;
                try { LOCK.wait(); }
                catch(InterruptedException e) {
                    onClose();
                    break;
                }
                active = !active;
            }
    }

    protected Resumable acquires() {
        synchronized(R) {
            if(!resist) {
                boolean deactived;
                synchronized(LOCK) {
                    deactived = active != null && !active;
                }
                if(deactived) {
                    resist = !resist;
                    return R;
                }
            }
            return null;
        }
    }

    protected abstract T onActive();

    protected abstract void onFinish(T output);

    protected abstract void onClose();

    public interface Resumable {
        void resume();
    }
}