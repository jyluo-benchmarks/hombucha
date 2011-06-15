package ruffkat.hombucha.model;

import org.hibernate.annotations.Type;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.time.Instant;

@Entity
public class Sample<Q extends Quantity>
        extends Persistent {

    @OneToOne(cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH, CascadeType.DETACH})
    private Ferment ferment;

    @Basic
    @Type(type = "instant")
    private Instant taken;

    @Basic
    @Type(type = "measure")
    private Measure<Q> measurement;

    public Ferment getFerment() {
        return ferment;
    }

    public void setFerment(Ferment ferment) {
        this.ferment = ferment;
    }

    public Instant getTaken() {
        return taken;
    }

    public void setTaken(Instant taken) {
        this.taken = taken;
    }

    public Measure<Q> getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measure<Q> measurement) {
        this.measurement = measurement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sample)) return false;

        Sample sample = (Sample) o;

        if (ferment != null ? !ferment.equals(sample.ferment) : sample.ferment != null) return false;
        if (measurement != null ? !measurement.equals(sample.measurement) : sample.measurement != null) return false;
        if (taken != null ? !taken.equals(sample.taken) : sample.taken != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ferment != null ? ferment.hashCode() : 0;
        result = 31 * result + (taken != null ? taken.hashCode() : 0);
        result = 31 * result + (measurement != null ? measurement.hashCode() : 0);
        return result;
    }
}
