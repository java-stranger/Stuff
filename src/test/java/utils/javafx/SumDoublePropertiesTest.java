package utils.javafx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Test;
import utils.javafx.SumDoubleProperties;

import static org.junit.Assert.*;

/**
 * Created by mac on 09/04/2016.
 */
public class SumDoublePropertiesTest {

    class Bean {
        DoubleProperty price = new SimpleDoubleProperty(0);
        DoubleProperty qty = new SimpleDoubleProperty(0);

        public Bean(double price, double qty) {
            this.price.set(price);
            this.qty.set(qty);
        }

        public double getPrice() {
            return price.get();
        }

        public DoubleProperty priceProperty() {
            return price;
        }

        public double getQty() {
            return qty.get();
        }

        public DoubleProperty qtyProperty() {
            return qty;
        }
    }

    @Test
    public void testSum() throws Exception {

        ObservableList<Bean> beans = FXCollections.observableArrayList();

        SumDoubleProperties<Bean> price = new SumDoubleProperties<>(beans, Bean::priceProperty);
        SumDoubleProperties<Bean> qty = new SumDoubleProperties<>(beans, Bean::qtyProperty);

        assertEquals(0., price.get(), 1e-6);
        assertEquals(0., qty.get(), 1e-6);

        beans.add(new Bean(1., 100.));
        assertEquals(1., price.get(), 1e-6);
        assertEquals(100., qty.get(), 1e-6);

        beans.add(new Bean(3., 200.));
        assertEquals(4., price.get(), 1e-6);
        assertEquals(300., qty.get(), 1e-6);

        beans.add(0, new Bean(-2., 0.));
        assertEquals(2., price.get(), 1e-6);
        assertEquals(300., qty.get(), 1e-6);

        beans.remove(1);
        beans.get(0).price.set(0.); // -2 -> 0
        beans.get(1).qty.set(-200.); // 200 -> -200
        assertEquals(3., price.get(), 1e-6);
        assertEquals(-200., qty.get(), 1e-6);


        ObservableList<Bean> beans2 = FXCollections.observableArrayList();
        SumDoubleProperties<Bean> price2 = new SumDoubleProperties<>(beans2, Bean::priceProperty);

        beans2.addAll(beans);
        beans.clear();
        assertEquals(0., price.get(), 1e-6);
        assertEquals(0., qty.get(), 1e-6);
        assertEquals(3., price2.get(), 1e-6);

    }
}