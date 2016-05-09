package utils.javafx.treetableview;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by denis.kubasov on 29/04/2016.
 */
public class TreeTableViewHelper<E> {

    private static final Logger logger = LoggerFactory.getLogger(TreeTableViewHelper.class.getName());

    private final Map<E, TreeItem<E>> nodes = new HashMap<>();
    private final AtomicBoolean sortScheduled = new AtomicBoolean(false);

    public TreeTableViewHelper(TreeTableView<E> treeTableView, E root, ObservableList<? extends E> items, Callback<E, TreeItem<E>> nodeFactory, Callback<E, List<E>> pathResolver, BiConsumer<E, TreeNode<E>> nodeListener) {
        SelectionModel<E> dataLinkedSelectionModel = new SelectionModel<>(treeTableView);
        dataLinkedSelectionModel.setSelectionMode(treeTableView.getSelectionModel().getSelectionMode());

        treeTableView.setSelectionModel(dataLinkedSelectionModel);

        BiConsumer<E, TreeNode<E>> deselectOnCollapse = (e, eTreeNode) -> {
            eTreeNode.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if(!newValue) {
                    TreeNode.forAllDescendants(eTreeNode, dataLinkedSelectionModel::clearSelection);
                }
            });
        };

        if(nodeListener == null) {
            nodeListener = deselectOnCollapse;
        } else {
            nodeListener = nodeListener.andThen(deselectOnCollapse);
        }

        TreeNode<E> rootNode = new TreeNode<>(root, nodeListener);
        if(nodeListener != null) {
            nodeListener.accept(root, rootNode);
        }
        treeTableView.setRoot(rootNode);

        items.forEach(data -> {
            TreeItem<E> newNode = nodeFactory.call(data);
            rootNode.getOrCreatePath(newNode, pathResolver.call(data));
            nodes.put(data, newNode);
        });

        treeTableView.getSortOrder().addListener((InvalidationListener) c -> {
            if(treeTableView.getSortOrder().isEmpty()) {
                // need to restore the "original" order
                nodes.values().forEach(TreeNode::detach);
                items.forEach(o -> rootNode.getOrCreatePath(nodes.get(o), pathResolver.call(o)));
            }
        });

        items.addListener((ListChangeListener<? super E>) c -> {
            boolean needToSort = false;
            while(c.next()) {
                if(c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        E data = c.getList().get(i);
                        rootNode.getOrCreatePath(nodes.get(data), pathResolver.call(data));
                    }
                    needToSort = true;
                }
                for (E data : c.getRemoved()) {
                    TreeNode.detach(nodes.remove(data));
                }
                for (E data : c.getAddedSubList()) {
                    TreeItem<E> newNode = nodeFactory.call(data);
                    rootNode.getOrCreatePath(newNode, pathResolver.call(data));
                    nodes.put(data, newNode);
                    needToSort = true;
                }
            }

            if(needToSort && !treeTableView.getSortOrder().isEmpty()) {
                if(sortScheduled.compareAndSet(false, true)) {
                    // Schedule a sort for later (when the update is finished)
                    Platform.runLater(() -> {
                        treeTableView.sort();
                        sortScheduled.set(false);
                    });
                }
            }
        });
    }
}
