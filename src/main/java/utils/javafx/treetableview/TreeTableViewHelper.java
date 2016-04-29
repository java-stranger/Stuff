package utils.javafx.treetableview;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by denis.kubasov on 29/04/2016.
 */
public class TreeTableViewHelper<E> {

    private final Map<E, TreeItem<E>> nodes = new HashMap<>();

    public TreeTableViewHelper(TreeTableView<E> treeTableView, E root, ObservableList<? extends E> items, Callback<E, TreeItem<E>> nodeFactory, Callback<E, ObservableList<E>> pathResolver) {
        SelectionMode selectionMode = treeTableView.getSelectionModel().getSelectionMode();
        treeTableView.setSelectionModel(new SelectionModel<>(treeTableView));
        treeTableView.getSelectionModel().setSelectionMode(selectionMode);

        TreeNode<E> rootNode = new TreeNode<E>(root);
        treeTableView.setRoot(rootNode);

        items.forEach(data -> {
            TreeItem<? extends E> parent = rootNode.getOrCreatePath(pathResolver.call(data));
            TreeItem<E> newNode = nodeFactory.call(data);
            parent.getChildren().add((TreeItem)newNode);
            nodes.put(data, newNode);
        });

        items.addListener((ListChangeListener<? super E>) c -> {
            boolean needToSort = false;
            while(c.next()) {
                if(c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        E data = c.getList().get(i);
                        TreeItem<E> node = nodes.get(data);
                        TreeItem<? extends E> newParent = rootNode.getOrCreatePath(pathResolver.call(data));
                        if(!newParent.equals(node.getParent())) {
                            TreeNode.detach(node);
                            newParent.getChildren().add((TreeItem) node);
                        }
                    }
                    needToSort = true;
                }
                for (E data : c.getRemoved()) {
                    TreeNode.detach(nodes.remove(data));
                }
                for (E data : c.getAddedSubList()) {
                    TreeItem<? extends E> parent = rootNode.getOrCreatePath(pathResolver.call(data));
                    TreeItem<E> newNode = nodeFactory.call(data);
                    parent.getChildren().add((TreeItem)newNode);
                    nodes.put(data, newNode);
                    needToSort = true;
                }
            }
            if(needToSort && !treeTableView.getSortOrder().isEmpty()) {
                treeTableView.sort();
            }
        });
    }
}
