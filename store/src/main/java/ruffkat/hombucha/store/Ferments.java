package ruffkat.hombucha.store;

import ruffkat.hombucha.model.Ferment;

import java.util.Set;

public interface Ferments {
    Ferment create();
    Ferment load(Long id);
    void delete(Long id);
    Set<Ferment> active();
}
