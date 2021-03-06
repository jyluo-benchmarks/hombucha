package ruffkat.hombucha.model;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.IndexedEmbedded;
import ruffkat.hombucha.measure.MeasureBridge;
import ruffkat.hombucha.measure.Measurements;
import ruffkat.hombucha.money.Money;
import ruffkat.hombucha.money.Priced;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Quantity;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.OneToOne;
import java.io.Serializable;

@Embeddable
public class Ingredient<Q extends Quantity>
        implements Serializable, Priced {

    @OneToOne
    @IndexedEmbedded
    private Item<Q> item;

    @Basic
    @Field
    @FieldBridge(impl = MeasureBridge.class)
    @Type(type = "measure")
    private Measure<Q> amount;

    @Basic
    @Field
    private String instructions;

    public Ingredient() {
        this(null, null, null);
    }

    public Ingredient(Item<Q> item, Measure<Q> amount) {
        this.item = item;
        this.amount = amount;
    }

    public Ingredient(Item<Q> item, Measure<Q> amount, String instructions) {
        this.item = item;
        this.amount = amount;
        this.instructions = instructions;
    }

    public Item<Q> getItem() {
        return item;
    }

    public void setItem(Item<Q> item) {
        this.item = item;
    }

    public Measure<Q> getAmount() {
        return amount;
    }

    public void setAmount(Measure<Q> amount) {
        this.amount = amount;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Ingredient<Q> scale(UnitConverter converter) {
        Ingredient<Q> ingredient = new Ingredient<Q>();
        ingredient.setItem(getItem());
        ingredient.setAmount(Measurements.convert(getAmount(), converter));
        ingredient.setInstructions(getInstructions());
        return ingredient;
    }

    public Money price() {
        Money price = item.unitPrice();
        return price.multiply(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ingredient that = (Ingredient) o;

        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        if (item != null ? !item.equals(that.item) : that.item != null) return false;
        if (instructions != null ? !instructions.equals(that.instructions) : that.instructions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (instructions != null ? instructions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "item=" + item +
                ", amount=" + amount +
                ", instructions='" + instructions + '\'' +
                '}';
    }
}
