package utils.javafx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Test;
import org.pmw.tinylog.Logger;
import utils.javafx.Money;

import java.util.Currency;

import static org.junit.Assert.assertEquals;

/**
 * Created by mac on 09/04/2016.
 */
public class MoneyTest {

    ObjectProperty<Currency> defCurrency = new SimpleObjectProperty<>(Currency.getInstance("USD"));

    class Order {
        DoubleProperty price = new SimpleDoubleProperty();
        DoubleProperty quantity = new SimpleDoubleProperty();
        Money value;

        public Order(double price, double qty) {
            this.price.set(price);
            quantity.set(qty);

            value = new Money(quantity.multiply(this.price), Currency.getInstance("USD"));
        }

        public Order(Money price, double qty) {
            this.price.bind(price.amountProperty());
            quantity.set(qty);

            value = price.multiply(quantity);
        }


        public Money getValue() {
            return value;
        }
    }

    class OrderBook {
        ObservableList<Order> orders = FXCollections.observableArrayList();
        Money value = Money.sum(orders, order -> order.value, defCurrency);

        public void addOrder(Order order) {
            orders.add(order);
        }

        public void removeOrder(Order order) {
            orders.remove(order);
        }
    }

    @Test
    public void testBook() throws Exception {

        Order o1 = new Order(10, 250);
        Order o2 = new Order(10.1, 1000);
        Order o3 = new Order(9.99, 100);

        assertEquals(2500., o1.getValue().getAmount().doubleValue(), 1e-6);
        assertEquals(10100., o2.getValue().getAmount().doubleValue(), 1e-6);
        assertEquals(999., o3.getValue().getAmount().doubleValue(), 1e-6);

        OrderBook book = new OrderBook();
        book.addOrder(o1);
        book.addOrder(o3);

        assertEquals(2500 + 999., book.value.getAmount().doubleValue(), 1e-6);

        o1.quantity.set(500);
        assertEquals(5000 + 999., book.value.getAmount().doubleValue(), 1e-6);
        o3.price.set(10.);
        assertEquals(5000 + 1000., book.value.getAmount().doubleValue(), 1e-6);

        o1.quantity.set(9999);
        o1.price.set(-192);
        book.removeOrder(o1);
        book.addOrder(o2);
        o2.price.set(10.2);
        assertEquals(10200. + 1000., book.value.getAmount().doubleValue(), 1e-6);
    }

    @Test
    public void testCcy() throws Exception {

        Order o1 = new Order(10, 250);

        Logger.info("o1 value: " + o1.value.formattedProperty("%.2f").get());

        ObjectProperty<Currency> orderCurrency = new SimpleObjectProperty<>(Currency.getInstance("EUR"));
        Order o2 = new Order(new Money(10, orderCurrency), 250);
        Money o2Val = o2.value;
        Money o2ValDefCcy = o2.value.convert(defCurrency);
        Logger.info("o2 value: " + o2Val.formattedProperty("%.2f").get());
        Logger.info("o2 value (def ccy): " + o2ValDefCcy.formattedProperty("%.2f").get());

        orderCurrency.set(Currency.getInstance("USD"));
        Logger.info("o2 value: " + o2Val.formattedProperty("%.2f").get());
        Logger.info("o2 value (def ccy): " + o2ValDefCcy.formattedProperty("%.2f").get());
        orderCurrency.set(Currency.getInstance("EUR"));

        OrderBook book = new OrderBook();
        book.addOrder(o1);
        book.addOrder(o2);

        Logger.info("book value: " + book.value.formattedProperty("%.2f").get());
        defCurrency.set(Currency.getInstance("EUR"));
        Logger.info("book value: " + book.value.formattedProperty("%.2f").get());
        Logger.info("o2 value (def ccy): " + o2ValDefCcy.formattedProperty("%.2f").get());

    }
}