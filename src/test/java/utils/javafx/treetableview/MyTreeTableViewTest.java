package utils.javafx.treetableview;

import com.google.common.base.Strings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.pmw.tinylog.Logger;
import org.slf4j.LoggerFactory;
import utils.javafx.SumDoubleProperties;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by denis.kubasov on 27/04/2016.
 */
public class MyTreeTableViewTest extends Application {

    enum OrderSide {
        BUY, SELL
    }

    static TreeTableView<AbstractData> treeTableView;

    public static void main(String[] args) {
        MyTreeTableViewTest.launch();
    }

    Random rand = new Random(15487954231L);
    private ObservableList<Data> getContent() {
        ObservableList<Data> list = FXCollections.observableArrayList(param -> new Observable[] {param.basketProperty(), param.sideProperty(), param.priceProperty()});

        for (int i = 0; i < 1000; i++) {
            list.add(new Data("bean " + i, rand.nextBoolean() ? OrderSide.BUY : OrderSide.SELL, 1000. + rand.nextInt(101), rand.nextBoolean() ? "EU" : "US"));
        }
        return list;
    }

    static <E, R> ObservableList<R> syncList(ObservableList<E> source, Function<E, R> transform) {
        ObservableList<R> list = FXCollections.observableArrayList();
        source.forEach(e -> list.add(transform.apply(e)));

        source.addListener((ListChangeListener<? super E>) c -> {
            while(c.next()) {
                if(c.wasRemoved()) {
                    list.remove(c.getFrom(), c.getFrom() + c.getRemovedSize());
                }
                if(c.wasAdded()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        list.add(i, transform.apply(c.getList().get(i)));
                    }
                }
                if(c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        list.set(i, transform.apply(c.getList().get(i)));
                    }
                }

                if(c.wasPermutated()) {
                    List<R> cp = new ArrayList<>(list);
                    for (int i = 0; i < c.getList().size(); i++) {
                        list.set(c.getPermutation(i), cp.get(i));
                    }
                }
            }
        });
        return list;
    }

    static <E, R> ObservableList<R> syncList(ObservableList<E> source, Function<E, R> transform, Callback<R, javafx.beans.Observable[]> extractor) {
        ObservableList<R> list = FXCollections.observableArrayList(extractor);
        source.forEach(e -> list.add(transform.apply(e)));

        source.addListener((ListChangeListener<? super E>) c -> {
            while(c.next()) {
                if(c.wasRemoved()) {
                    list.remove(c.getFrom(), c.getFrom() + c.getRemovedSize());
                }
                if(c.wasAdded()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        list.add(i, transform.apply(c.getList().get(i)));
                    }
                }
                if(c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        list.set(i, transform.apply(c.getList().get(i)));
                    }
                }

                if(c.wasPermutated()) {
                    List<R> cp = new ArrayList<>(list);
                    for (int i = 0; i < c.getList().size(); i++) {
                        list.set(c.getPermutation(i), cp.get(i));
                    }
                }
            }
        });
        return list;
    }

    private <E, U> void smartInsert(List<E> reference, List<U> target, U element, BiFunction<E, U, Boolean> comparator) {
        int itRef = 0;
        int itTgt = 0;

        while(itTgt < target.size()) {
            while(itRef < reference.size()) {
                if(comparator.apply(reference.get(itRef), element)) {
                    target.add(itTgt, element);
                    return;
                } else if(comparator.apply(reference.get(itRef), target.get(itTgt))) {
                    break;
                } else {
                    ++itRef;
                }
            }
            if(itRef == reference.size()) {
                throw new IllegalArgumentException("Reference list does not contain " + target.get(itTgt));
            }
            ++itRef;
            ++itTgt;
        }

        target.add(itTgt, element);
    }

    Timer timer = new Timer();

    @Override
    public void stop() throws Exception {
        timer.cancel();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        treeTableView = new TreeTableView<>();
        {
            TreeTableColumn<AbstractData, String> columnName = new TreeTableColumn<>("Name");
            columnName.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
            treeTableView.getColumns().add(columnName);
        }
        {
            TreeTableColumn<AbstractData, OrderSide> columnsSide = new TreeTableColumn<>("Side");
            columnsSide.setCellValueFactory(param -> param.getValue().getValue().sideProperty());
            treeTableView.getColumns().add(columnsSide);
        }
        {
            TreeTableColumn<AbstractData, Number> columnsPrice = new TreeTableColumn<>("Price");
            columnsPrice.setCellValueFactory(param -> param.getValue().getValue().priceProperty());
            treeTableView.getColumns().add(columnsPrice);
        }
        {
            TreeTableColumn<AbstractData, String> basketName = new TreeTableColumn<>("Basket");
            basketName.setCellValueFactory(param -> param.getValue().getValue().basketProperty());
            treeTableView.getColumns().add(basketName);
        }

        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.setShowRoot(false);

//        TreeTableViewHelper<AbstractData> helper = new TreeTableViewHelper<>(this::createPath, this::getNodeForPath, MyTreeItemFactory::createNode);

        ObservableList<Data> dataItems = getContent();
        FilteredList<Data> items = dataItems.sorted(Comparator.comparing(Data::getPrice)).filtered(data -> true);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!dataItems.isEmpty()) {
                    int ind = rand.nextInt(dataItems.size());
                    long ts = System.nanoTime();
                    Platform.runLater(() -> {
                        long now = System.nanoTime();
                        if(now - ts > 10000000) {
                            Logger.info("Lag >10ms: " + (now - ts) / 1000000 + "ms");
                        }
                        dataItems.get(ind).setPrice(dataItems.get(ind).getPrice() + rand.nextInt(51) - 25);
                    });
                }
