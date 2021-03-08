package threads.safe;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * No one can use this reference if it's busy
 * Usage is safe and non-blocking
 */
public class NonBlockingReference<T> {

    private final T resource;
    private final AtomicBoolean busy;

    public NonBlockingReference(Supplier<T> creator) {
        this.resource = creator.get();
        this.busy = new AtomicBoolean(false);
    }

    /**
     * Use resource with safety (non-blocking) consumer
     * @return true if operation was successful otherwise false (resource is busy)
     */
    public boolean use(Consumer<T> consume) {
        Optional<T> canBeUsed = this.use();
        canBeUsed.ifPresent(consume.andThen(object -> this.release()));
        return canBeUsed.isPresent();
    }

    /**
     * Use resource with non-blocking use operation. Do not forget to release resource after usage!
     * @return empty optional if resource is busy at this moment else of optional of resource
     */
    public Optional<T> use() {
        if (this.busy.compareAndSet(false, true)) {
            return Optional.of(resource);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Release operation makes resource available to other threads that are interested in using it
     */
    public void release() {
        if (!this.busy.compareAndSet(true, false)) {
            throw new IllegalStateException("Concurrency was not correct! " +
                    "You need to release resource after usage!");
        }
    }
}
