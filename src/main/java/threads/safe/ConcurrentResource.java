package threads.safe;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * No one can use this reference if it's busy
 */
public class ConcurrentResource<T> {

    private final T resource;
    private final AtomicBoolean busy;
    private volatile boolean hasSomeWaiters;

    public ConcurrentResource(Supplier<T> creator) {
        this.resource = creator.get();
        if (this.resource == null) {
            throw new IllegalArgumentException("Creator cannot return null!");
        }
        this.busy = new AtomicBoolean(false);
        this.hasSomeWaiters = false;
    }

    /**
     * Use resource in a non-blocking manner
     * Do not forget to release resource after usage! It must be released on any possible exceptions
     * <p>
     * Usage example:
     * Optional<T> resource = this.use();
     * if (resource.isPresent()) {
     *     try {
     *         // to work with resource
     *     } finally {
     *         resource.release();
     *     }
     * }
     * OR in a functional way
     * this.use().ifPresent(resource -> {
     *     try {
     *         // work with resource
     *     } finally {
     *         resource.release();
     *     }
     * });
     *
     *
     * @return empty optional if resource is busy at this moment else optional of resource
     */
    public Optional<T> use() {
        try {
            if (this.busy.compareAndSet(false, true)) {
                return Optional.of(resource);
            } else {
                return Optional.empty();
            }
        } catch (Throwable e) {
            this.busy.compareAndSet(true, false);
            throw e;
        }
    }

    /**
     * Blocking is relatively safe but can be dangerous if resource will be busy quite long or infinitely
     *
     * @return false if waiting for release has no any sense otherwise true (after waiting)
     */
    public synchronized boolean waitForRelease() throws InterruptedException {
        try {
            if (this.use().isPresent()) {
                this.release();
                return false;
            }
        } catch (Throwable e) {
            if (this.busy.get()) {
                this.release();
            }
            throw e;
        }

        try {
            this.hasSomeWaiters = true;
            this.wait();
        } finally {
            this.hasSomeWaiters = false;
        }
        return true;
    }

    /**
     * Release operation makes resource available to other threads that are interested in using it
     */
    public void release() {
        if (!this.busy.compareAndSet(true, false)) {
            throw new IllegalStateException("Concurrency was not correct! " +
                    "You need to release resource after usage!");
        }

        synchronized (this) {
            if (this.hasSomeWaiters) {
                this.notifyAll();
            }
        }
    }
}
