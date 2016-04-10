package utils.javafx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by mac on 09/04/2016.
 */
public class SumDoubleProperties<E> extends DoubleBinding {

    Map<E, DoubleExpression> itemsToSum = new HashMap<>();
    private double sum = 0.;

    private final ChangeListener<Number> VALUE_CHANGE_LISTENER = (observable, oldValue, newValue) -> {
        sum += newValue.doubleValue() - oldValue.doubleValue();
    };

    public SumDoubleProperties(ObservableList<E> items, Function<E, DoubleExpression> extractor) {
        items.forEach(o -> itemsToSum.put(o, addNewItem(extractor.apply(o))));
        items.addListener((ListChangeListener<? super E>) observable -> {
            while(observable.next()) {
                observable.getRemoved().forEach(o -> removeItem(itemsToSum.remove(o)));
                observable.getAddedSubList().forEach(o -> itemsToSum.put(o, addNewItem(extractor.apply(o))));
//                if (observable.wasRemoved()) {
//                    recalculate();
//                }
            }
        });
    }

    private DoubleExpression addNewItem(DoubleExpression e) {
        e.addListener(VALUE_CHANGE_LISTENER);
        bind(e);
        sum += e.get();
        invalidate();
        return e;
    }

    private void removeItem(DoubleExpression e) {
        unbind(e);
        e.removeListener(VALUE_CHANGE_LISTENER);
        sum -= e.get();
        invalidate();
    }


    private void recalculate() {
        sum = itemsToSum.values().stream().mapToDouble(DoubleExpression::doubleValue).sum();
        invalidate();
    }

    @Override
    protected double computeValue() {
        return sum;
    }
}
