package utils.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
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

        beans.get(0).price.set(20.); // 0 -> 20
        beans.get(1).qty.set(0.); // -200 -> 0
        assertEquals(23., price.get(), 1e-6);
        assertEquals(0., qty.get(), 1e-6);

        beans.get(1).price.set(Double.NaN);
        assertEquals(Double.NaN, price.get(), 1e-6);

        beans.get(1).price.set(3.);
        assertEquals(23., price.get(), 1e-6);

        beans.get(0).price.set(0.);

        ObservableList<Bean> beans2 = FXCollections.observableArrayList();
        SumDoubleProperties<Bean> price2 = new SumDoubleProperties<>(beans2, Bean::priceProperty);

        beans2.addAll(beans);
        beans.clear();
        assertEquals(0., price.get(), 1e-6);
        assertEquals(0., qty.get(), 1e-6);
        assertEquals(3., price2.get(), 1e-6);
    }

    class BeanAggregator {
        ObservableList<Bean> beans = FXCollections.observableArrayList();

        DoubleExpression priceSum = new SumDoubleProperties<>(beans, Bean::priceProperty);
        DoubleExpression price = priceSum.divide(Bindings.size(beans));
        DoubleExpression qty = new SumDoubleProperties<>(beans, Bean::qtyProperty).divide(Bindings.size(beans));

        public DoubleExpression priceProperty() {
            return price;
        }

        public DoubleExpression qtyProperty() {
            return qty;
        }
    }

    @Test
    public void testListOfList() throws Exception {

        ObservableList<BeanAggregator> listOfListBeans = FXCollections.observableArrayList();

        SumDoubleProperties<BeanAggregator> price = new SumDoubleProperties<>(listOfListBeans, BeanAggregator::priceProperty);

        BeanAggregator agg1 = new BeanAggregator();
        agg1.beans.add(new Bean(10, 100));

        listOfListBeans.add(agg1);

        assertEquals(10., price.get(), 1e-6);

        agg1.beans.add(new Bean(20, 100));
        assertEquals(15., price.get(), 1e-6);

        agg1.beans.get(0).price.set(8);
        assertEquals(14., price.get(), 1e-6);

        BeanAggregator agg2 = new BeanAggregator();
        listOfListBeans.add(agg2);
        assertEquals(Double.NaN, price.get(), 1e-6);

        agg2.beans.add(new Bean(50, 100));
        assertEquals(64., price.get(), 1e-6);

        agg2.beans.add(new Bean(60, 100));
        assertEquals(69., price.get(), 1e-6);

        agg1.beans.remove(0);
        assertEquals(75., price.get(), 1e-6);

        listOfListBeans.remove(agg1);
        assertEquals(55., price.get(), 1e-6);

        listOfListBeans.add(agg1);
        assertEquals(75., price.get(), 1e-6);

        agg1.beans.clear();
        assertEquals(Double.NaN, price.get(), 1e-6);

        listOfListBeans.remove(agg1);
        assertEquals(55., price.get(), 1e-6);
    }
}