package problem;


import threads.safe.NonBlockingReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

public class Fork extends NonBlockingReference<Object> {

    private final static LinkedList<String> POSSIBLE_INSCRIPTIONS = new LinkedList<>(Arrays.asList(
            "Just a nice fork",
            "Made in China",
            "Use it for eating food only",
            "Alex was here...",
            "Created by Socrates only for other philosophers"
    ));

    public final String niceInscription;

    public Fork() {
        super(Object::new);
        this.niceInscription = this.pickRandomInscription();
    }

    public Optional<Fork> take() {
        return super.use().map(object -> this);
    }

    public void returnBack() {
        super.release();
    }

    private String pickRandomInscription() {
        Collections.shuffle(POSSIBLE_INSCRIPTIONS);
        return POSSIBLE_INSCRIPTIONS.poll();
    }
}
