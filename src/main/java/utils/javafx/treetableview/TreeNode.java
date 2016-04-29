package utils.javafx.treetableview;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mac on 28/04/2016.
 */
public class TreeNode<E> extends TreeItem<E> {

    private final ObjectProperty<Comparator<E>> comparator = new SimpleObjectProperty<>();

    private final AtomicBoolean sorting = new AtomicBoolean(false);
    private InvalidationListener SORT_LISTENER =  (InvalidationListener) observable -> {
        boolean isSorting = sorting.getAndSet(true);
        if (!isSorting) {
            getChildren().sort((o1, o2) -> this.comparator.get().compare(o1.getValue(), o2.getValue()));
            sorting.getAndSet(false);
        }
    };

    public TreeNode(E e) {
        super(e);
        setExpanded(true);
        comparator.addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                getChildren().addListener(SORT_LISTENER);
            } else {
                getChildren().removeListener(SORT_LISTENER);
            }
        });
    }

    public TreeNode(E e, Comparator<E> comparator) {
        this(e);
        setComparator(comparator);
    }

    public static <E> TreeItem<E> findChild(TreeItem<E> node, E data) {
        for (TreeItem<E> child : node.getChildren()) {
            if(Objects.equals(child.getValue(), data)) {
                return child;
            }
        }
        return null;
    }

    public TreeItem<? extends E> getOrCreatePath(E... datas) {
        return getOrCreatePath(Arrays.asList(datas));
    }

    public TreeItem<? extends E> getOrCreatePath(Iterable<? extends E> datas) {
        TreeItem<E> node = this;
        for (E e : datas) {
            TreeItem<E> next = findChild(node, e);
            if(next == null) {
                next = new TreeNode<>(e, comparator.get());
                node.getChildren().add(next);
            }
            node = next;
        }

        return node;
    }

    public static <E> boolean detach(TreeItem<E> node) {
        if(node.getParent() == null) {
            return false;
        }
        node.getParent().getChildren().remove(node);
        return true;
    }

    public Comparator<E> getComparator() {
        return comparator.get();
    }

    public void setComparator(Comparator<E> comparator) {
        getChildren().forEach(eTreeItem -> {
            if(eTreeItem instanceof TreeNode) {
                ((TreeNode<E>) eTreeItem).setComparator(comparator);
            }
        });
        this.comparator.set(comparator);
    }
}
