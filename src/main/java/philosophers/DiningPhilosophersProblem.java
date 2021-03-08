package philosophers;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler;

import java.sql.Timestamp;

public class DiningPhilosophersProblem {

    /**
     * Great Architect of the Dining philosophers problem Universe
     * <p>
     * https://en.wikipedia.org/wiki/Great_Architect_of_the_Universe
     * https://en.wikipedia.org/wiki/Dining_philosophers_problem
     * https://en.wikipedia.org/wiki/Edsger_W._Dijkstra
     */
    private final static Scheduler GREAT_ARCHITECT = new IoScheduler();
    private final static int MAGIC_FIVE = 5;

    private static void Dijkstra() {
        Observable<Philosopher> philosophers = Observable.using(
                () -> new RoundTable(MAGIC_FIVE),
                table -> Observable.fromIterable(table.philosophers),
                table -> {
                    table.philosophers.clear();
                    table.forks.clear();
                }
        );

        philosophers
                .doOnNext(Philosopher::run)
                .flatMap(philosopher -> philosopher.mouth)
                .observeOn(GREAT_ARCHITECT)
                .blockingSubscribe(whatTheyAreTalkingAbout -> {
                    Timestamp ts = new Timestamp(System.currentTimeMillis());
                    String prettyTs = "[" + ts + "]";
                    System.out.println(prettyTs + ": Dijkstra hear something...: " + whatTheyAreTalkingAbout);
                    System.out.flush();
                });
    }

    public static void run() {
        Dijkstra();
    }
}
