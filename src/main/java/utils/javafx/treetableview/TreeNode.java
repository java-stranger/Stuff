package utils.javafx.treetableview;

import com.google.common.collect.Lists;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by mac on 28/04/2016.
 */
public class TreeNode<E> extends TreeItem<E> {

    private final ObjectProperty<Comparator<E>> comparator = new SimpleObjectProperty<>();
    private final BiConsumer<E, TreeNode<E>> nodeListener;
    private final List<TreeItem<E>> emptyChildren = Lists.newArrayList();

    private final AtomicBoolean sorting = new AtomicBoolean(false);
    private InvalidationListener SORT_LISTENER =  (InvalidationListener) observable -> {
        boolean isSorting = sorting.getAndSet(true);
        if (!isSorting) {
            getChildren().sort((o1, o2) -> this.comparator.get().compare(o1.getValue(), o2.getValue()));
            sorting.getAndSet(false);
        }
    };

    public TreeNode(E e) {
        this(e, null);
    }

    public TreeNode(E e, BiConsumer<E, TreeNode<E>> nodeListener) {
        super(e);
        this.nodeListener = nodeListener;
        setExpanded(true);
        comparator.addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                getChildren().addListener(SORT_LISTENER);
            } else {
                getChildren().removeListener(SORT_LISTENER);
            }
        });
    }

    public TreeNode(E e, Comparator<E> comparator, BiConsumer<E, TreeNode<E>> nodeListener) {
        this(e, nodeListener);
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

    public static <E> TreeItem<E> findRestoreHiddenChild(TreeNode<E> node, E data) {
        for (TreeItem<E> child : node.emptyChildren) {
            if(Objects.equals(child.getValue(), data)) {
                node.getChildren().add(child);
                return child;
            }
        }
        return null;
    }

    public TreeItem<? extends E> getOrCreatePath(TreeItem<? extends E> child, E... datas) {
        return getOrCreatePath(child, Arrays.asList(datas));
    }

    public TreeItem<? extends E> getOrCreatePath(TreeItem<? extends E> leaf, Iterable<? extends E> datas) {
        TreeItem<E> node = this;
        for (E e : datas) {
            TreeItem<E> next = findChild(node, e);
            if(next == null && (node instanceof TreeNode)){
                next = findRestoreHiddenChild((TreeNode<E>)node, e);
            }
            if(next == null) {
                next = new TreeNode<>(e, comparator.get(), nodeListener);
                if(nodeListener != null) {
                    nodeListener.accept(e, (TreeNode<E>) next);
                }
                node.getChildren().add(next);
                TreeItem<E> parent = node;
                TreeItem<E> child = next;
                child.getChildren().addListener((InvalidationListener) observable -> {
                    if(child.getChildren().isEmpty()) {
                        parent.getChildren().remove(child);
                        if(parent instanceof TreeNode) {
                            ((TreeNode<E>) parent).emptyChildren.add(child);
                        }
                    }
                });
            }
            node = next;
        }

        if(!node.equals(leaf.getParent())) {
            TreeNode.detach(leaf);
            node.getChildren().add((TreeItem) leaf);
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

    public static <E> void forAllDescendants(TreeItem<E> node, Consumer<ObservableList<TreeItem<E>>> consumer) {
        consumer.accept(node.getChildren());
        node.getChildren().forEach(eTreeItem -> forAllDescendants(eTreeItem, consumer));
    }
}
