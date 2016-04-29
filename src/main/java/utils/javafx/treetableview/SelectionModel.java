package utils.javafx.treetableview;

import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;
import javafx.collections.*;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
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

    ObservableSet<TreeItem<E>> selected = FXCollections.observableSet();
    int focused = -1;


    int getRow(TreeItem<E> item) {
        return getTreeTableView().getRow(item);
    }

    public SelectionModel(TreeTableView<E> treeTableView) {
        super(treeTableView);
        selected.addListener((SetChangeListener<? super TreeItem<E>>) change -> {
            if (change.wasRemoved()) {
                Iterator<TreeTablePosition<E, ?>> it = selectedCells.iterator();
                int row = getRow(change.getElementRemoved());
                while (it.hasNext()) {
                    TreeTablePosition<E, ?> cell = it.next();
                    if (cell.getRow() == row) {
                        it.remove();
                    }
                }
            }
            if (change.wasAdded()) {
                int row = getRow(change.getElementRemoved());
                treeTableView.getColumns().forEach(eTreeTableColumn -> {
                    selectedCells.add(new TreeTablePosition<>(treeTableView, row, eTreeTableColumn));
                });
            }
            logger.info("Selection: " + selected);
        });

//        treeTableView.setFocusModel(new TreeTableView.TreeTableViewFocusModel<E>(treeTableView){
//
//            TreeItem<E> focused = null;
//
//            @Override
//            protected int getItemCount() {
//                return focused == null ? 0 : 1;
//            }
//
//            private void focus(TreeItem<E> row) {
//                focused = row;
//            }
//
//            @Override
//            public void focus(int row, TreeTableColumn<E, ?> column) {
//                super.focus(row, column);
//                focus(row);
//            }
//
//            @Override
//            public void focus(TreeTablePosition<E, ?> pos) {
//                super.focus(pos);
//                focus(super.getModelItem(pos.getRow()));
//            }
//
//            @Override
//            public boolean isFocused(int row, TreeTableColumn<E, ?> column) {
//                return isFocused(row);
//            }
//
//            @Override
//            public void focus(int index) {
//                super.focus(index);
//                focus(super.getModelItem(index));
//            }
//
//            @Override
//            public void focusAboveCell() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public void focusBelowCell() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public void focusLeftCell() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public void focusRightCell() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public void focusPrevious() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public void focusNext() {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public boolean isFocused(int index) {
//                TreeItem<E> node = super.getModelItem(index);
//                return node != null && focused == node;
//            }
//        });
        treeTableView.getFocusModel().focusedCellProperty().addListener((observable, oldValue, newValue) -> {
            logger.info("Focused cell changed: " + newValue);
        });
    }

    @Override
    public void clearAndSelect(int index) {
        // replace the anchor
        TreeTableCellBehavior.setAnchor(getTreeTableView(), new TreeTablePosition<>(getTreeTableView(), index, (TreeTableColumn<E,?>)null), false);

        selected.clear();
        select(index);
    }

    @Override
    public void select(int index) {
//        super.select(index);
        getTreeTableView().getFocusModel().focus(index);
        logger.info("Selecting " + index);
        selected.add(getTreeTableView().getTreeItem(index));
    }

    @Override
    public void clearSelection(int index) {
        selected.remove(getTreeTableView().getTreeItem(index));
    }

    @Override
    public void clearSelection() {
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectNext() {
        throw new UnsupportedOperationException();
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
        return super.getSelectedIndices();
    }

    @Override
    public ObservableList<TreeItem<E>> getSelectedItems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void select(TreeItem<E> obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectIndices(int row, int... rows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectAll() {
        for (int i = 0; i < getTreeTableView().getExpandedItemCount(); i++) {
            select(i);
        }
    }

    @Override
    public void selectRange(int minRow, TableColumnBase<TreeItem<E>, ?> minColumn, int maxRow, TableColumnBase<TreeItem<E>, ?> maxColumn) {
        logger.info("Range selection from " + minRow + " to " + maxRow);
        for (int i = Math.min(minRow, maxRow); i <= Math.max(minRow, maxRow); i++) {
            select(i);
        }
    }
}
