package philosophers;

import io.reactivex.rxjava3.core.Observable;

import java.util.List;
import java.util.stream.Collectors;

public class RoundTable {

    private static final int START_INDEX = 0;

    public List<Fork> forks;
    public List<Philosopher> philosophers;

    private final int howManyPhilosophers;

    public RoundTable(int howManyPhilosophers) {
        this.howManyPhilosophers = howManyPhilosophers;
        this.forks = this.setupForks();
        this.philosophers = this.setupPhilosophers();
    }

    public List<Fork> setupForks() {
        return Observable.fromSupplier(Fork::new)
                .repeat(howManyPhilosophers)
                .blockingStream()
                .collect(Collectors.toList());
    }

    private List<Philosopher> setupPhilosophers() {
        this.philosophers = Observable.fromSupplier(Philosopher::new)
                .repeat(howManyPhilosophers)
                .blockingStream()
                .collect(Collectors.toList());

        Observable<Integer> cycledIndexes = Observable.range(START_INDEX, howManyPhilosophers)
                .repeat();

        Observable.merge(
                // Shift all indexes by minus one thus take 5 -> 4 0 1 2 3 for howManyPhilosophers = 5
                cycledIndexes.skip(howManyPhilosophers - 1),
                cycledIndexes,
                // Shift all indexes by one thus take 5 -> 1 2 3 4 0 for howManyPhilosophers = 5
                cycledIndexes.skip(1)
        )
                .buffer(3)
                .take(howManyPhilosophers)
                .forEach(indexes -> {
                    // so indexes now is (prev, current, next) philosopher indexes
                    Philosopher current = this.philosophers.get(indexes.get(1));

                    current.leftNeighbor = this.philosophers.get(indexes.get(0));
                    current.rightNeighbor = this.philosophers.get(indexes.get(2));

                    current.leftFork = this.forks.get(indexes.get(1));
                    current.rightFork = this.forks.get(indexes.get(2));
                });

        return philosophers;
    }
}
