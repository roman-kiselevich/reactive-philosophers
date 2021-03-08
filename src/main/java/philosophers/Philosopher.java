package philosophers;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler;
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Philosopher {

    private final static Random RANDOM = new Random();
    private final static int MAX_ACTION_TIME = 1000; // milliseconds
    // https://www.google.com/search?q=human+brain+visual+reaction+time
    private final static int HUMAN_BRAIN_VISUAL_REACTION_TIME = 200; // milliseconds
    private static final LinkedList<String> WHAT_CAN_PHILOSOPHER_THINK_ABOUT = new LinkedList<>(Arrays.asList(
            "Reactive X", "Programming", "Why people don't want to learn haskell",
            "Why the Answer to the Ultimate Question of Life, the Universe, and Everything is 42",
            "Linux is the best OS for programmers?", "Functional vs OOP way", "Prolog is died?",
            "wave–particle duality"
    ));
    private final static LinkedList<String> POSSIBLE_NAMES = new LinkedList<>(Arrays.asList(
            "James", "David", "John", "Daniel", "Ronald", "Kevin", "Steven"
    ));

    private enum Thoughts {
        THINK,
        TRY_TAKE_FORKS,
        READ_INSCRIPTION,
        EAT,
        RETURN_FORKS_BACK,
    }

    private enum ForkLocation {
        LEFT,
        RIGHT
    }

    private final Scheduler brain;
    private final Scheduler organism;
    private final String name;

    public final PublishSubject<String> mouth;
    public final BehaviorSubject<Thoughts> lastThought;
    public final Observable<Thoughts> thoughts;

    public Fork leftFork;
    public Fork rightFork;

    public Philosopher leftNeighbor;
    public Philosopher rightNeighbor;

    public Philosopher() {
        this.brain = new IoScheduler();
        this.organism = new SingleScheduler();
        this.mouth = PublishSubject.create();
        this.lastThought = BehaviorSubject.create();
        this.name = this.pickRandomName();
        this.thoughts = this.setupThoughts()
                .doOnNext(lastThought::onNext);
    }

    public void run() {
        this.thoughts
                .subscribeOn(this.organism)
                .subscribe(this::whatToDo);
    }

    private Observable<Thoughts> setupThoughts() {
        return Observable.concat(Arrays.asList(
                        Observable.just(Thoughts.THINK),
                        Observable.just(Thoughts.TRY_TAKE_FORKS),
                        Observable.just(Thoughts.READ_INSCRIPTION),
                        Observable.just(Thoughts.EAT),
                        Observable.just(Thoughts.RETURN_FORKS_BACK))
        ).repeat();
    }

    private void whatToDo(Thoughts next) {
        switch (next) {
            case THINK:
                this.think();
                break;
            case TRY_TAKE_FORKS:
                this.tryToTakeForks();
                break;
            case READ_INSCRIPTION:
                this.readInscription();
                break;
            case EAT:
                this.eat();
                break;
            case RETURN_FORKS_BACK:
                this.returnForksBack();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + next);
        }
    }

    private void think() {
        this.lastThought.onNext(Thoughts.THINK);
        this.mouth.onNext(this.logJoin("Thinking about...", this.pickRandomThought()));
        this.simulateSomeActivity();
    }

    private void tryToTakeForks() {
        while (true) {
            List<Fork> taken = this.tryToTakeBothForks();
            if (taken.size() == 2) {
                this.mouth.onNext(this.logJoin("I've took both left and right forks"));
                break;
            }

            if (taken.size() == 1) {
                Fork firstTaken = taken.get(0);
                this.mouth.onNext(this.logJoin("I've took the first", this.whereAsString(firstTaken), "fork"));
                this.think();
                this.mouth.onNext(this.logJoin("Trying to take other fork..."));
                Optional<Fork> other = this.attemptToTakeOtherFork(firstTaken);
                if (other.isPresent()) {
                    this.mouth.onNext(this.logJoin("I've took the second", this.whereAsString(other.get()), "fork"));
                    break;
                } else {
                    this.mouth.onNext(this.logJoin("I'll return first taken",
                            this.whereAsString(firstTaken), "fork because other fork is in use"));
                    firstTaken.returnBack();
                    this.think();
                }
            }

            Observable.timer(HUMAN_BRAIN_VISUAL_REACTION_TIME, TimeUnit.MILLISECONDS)
                    .observeOn(this.brain)
                    .blockingSubscribe();
        }
    }

    private void readInscription() {
        this.mouth.onNext(this.logJoin("I'm reading inscription on the left fork and it saying:", this.leftFork.niceInscription));
        this.simulateSomeActivity();
        this.mouth.onNext(this.logJoin("I'm reading inscription on the right fork and it saying:", this.rightFork.niceInscription));
        this.simulateSomeActivity();
    }

    private void eat() {
        this.mouth.onNext(this.logJoin("eating..."));
        this.simulateSomeActivity();
    }

    private void returnForksBack() {
        this.mouth.onNext(this.logJoin("returning both left and right forks back..."));
        this.leftFork.returnBack();
        this.rightFork.returnBack();
    }

    private List<Fork> tryToTakeBothForks() {
        return Stream.of(this.leftFork, this.rightFork)
                .map(Fork::take)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<Fork> attemptToTakeOtherFork(Fork firstTaken) {
        if (this.otherForkNeighborLastThought(firstTaken) == Thoughts.THINK) {
            return this.tryToTakeOtherFork(firstTaken);
        }

        return Optional.empty();
    }

    private Thoughts otherForkNeighborLastThought(Fork firstTaken) {
        switch (this.where(firstTaken)) {
            case LEFT:
                return this.rightNeighbor.lastThought.blockingFirst();
            case RIGHT:
                return this.leftNeighbor.lastThought.blockingFirst();
            default:
                throw new IllegalStateException("Unexpected value: " + this.where(firstTaken));
        }
    }

    private Optional<Fork> tryToTakeOtherFork(Fork firstTaken) {
        switch (this.where(firstTaken)) {
            case LEFT:
                return this.rightFork.take();
            case RIGHT:
                return this.leftFork.take();
            default:
                throw new IllegalStateException("Unexpected value: " + this.where(firstTaken));
        }
    }

    private ForkLocation where(Fork fork) {
        if (fork == this.leftFork) {
            return ForkLocation.LEFT;
        } else {
            return ForkLocation.RIGHT;
        }
    }

    private String whereAsString(Fork fork) {
        switch (this.where(fork)) {
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            default:
                throw new IllegalStateException("Unexpected value: " + this.where(fork));
        }
    }

    private String pickRandomThought() {
        LinkedList<String> whatCanIThink = new LinkedList<>(WHAT_CAN_PHILOSOPHER_THINK_ABOUT);
        Collections.shuffle(whatCanIThink);
        return whatCanIThink.peek();
    }

    private String pickRandomName() {
        Collections.shuffle(POSSIBLE_NAMES);
        if (POSSIBLE_NAMES.isEmpty()) {
            throw new IllegalStateException("Possible names collection is empty: cannot pick a name!");
        }
        return POSSIBLE_NAMES.poll();
    }

    private String logJoin(String... whats) {
        return Stream.concat(Stream.of(this.name + ":"), Arrays.stream(whats))
            .collect(Collectors.joining(" "));
    }

    private void simulateSomeActivity() {
        int halfActionTime = MAX_ACTION_TIME / 2;
        Observable.timer(halfActionTime + RANDOM.nextInt(halfActionTime), TimeUnit.MILLISECONDS)
                .blockingSubscribe();
    }
}
