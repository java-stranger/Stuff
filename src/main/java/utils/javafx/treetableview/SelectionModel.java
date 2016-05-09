package utils.javafx.treetableview;

import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * An alternative {@code TreeTableViewSelectionModel} for {@code TreeTableView}
 * Created by denis.kubasov on 29/04/2016.
 */
public class SelectionModel<E> extends TreeTableView.TreeTableViewSelectionModel<E> {

    private static final Logger logger = LoggerFactory.getLogger(SelectionModel.class.getName());
    /**
     * Builds a default TreeTableViewSelectionModel instance with the provided
     * TreeTableView.
     *
     * @param treeTableView The TreeTableView upon which this selection model should
     * operate.
     * @throws NullPointerException TreeTableView can not be null.
     */

    ObservableList<TreeTablePosition<E, ?>> selectedCells = FXCollections.observableArrayList();
    ObservableList<Integer> selectedIndices = FXCollections.observableArrayList();

    ObservableList<TreeItem<E>> selected = FXCollections.observableArrayList();

    int getRow(TreeItem<E> item) {
        return getTreeTableView().getRow(item);
    }

    public SelectionModel(TreeTableView<E> treeTableView) {
        super(treeTableView);
        selected.addListener((ListChangeListener<? super TreeItem<E>>) change -> {
            while(change.next()) {
                change.getRemoved().forEach(o -> {
                    Iterator<TreeTablePosition<E, ?>> it = selectedCells.iterator();
                    int row = getRow(o);
                    while (it.hasNext()) {
                        TreeTablePosition<E, ?> cell = it.next();
                        if (cell.getRow() == row) {
                            it.remove();
                        }
                    }
                    selectedIndices.removeAll(row);
                });
                change.getAddedSubList().forEach(o -> {
                    int row = getRow(o);
                    treeTableView.getColumns().forEach(eTreeTableColumn -> {
                        selectedCells.add(new TreeTablePosition<>(treeTableView, row, eTreeTableColumn));
                    });
                    selectedIndices.addAll(row);
                });
            }
//            logger.info("Selection: " + selected);
        });
    }


    private int getRowCount() {
        return getTreeTableView().getExpandedItemCount();
    }

    @Override
    public void clearAndSelect(int index) {
        // replace the anchor
        TreeTableCellBehavior.setAnchor(getTreeTableView(), new TreeTablePosition<>(getTreeTableView(), index, (TreeTableColumn<E, ?>) null), false);
        getTreeTableView().getFocusModel().focus(index);

        selected.clear();
        select(index);
    }

    @Override
    public void select(int index) {
        TreeItem<E> node = getTreeTableView().getTreeItem(index);
        if(!selected.contains(node)) {
            selected.add(node);
        }
    }

    @Override
    public void clearSelection(int index) {
        selected.remove(getTreeTableView().getTreeItem(index));
    }

    @Override
    public void clearSelection() {
        getTreeTableView().getFocusModel().focus(-1);
        selected.clear();
    }

    @Override
    public boolean isSelected(int index) {
        return selected.contains(getTreeTableView().getTreeItem(index));
    }

    @Override
    public boolean isEmpty() {
        return selected.isEmpty();
    }

    @Override
    public void selectPrevious() {
        int focusIndex = getFocusedIndex();
        if (focusIndex == -1) {
            select(getRowCount() - 1);
        } else if (focusIndex > 0) {
            select(focusIndex - 1);
        }
    }

    @Override
    public void selectNext() {
        int focusIndex = getFocusedIndex();
        if (focusIndex == -1) {
            select(0);
        } else if (focusIndex < getRowCount() - 1) {
            select(focusIndex + 1);
        }
    }

    @Override
    public void selectFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObservableList<TreeTablePosition<E, ?>> getSelectedCells() {
        return selectedCells;
    }

    @Override
    public boolean isSelected(int row, TableColumnBase<TreeItem<E>, ?> column) {
        return isSelected(row);
    }

    @Override
    public void select(int row, TableColumnBase<TreeItem<E>, ?> column) {
        select(row);
    }

    @Override
    public void clearAndSelect(int row, TableColumnBase<TreeItem<E>, ?> column) {
        clearAndSelect(row);
    }

    @Override
    public void clearSelection(int row, TableColumnBase<TreeItem<E>, ?> column) {
        clearSelection(row);
    }

    public void clearSelection(TreeItem<E>... items) {
        selected.removeAll(items);
    }

    public void clearSelection(Collection<TreeItem<E>> items) {
        selected.removeAll(items);
    }

    @Override
    public void selectLeftCell() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectRightCell() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectAboveCell() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectBelowCell() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TreeItem<E> getModelItem(int index) {
        return super.getModelItem(index);
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    @Override
    public ObservableList<TreeItem<E>> getSelectedItems() {
        return selected;
    }

    @Override
    public void select(TreeItem<E> obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectIndices(int row, int... rows) {
        select(row);
        getTreeTableView().getFocusModel().focus(row);
        for (int i : rows) {
            select(i);
            getTreeTableView().getFocusModel().focus(i);
        }
    }

    @Override
    public void selectAll() {
        for (int i = 0; i < getTreeTableView().getExpandedItemCount(); i++) {
            select(i);
        }
    }

    @Override
    public void selectRange(int minRow, TableColumnBase<TreeItem<E>, ?> minColumn, int maxRow, TableColumnBase<TreeItem<E>, ?> maxColumn) {
        for (int i = Math.min(minRow, maxRow); i <= Math.max(minRow, maxRow); i++) {
            select(i);
        }
        getTreeTableView().getFocusModel().focus(maxRow);
    }
}
