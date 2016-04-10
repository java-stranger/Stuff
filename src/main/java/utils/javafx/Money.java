package utils.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

import java.util.Currency;
import java.util.function.Function;

/**
 * Created by mac on 09/04/2016.
 */
public class Money {
    private final DoubleExpression amount;
    private final ObjectExpression<Currency> ccy;

    public Money(double amount, Currency ccy) {
        this.amount = new SimpleDoubleProperty(amount);
        this.ccy = new SimpleObjectProperty<>(ccy);
    }

    public Money(DoubleExpression amount, Currency ccy) {
        this.amount = amount;
        this.ccy = new SimpleObjectProperty<>(ccy);
    }

    public Money(DoubleExpression amount, ObjectExpression<Currency> ccy) {
        this.amount = amount;
        this.ccy = ccy;
    }

    public Money(double amount, ObjectExpression<Currency> ccy) {
        this.amount = new SimpleDoubleProperty(amount);
        this.ccy = ccy;
    }

    public Number getAmount() {
        return amount.get();
    }

    public DoubleExpression amountProperty() {
        return amount;
    }

    public StringExpression formattedProperty(String format) {
        return Bindings.createStringBinding(() -> String.format(format + "%s", amount.get(), ccy.get().getSymbol()), amount, ccy);
    }

    public DoubleExpression inCurrency(Currency ccy) {
        return amount.multiply(getRate(this.ccy, ccy));
    }

    public DoubleExpression inCurrency(ObjectExpression<Currency> ccy) {
        DoubleExpression rate = getRate(this.ccy, ccy);
        return amount.multiply(rate);
    }

    public Money convert(ObjectExpression<Currency> ccy) {
        return new Money(this.inCurrency(ccy), ccy);
    }

    public static <E> Money sum(ObservableList<E> list, Function<E, Money> extractor, Currency ccy) {
        DoubleExpression amount = new SumDoubleProperties<>(list, e -> extractor.apply(e).inCurrency(ccy));
        return new Money(amount, ccy);
    }

    public static <E> Money sum(ObservableList<E> list, Function<E, Money> extractor, ObjectExpression<Currency> ccy) {
        DoubleExpression amount = new SumDoubleProperties<>(list, e -> extractor.apply(e).inCurrency(ccy));
        return new Money(amount, ccy);
    }

    public static DoubleExpression getRate(Currency ccy1, Currency ccy2) {
        return Bindings.createDoubleBinding(() -> getFixedRate(ccy1, ccy2));
    }

    public static DoubleExpression getRate(ObjectExpression<Currency> ccy1, ObjectExpression<Currency> ccy2) {
        return Bindings.createDoubleBinding(() -> getFixedRate(ccy1.get(), ccy2.get()), ccy1, ccy2);
    }

    public static DoubleExpression getRate(Currency ccy1, ObjectExpression<Currency> ccy2) {
        return Bindings.createDoubleBinding(() -> getFixedRate(ccy1, ccy2.get()), ccy2);
    }

    public static DoubleExpression getRate(ObjectExpression<Currency> ccy1, Currency ccy2) {
        return Bindings.createDoubleBinding(() -> getFixedRate(ccy1.get(), ccy2), ccy1);
    }

    public static double getFixedRate(Currency ccy1, Currency ccy2) {
        if (ccy1.equals(ccy2)) {
            return 1.;
        } else if (ccy1.getCurrencyCode().equals("USD") && ccy2.getCurrencyCode().equals("EUR")) {
            return 1. / 1.2;
        } else if (ccy1.getCurrencyCode().equals("EUR") && ccy2.getCurrencyCode().equals("USD")) {
            return 1.2;
        }
        throw new IllegalArgumentException("Unknown currency " + ccy1.getCurrencyCode() + " vs " + ccy2.getCurrencyCode());
    }

    public Money multiply(DoubleExpression mul) {
        return new Money(amount.multiply(mul), ccy);
    }

    public Money add(Money add) {
        if(!ccy.equals(add.ccy)) {
            throw new IllegalArgumentException("Cannot add money in ccy " + add.ccy.get().getCurrencyCode() + " to amount in " + this.ccy.get().getCurrencyCode());
        }
        return new Money(amount.add(add.amount), ccy);
    }

    public Money subtract(Money subtract) {
        if(!ccy.equals(subtract.ccy)) {
            throw new IllegalArgumentException("Cannot subtract money in ccy " + subtract.ccy.get().getCurrencyCode() + " from amount in " + this.ccy.get().getCurrencyCode());
        }
        return new Money(amount.subtract(subtract.amount), ccy);
    }

    public DoubleExpression divide(Money div) {
        if(!ccy.equals(div.ccy)) {
            throw new IllegalArgumentException("Cannot divide money in ccy " + div.ccy.get().getCurrencyCode() + " by amount in " + this.ccy.get().getCurrencyCode());
        }
        return amount.divide(div.amount);
    }
}