//                logger.info("setting price of " + items.get(ind).getName() + " to " + items.get(ind).getPrice());
            }
        }, 50, 50);

        Callback<AbstractData, List<AbstractData>> pathResolver = data -> FXCollections.observableArrayList(new DataGroup(((Data)data).getBasket()), new DataGroup(((Data)data).getSide().name()));
        BiConsumer<AbstractData, TreeNode<AbstractData>> nodeConsumer = (abstractData, abstractDataTreeNode) -> Bindings.bindContent(((DataGroup) abstractData).getChildren(), abstractDataTreeNode.getChildren());
        TreeTableViewHelper<AbstractData> helper3 = new TreeTableViewHelper<>(treeTableView, new DataGroup("All"), items, TreeNode::new, pathResolver, nodeConsumer);

        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        HBox buttons = new HBox();
        TextField filter = new TextField();
        VBox vbox = new VBox(filter, treeTableView, buttons);
        vbox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        buttons.setPrefHeight(Region.USE_COMPUTED_SIZE);
        treeTableView.setPrefHeight(600);
        treeTableView.setPrefWidth(500);
        treeTableView.setMaxHeight(Double.MAX_VALUE);

        filter.textProperty().addListener((observable, oldValue, newValue) -> {
            if(Strings.isNullOrEmpty(newValue)) {
                items.setPredicate(data -> true);
            } else {
                items.setPredicate(data -> data.toString().toLowerCase().contains(newValue.toLowerCase()));
            }
        });

        VBox.setVgrow(vbox, Priority.ALWAYS);
        VBox.setVgrow(treeTableView, Priority.ALWAYS);
        vbox.setMaxHeight(Double.MAX_VALUE);
        final Scene scene = new Scene(vbox);

        Button button = new Button("Change EU-US");
        button.setOnAction(event -> {
            if (items.get(0).getBasket().equals("EU")) {
                items.get(0).setBasket("US");
            } else {
                items.get(0).setBasket("EU");
            }
        });

        Button button1 = new Button("Remove first");
        button1.setOnAction(event -> {
            items.remove(0);
        });

        AtomicInteger i = new AtomicInteger(1);
        Button button2 = new Button("Add");
        button2.setOnAction(event -> {
            dataItems.add(new Data("added " + i.incrementAndGet(), OrderSide.BUY, 9.99, "US"));
        });

        Button buttonSort = new Button("Sort");
        AtomicBoolean asc = new AtomicBoolean(true);
        buttonSort.setOnAction(event -> {
            Comparator<Data> comparator = Comparator.comparing(t -> t.getPrice());
            boolean ascending = asc.get();
            items.sort(ascending ? comparator : comparator.reversed());
            asc.set(!ascending);
            Logger.info("Sorted ASC " + ascending + ": " + items);

        });

        buttons.getChildren().add(button);
        buttons.getChildren().add(button1);
        buttons.getChildren().add(button2);
        buttons.getChildren().add(buttonSort);
        primaryStage.setScene(scene);
        primaryStage.show();
    }



    public static class AbstractData {
        public StringExpression nameProperty() {
            return null;
        }

        public ObjectProperty<OrderSide> sideProperty() {
            return null;
        }

        public DoubleExpression priceProperty() {
            return null;
        }

        public StringProperty basketProperty() {
            return null;
        }

        @Override
        public String toString() {
            return nameProperty() != null ? nameProperty().get() : "null";
        }
    }

    public static class DataGroup extends AbstractData {
        private final String name;

        private final ObservableList<TreeItem<AbstractData>> list = FXCollections.observableArrayList();
        private final NumberExpression listSize = Bindings.size(list);
        private final DoubleExpression price = new SumDoubleProperties<>(list, abstractDataTreeItem -> abstractDataTreeItem.getValue().priceProperty());//.divide(Bindings.size(list));
        private final DoubleExpression avgPrice = price.divide(listSize);

        public DataGroup(String name) {
            this.name = name;
        }

        public StringExpression nameProperty() {
            return Bindings.createStringBinding(() -> name);
        }

        public ObservableList<TreeItem<AbstractData>> getChildren() {
            return list;
        }

        @Override
        public DoubleExpression priceProperty() {
            return avgPrice;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DataGroup)) return false;

            DataGroup dataGroup = (DataGroup) o;

            return name != null ? name.equals(dataGroup.name) : dataGroup.name == null;

        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class Data extends AbstractData {

        StringProperty name = new SimpleStringProperty();
        ObjectProperty<OrderSide> side = new SimpleObjectProperty<>();
        DoubleProperty price = new SimpleDoubleProperty();
        StringProperty basket = new SimpleStringProperty();

        public Data(String name, OrderSide side, double price, String basket) {
            this.name.set(name);
            this.side.set(side);
            this.price.set(price);
            this.basket.set(basket);
        }

        public String getName() {
            return name.get();
        }

        public StringExpression nameProperty() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public OrderSide getSide() {
            return side.get();
        }

        public ObjectProperty<OrderSide> sideProperty() {
            return side;
        }

        public void setSide(OrderSide side) {
            this.side.set(side);
        }

        public double getPrice() {
            return price.get();
        }

        public DoubleExpression priceProperty() {
            return price;
        }

        public void setPrice(double price) {
            this.price.set(price);
        }

        public String getBasket() {
            return basket.get();
        }

        public StringProperty basketProperty() {
            return basket;
        }

        public void setBasket(String basket) {
            this.basket.set(basket);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "name=" + name.get() +
                    ", side=" + side.get().name() +
                    ", price=" + price.get() +
                    ", basket=" + basket.get() +
                    '}';
        }
    }
}
