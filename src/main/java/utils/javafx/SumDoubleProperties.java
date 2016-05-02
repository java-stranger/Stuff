package utils.javafx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberExpression;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by mac on 09/04/2016.
 */
public class SumDoubleProperties<E> extends DoubleBinding {

    Map<E, NumberExpression> itemsToSum = new HashMap<>();

    public SumDoubleProperties(ObservableList<E> items, Function<E, NumberExpression> extractor) {
        items.forEach(o -> itemsToSum.put(o, addNew(extractor.apply(o))));
        items.addListener((ListChangeListener<? super E>) change -> {
            while(change.next()) {
                change.getRemoved().forEach(o -> remove(itemsToSum.remove(o)));
                change.getAddedSubList().forEach(o -> itemsToSum.put(o, addNew(extractor.apply(o))));
                if(change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    invalidate();
                }
            }
        });
    }

    private NumberExpression addNew(NumberExpression expr) {
        bind(expr);
        return expr;
    }

    private void remove(NumberExpression expr) {
        unbind(expr);
    }

    @Override
    protected double computeValue() {
        return itemsToSum.values().stream().mapToDouble(ObservableNumberValue::doubleValue).sum();
    }
}
