package utils.javafx;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A helper class that allows to bind a {@code {@link TextField}} to a collection of beans, to perform a 'search-as-you-type'
 * in this collection among all the fields of the bean. Relies on a JSON representation of the bean.
 *
 * Created by denis.kubasov on 06/05/2016.
 */
public class SearchBox {

    private static final Logger logger = LoggerFactory.getLogger(SearchBox.class.getName());
    private final static Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    /**
     * The matching function of the search, called on a text (json value) and a pattern (the {@code TextField} contents)
     * @return true if the {@code text} matches the {@code pattern}
     */
    private static boolean match(String pattern, String text) {
        return text.toLowerCase().contains(pattern.toLowerCase());
    }

    /**
     * The corner-stone of the search, populating a set of fields of the bean that match the given search pattern.
     * @param json JSON representation of a bean
     * @param matched a set of matching fields (to be populated), will be empty if none matched
     * @param value search pattern
     * @return true if any field matches
     */
    private static boolean jsonFindValue(JsonElement json, Set<Map.Entry<String, JsonElement>> matched, String value) {
        matched.clear();
        if(json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(stringJsonElementEntry -> {
                if(match(value, stringJsonElementEntry.getValue().getAsString())) {
                    matched.add(stringJsonElementEntry);
                }
            });
        }
        return !matched.isEmpty();
    }

    private static class MatchedItem {
        private final Set<Map.Entry<String, JsonElement>> matchedFields = Sets.newHashSet();
    }

    /**
     * Binds a {@code {@link TextField}} to a collection of beans, to perform a 'search-as-you-type'
     * in this collection among all the fields of the bean. Relies on a JSON representation of the bean.
     *
     * @param node {@code {@link TextField}} representing the search input
     * @param items collection of beans to search in
     * @param name a callback to get the 'name' (or description) of a bean
     * @param onSelected a callback indicating which bean the user has selected
     * @param <T> bean class
     * @return a {@code {@link ListView<T>}} object that is used to display the matching result. The caller is free
     * to adjust its size or opacity to his taste.
     */
    public static <T> ListView<T> attachToTextField(TextField node, Collection<T> items, Callback<T, String> name, BiConsumer<T, JsonElement> onSelected) {
        return attachToTextField(node, FXCollections.observableArrayList(items), name, onSelected);
    }

    /**
     * Binds a {@code {@link TextField}} to an observable list of beans, to perform a 'search-as-you-type'
     * in this collection among all the fields of the bean. Relies on a JSON representation of the bean.
     * Will adapt to changes in the observable list (new elements are added, old ones removed, and for the updated ones
     * the JSON representation will be regenerated)
     *
     * @param node {@code {@link TextField}} representing the search input
     * @param items collection of beans to search in
     * @param name a callback to get the 'name' (or description) of a bean
     * @param onSelected a callback indicating which bean the user has selected
     * @param <T> bean class
     * @return a {@code {@link ListView<T>}} object that is used to display the matching result. The caller is free
     * to adjust its size or opacity to his taste.
     */
    public static <T> ListView<T> attachToTextField(TextField node, ObservableList<T> items, Callback<T, String> name, BiConsumer<T, JsonElement> onSelected) {
        FilteredList<T> itemsFiltered = items.filtered(item -> false);

        ObservableMap<T, JsonElement> itemsJsoned = FXCollections.observableHashMap();
        items.addListener((ListChangeListener<? super T>) c -> {
            while(c.next()) {
                c.getAddedSubList().forEach(o -> itemsJsoned.put(o, gson.toJsonTree(o)));
                c.getRemoved().forEach(itemsJsoned::remove);
                if(c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        itemsJsoned.put(c.getList().get(i), gson.toJsonTree(c.getList().get(i)));
                    }
                }
            }
        });
        items.forEach(o -> itemsJsoned.put(o, gson.toJsonTree(o)));

        ObservableMap<T, MatchedItem> matched = FXCollections.observableHashMap();
        node.textProperty().addListener((observable, oldValue, newValue) -> {
            if(Strings.isNullOrEmpty(newValue)) {
                itemsFiltered.setPredicate(t -> false);
            } else {
                itemsFiltered.setPredicate(bean -> jsonFindValue(itemsJsoned.get(bean), matched.computeIfAbsent(bean, t -> new MatchedItem()).matchedFields, newValue));
            }
        });

        ListView<T> list = new ListView<>();
        list.setItems(itemsFiltered);
        Popup stage = new Popup();
        stage.getContent().add(list);
        stage.setAutoHide(true);
        stage.setHideOnEscape(true);

        list.setPrefHeight(300.);
        list.setPrefWidth(600.);
        list.setOpacity(.8);

        list.setCellFactory(param -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty && item  != null) {
                    Label nameLabel = new Label(name.call(item));
                    TextFlow label = new TextFlow(nameLabel);
                    nameLabel.setMaxWidth(USE_PREF_SIZE);
                    nameLabel.setPrefWidth(150);
                    nameLabel.setEllipsisString("...");
                    matched.get(item).matchedFields.forEach(o -> {
                        Label fieldName = new Label(" " + o.getKey() + ": ");
                        fieldName.setStyle("-fx-font-weight:bold;");
                        label.getChildren().addAll(fieldName, new Label(o.getValue().toString()));
                    });
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        });

        list.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    logger.debug("Selected " + list.getSelectionModel().getSelectedItem());
                    T selected = list.getSelectionModel().getSelectedItem();
                    onSelected.accept(selected, itemsJsoned.get(selected));
                    stage.hide();
                    break;
                case ESCAPE:
//                    logger.info("Hiding!");
                    stage.hide();
                    break;
            }
        });

        list.setOnMouseClicked(event -> {
            T selected = list.getSelectionModel().getSelectedItem();
            onSelected.accept(selected, itemsJsoned.get(selected));
            logger.debug("Mouse clicked on " + list.getSelectionModel().getSelectedItem());
        });

        node.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.ESCAPE) || event.getCode().equals(KeyCode.ENTER)) {
                return;
            }
            stage.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
            Point2D p = node.localToScene(0.0, 0.0);
            stage.show(node.getScene().getWindow(), node.getScene().getX() + node.getScene().getWindow().getX() + p.getX(), node.getScene().getY() + node.getScene().getWindow().getY() + p.getY() + node.getHeight());
        });

        return list;
    }
}
