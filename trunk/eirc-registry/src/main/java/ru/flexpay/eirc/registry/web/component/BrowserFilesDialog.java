package ru.flexpay.eirc.registry.web.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.digester.RegexMatcher;
import org.apache.commons.digester.SimpleRegexMatcher;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.odlabs.wiquery.core.javascript.JsStatement;
import org.odlabs.wiquery.ui.core.JsScopeUiEvent;
import org.odlabs.wiquery.ui.dialog.Dialog;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class BrowserFilesDialog extends Panel {

    private static final RegexMatcher MATCHER = new SimpleRegexMatcher();

    private Dialog lookupDialog;

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    private File oldParent;
    private File parent;

    private AttributeModifier styleDisable = new AttributeModifier("class", "btnMiddleDisable");
    private AttributeModifier styleEnable = new AttributeModifier("class", "btnMiddle");

    private Item<File> oldSelected = null;
    private Item<File> selected = null;

    private Component refreshComponent;
    private IModel<File> selectedModel;
    private String workDir;

    private WebMarkupContainer container;

    private SingleSortState<String> oldState = new SingleSortState<>();
    private SingleSortState<String> state = new SingleSortState<>();

    private String oldSortProperty = "name";
    private String sortProperty = "name";

    private Model<String> oldFileNameModel = new Model<>("*");
    private Model<String> fileNameModel = new Model<>("*");

    private final List<IColumn<File, String>> COLUMNS = ImmutableList.<IColumn<File, String>>of(
            new IFileColumn(new ResourceModel("name"), new IFileModel() {
                @Override
                public String getObject() {
                    String name = getFile().getName();
                    return getFile().isDirectory() ? name + "/" : name;
                }
            }, "name", "nameColumn"),
            new IFileColumn(new ResourceModel("size"), new IFileModel() {
                @Override
                public String getObject() {
                    return isUpControl(getFile()) ? "" : String.valueOf(getFile().length());
                }
            }, "size", "sizeColumn"),
            new IFileColumn(new ResourceModel("date"), new IFileModel() {
                @Override
                public String getObject() {
                    return isUpControl(getFile()) ? "" : DATE_FORMAT.format(getFile().lastModified());
                }
            }, "date", "dateColumn")
    );

    public BrowserFilesDialog(String id, Component refreshComponent, IModel<File> selectedModel, String workDir) {
        super(id);

        this.workDir = workDir;
        this.refreshComponent = refreshComponent;
        this.selectedModel = selectedModel;

        init();
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {

        container.getHeaderResponse().render(CssHeaderItem.forReference(
                new PackageResourceReference(BrowserFilesDialog.class, "browser.css")));
    }

    public File getSelectedFile() {
        return isFile() ? selectedModel.getObject() : null;
    }

    private void init() {

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
        // DataTable //
        IDataProvider<File> provider = newDataProvider();
        /*
        //Since wicket 6.9
        Options options = new Options();
        options.set("height", 430);
        options.set("scrollable", "{ virtual: true }"); //infinite scroll
        */
        final AjaxLink selectButton = new AjaxLink("select") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                lookupDialog.close(target);
                if (isFile()) {
                    selectedModel.setObject(selected.getModelObject());
                    target.add(refreshComponent);
                    onSelected(target);
                }
            }
        };
        selectButton.setOutputMarkupId(true);
        selectButton.setEnabled(isFile());
        selectButton.add(styleDisable);

        form.add(selectButton);

        final AjaxLink cancelButton = new AjaxLink("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                lookupDialog.close(target);

                fileNameModel.setObject(oldFileNameModel.getObject());
                parent = oldParent;
                selected = oldSelected;
                sortProperty = oldSortProperty;

                state.setSort(oldState.getSort());
                state.setPropertySortOrder(sortProperty, oldState.getPropertySortOrder(sortProperty));
            }
        };
        form.add(cancelButton);

        TextField<String> fileNameFilter = new TextField<>("fileNameFilter", fileNameModel);
        form.add(fileNameFilter);

        AjaxButton filterButton = new AjaxButton("filterButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (StringUtils.isBlank(fileNameModel.getObject())) {
                    fileNameModel.setObject("*");
                }
                target.add(container);
            }
        };
        form.add(filterButton);

        final TextField<String> selectedFile = new TextField<>("selected", new IModel<String>() {
            @Override
            public String getObject() {
                return selected != null && selected.getModelObject() != null && isFile()? selected.getModelObject().getName() : "";
            }

            @Override
            public void setObject(String object) {

            }

            @Override
            public void detach() {

            }
        });
        selectedFile.setEnabled(false);
        selectedFile.setOutputMarkupId(true);
        form.add(selectedFile);

        final DataTable<File, String> table = new DataTable<File, String>("datatable", COLUMNS, provider, 1000) {
            private AttributeAppender style = new AttributeAppender("class", new Model<>("selected"));

            @Override
            protected Item<File> newRowItem(String id, int index, final IModel<File> model) {
                final Item<File> rowItem = super.newRowItem(id, index, model);

                if (selected != null && !selected.equals(rowItem) &&
                        StringUtils.equals(rowItem.getModelObject().getPath(), selected.getModelObject().getPath())) {
                    rowItem.add(style);
                    selected = rowItem;
                }

                rowItem.add(new AjaxEventBehavior("ondblclick") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        if (isUpControl(model.getObject())) {
                            parent = model.getObject().getParentFile();
                        } else if (model.getObject().isFile()) {
                            return;
                        } else {
                            parent = model.getObject();
                        }
                        selected = null;
                        updateButtonState(selectButton, target);

                        target.add(container);
                    }
                });
                rowItem.add(new AjaxEventBehavior("onclick") {
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        if (selected != null && selected.equals(rowItem)) {
                            return;
                        }
                        if (selected != null) {
                            selected.remove(style);
                            target.add(selected);
                        }
                        selected = rowItem;
                        rowItem.add(style);

                        target.add(selected);
                        target.add(selectedFile);
                        updateButtonState(selectButton, target);
                    }
                });
                return rowItem;
            }
        };
        table.addTopToolbar(new HeadersToolbar<String>(table, new ISortStateLocator<String>() {
            @Override
            public ISortState<String> getSortState() {
                return state;
            }
        }) {
            @Override
            protected WebMarkupContainer newSortableHeader(final String headerId, final String property,
                                                           final ISortStateLocator<String> locator)
            {
                return new AjaxFallbackOrderByBorder<String>(headerId, property, locator) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onAjaxClick(AjaxRequestTarget target) {
                        sortProperty = property;
                        target.add(container);
                    }
                };
            }
        });
        form.add(table);
    }

    public boolean isFile() {
        return selected != null && !isUpControl(selected.getModelObject()) && selected.getModelObject().isFile();
    }

    private void updateButtonState(AjaxLink button, AjaxRequestTarget target) {
        button.setEnabled(isFile());
        if (isFile()) {
            button.add(styleEnable);
        } else {
            button.add(styleDisable);
        }
        if (target != null) {
            target.add(button);
        }
    }

    private IDataProvider<File> newDataProvider() {
        String workDir = getWorkDir();
        if (workDir == null) {
            return new ListDataProvider<>(Collections.<File>emptyList());
        }
        if (parent == null) {
            parent = new File(workDir);
        }
        return new ListDataProvider<File>() {
            @Override
            protected List<File> getData() {
                String workDir = getWorkDir();
                if (workDir == null) {
                    return Collections.emptyList();
                }

                try {
                    if (parent != null &&
                            Files.isSameFile(FileSystems.getDefault().getPath(parent.getPath()),
                                    FileSystems.getDefault().getPath(workDir))) {
                        return Lists.newArrayList(getFiles(parent));
                    }
                } catch (IOException e) {
                    parent = null;
                    container.error(getString("failed_system_path"));
                }

                return parent != null?
                        Lists.asList(new File(parent.getParentFile(), "..."), getFiles(parent)) :
                        Collections.<File>emptyList();
            }
        };
    }
    
    private String getWorkDir() {
        if (workDir == null || !Files.isDirectory(FileSystems.getDefault().getPath(workDir))) {
            parent = null;
            container.error(getString("failed_work_dir"));
            return null;
        }
        return workDir;
    }

    private File[] getFiles(File parent) {

        File[] files = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                return MATCHER.match(pathname.getName(), fileNameModel.getObject());
            }
        });
        final SortOrder order = state.getPropertySortOrder(sortProperty);
        switch (sortProperty) {
            case "name":
                Arrays.sort(files, new Comparator() {
                    public int compare(Object o1, Object o2) {

                        if (((File) o1).isDirectory() && ((File) o2).isFile()) {
                            return -1;
                        } else if (((File) o1).isFile() && ((File) o2).isDirectory()) {
                            return 1;
                        }
                        int comp = ((File) o1).getName().compareTo(((File) o2).getName());
                        return SortOrder.ASCENDING.equals(order) ? comp : -1*comp;
                    }

                });
                break;
            case "size":
                Arrays.sort(files, new Comparator() {
                    public int compare(Object o1, Object o2) {

                        if (((File) o1).isDirectory() && ((File) o2).isFile()) {
                            return -1;
                        } else if (((File) o1).isFile() && ((File) o2).isDirectory()) {
                            return 1;
                        }
                        int comp = Long.compare(((File) o1).length(), ((File) o2).length());
                        return SortOrder.ASCENDING.equals(order) ? comp : -1*comp;
                    }

                });
                break;
            case "date":
                Arrays.sort(files, new Comparator() {
                    public int compare(Object o1, Object o2) {

                        if (((File) o1).isDirectory() && ((File) o2).isFile()) {
                            return -1;
                        } else if (((File) o1).isFile() && ((File) o2).isDirectory()) {
                            return 1;
                        }
                        int comp = Long.compare(((File) o1).lastModified(), ((File) o2).lastModified());
                        return SortOrder.ASCENDING.equals(order) ? comp : -1*comp;
                    }

                });
                break;
        }
        return files;
    }

    private boolean isUpControl(File file) {
        return StringUtils.equals(file.getName(), "...");
    }

    public void open(AjaxRequestTarget target) {
        oldFileNameModel.setObject(fileNameModel.getObject());
        oldParent = parent;
        oldSelected = selected;
        oldSortProperty = sortProperty;

        oldState.setSort(state.getSort());
        oldState.setPropertySortOrder(oldSortProperty, state.getPropertySortOrder(oldSortProperty));

        target.add(container);
        lookupDialog.open(target);
    }

    public void onSelected(AjaxRequestTarget target) {

    }

    private class IFileColumn extends AbstractColumn<File, String> {
        private IFileModel cellModel;
        private String cssClass;

        public IFileColumn(IModel<String> displayModel, IFileModel cellModel,  String sortProperty, String cssClass) {
            super(displayModel, sortProperty);
            this.cellModel = cellModel;
            this.cssClass = cssClass;
        }

        @Override
        public void populateItem(final Item<ICellPopulator<File>> cellItem, String componentId, final IModel<File> rowModel) {
            cellModel.setFile(rowModel.getObject());
            cellItem.add(new Label(componentId, cellModel.getObject()));
            if (!rowModel.getObject().isFile()) {
                cellItem.add(new AttributeAppender("class", new Model<>(" folder")));
            }
        }

        @Override
        public String getCssClass() {
            return cssClass;
        }
    }

    private abstract class IFileModel implements IModel<String> {

        private File file;

        public void setFile(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        @Override
        public void setObject(String object) {

        }

        @Override
        public void detach() {

        }
    }
}
