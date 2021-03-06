package ruffkat.hombucha.model;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import ruffkat.hombucha.measure.MeasureBridge;
import ruffkat.hombucha.money.Econometric;
import ruffkat.hombucha.money.Money;
import ruffkat.hombucha.money.MoneyBridge;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Indexed
public class Item<Q extends Quantity>
        extends Sourced
        implements Econometric<Q> {

    @Basic
    @Field
    private String reference;

    @Basic
    @Field
    @FieldBridge(impl = MoneyBridge.class)
    @Type(type = "money")
    private Money price;

    @Basic
    @Field
    @FieldBridge(impl = MeasureBridge.class)
    @Type(type = "measure")
    private Measure<Q> unit;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public Measure<Q> getUnit() {
        return unit;
    }

    public void setUnit(Measure<Q> unit) {
        this.unit = unit;
    }

    public Money unitPrice() {
        return price.divide(unit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        if (!super.equals(o)) return false;

        Item item = (Item) o;

        if (price != null ? !price.equals(item.price) : item.price != null) return false;
        if (reference != null ? !reference.equals(item.reference) : item.reference != null) return false;
        if (unit != null ? !unit.equals(item.unit) : item.unit != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (reference != null ? reference.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Item{" +
                "oid=" + getOid() +
                ", name='" + getName() + '\'' +
                ", reference='" + getReference() + '\'' +
                ", price=" + getPrice() +
                ", unit=" + getUnit() +
                '}';
    }
}
