package ru.flexpay.eirc.registry.web.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.complitex.dictionary.entity.Preference;
import org.complitex.dictionary.web.DictionaryFwSession;
import org.odlabs.wiquery.core.javascript.JsStatement;
import org.odlabs.wiquery.ui.core.JsScopeUiEvent;
import org.odlabs.wiquery.ui.dialog.Dialog;
import ru.flexpay.eirc.service_provider_account.web.component.AjaxCheckBoxPanel;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
public class ColumnsPropertiesDialog extends Panel {

    private static final String SKIP_COLUMN_POSTFIX = "_skip";

    private DataTable<?, String> table;

    private Dialog lookupDialog;
    private WebMarkupContainer container;

    private List<ColumnView> columnViews = Lists.newArrayList();

    public ColumnsPropertiesDialog(String id, DataTable<?, String> table) {
        super(id);

        this.table = table;

        init();
    }

    private void init() {

        initColumnViews();

        lookupDialog = new Dialog("lookupDialog") {

            {
                getOptions().putLiteral("width", "auto");
            }
        };
        lookupDialog.setModal(true);
        lookupDialog.setOpenEvent(JsScopeUiEvent.quickScope(new JsStatement().self().chain("parents", "'.ui-dialog:first'").
                chain("find", "'.ui-dialog-titlebar-close'").
                chain("hide").render()));
        lookupDialog.setCloseOnEscape(false);
        add(lookupDialog);


        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        lookupDialog.add(container);

        final Form form = new Form("form");
        container.add(form);

        final List<IColumn<ColumnView, String>> columns = ImmutableList.<IColumn<ColumnView, String>>of(
                new AbstractColumn<ColumnView, String>(new Model<String>(), "show") {
                    @Override
                    public void populateItem(Item<ICellPopulator<ColumnView>> cellItem, String componentId, IModel<ColumnView> rowModel) {
                        if (rowModel.getObject().isSkip()) {
                            cellItem.setVisible(false);
                            return;
                        }
                        AjaxCheckBoxPanel ajaxCheckBoxPanel = new AjaxCheckBoxPanel(componentId, new PropertyModel<Boolean>(rowModel, getSortProperty())) {
                            @Override
                            public void onUpdate(AjaxRequestTarget target) {
                            }
                        };
                        cellItem.add(ajaxCheckBoxPanel);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }
                },
                new AbstractColumn<ColumnView, String>(new Model<String>()) {
                    @Override
                    public void populateItem(Item<ICellPopulator<ColumnView>> cellItem, String componentId, IModel<ColumnView> rowModel) {
                        if (rowModel.getObject().isSkip()) {
                            cellItem.setVisible(false);
                            return;
                        }
                        Label label = new Label(componentId, rowModel.getObject().getColumnName());
                        cellItem.add(label);
                    }
                }
        );

        final IDataProvider<ColumnView> dataProvider = getInstanceDataProvider();

        updateColumnsState();

        final DataTable<ColumnView, String> table = new DataTable<>("datatable", columns, dataProvider, 1000);
        form.add(table);

        final AjaxLink submitButton = new AjaxLink("submit") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                updateColumnsState();
                target.add(ColumnsPropertiesDialog.this.table);
                lookupDialog.close(target);
            }
        };
        submitButton.setOutputMarkupId(true);

        form.add(submitButton);

        final AjaxLink cancelButton = new AjaxLink("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {

                lookupDialog.close(target);
            }
        };
        form.add(cancelButton);
    }

    @SuppressWarnings("unchecked")
    public void updateColumnsState() {
        Map<String, Preference> preferences = this.getSession().getPreferenceMap(getPreferenceKey());
        List<AbstractColumn<?, String>> iColumns = (List<AbstractColumn<?, String>>) this.table.getColumns();
        iColumns.clear();
        for (ColumnView columnView : columnViews) {
            if (columnView.isShow()) {
                iColumns.add(columnView.getColumn());
                if (preferences.containsKey(columnView.getColumnSortProperty())) {
                    this.getSession().putPreference(getPreferenceKey(), columnView.getColumnSortProperty(), null, true);
                }
            } else {
                iColumns.remove(columnView.getColumn());
                this.getSession().putPreference(getPreferenceKey(), columnView.getColumnSortProperty(), Boolean.FALSE.toString(), true);
            }
        }
    }

    public String getPreferenceKey() {
        return table.getPage().getPageClass().toString();
    }

    public void open(AjaxRequestTarget target) {
        target.add(container);
        lookupDialog.open(target);
    }

    public IDataProvider<ColumnView> getInstanceDataProvider() {
        return new ListDataProvider<ColumnView>() {
            @Override
            protected List<ColumnView> getData() {
                List<ColumnView> data = Lists.newArrayList();
                for (ColumnView columnView : columnViews) {
                    if (!columnView.isSkip()) {
                        data.add(columnView);
                    }
                }
                return data;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public void initColumnViews() {
        Map<String, Preference> preferences = getSession().getPreferenceMap(getPreferenceKey());
        columnViews.clear();
        for (IColumn<?, String> column : table.getColumns()) {
            boolean show = !preferences.containsKey(column.getSortProperty());
            columnViews.add(new ColumnView((AbstractColumn<?, String>) column, show));
        }
    }

    @Override
    public DictionaryFwSession getSession() {
        return (DictionaryFwSession) super.getSession();
    }

    private class ColumnView implements Serializable {
        private AbstractColumn<?, String> column;
        private boolean show;

        private ColumnView(AbstractColumn<?, String> column, boolean show) {
            this.column = column;
            this.show = show;
        }

        public AbstractColumn<?, String> getColumn() {
            return column;
        }

        public boolean isShow() {
            return show || isSkip();
        }

        public boolean isSkip() {
            return StringUtils.endsWith(column.getSortProperty(), SKIP_COLUMN_POSTFIX);
        }

        public void setShow(boolean show) {
            this.show = show;
        }

        public String getColumnName() {
            return column.getDisplayModel().getObject();
        }

        public String getColumnSortProperty() {
            return column.getSortProperty();
        }
    }
}
