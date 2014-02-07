package ru.flexpay.eirc.mb_transformer.web.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
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
import org.odlabs.wiquery.ui.dialog.Dialog;
import ru.flexpay.eirc.mb_transformer.service.FileService;

import javax.ejb.EJB;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class BrowserFilesDialog extends Panel {

    private Dialog dialog;

    @EJB
    private FileService fileService;

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    private File parent;

    private AttributeModifier styleDisable = new AttributeModifier("class", "btnMiddleDisable");
    private AttributeModifier styleEnable = new AttributeModifier("class", "btnMiddle");

    private Item<File> selected = null;

    private Component refreshComponent;
    private IModel<File> selectedModel;

    private WebMarkupContainer container;

    private final List<IColumn<File>> COLUMNS = ImmutableList.<IColumn<File>>of(
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

    public BrowserFilesDialog(String id, Component refreshComponent, IModel<File> selectedModel) {
        super(id);

        this.refreshComponent = refreshComponent;
        this.selectedModel = selectedModel;

        init();
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {

        container.getHeaderResponse().renderCSSReference(new PackageResourceReference(BrowserFilesDialog.class, "browser.css"));
    }

    public File getSelectedFile() {
        return isFile() ? selected.getModelObject() : null;
    }

    private void init() {
        dialog = new Dialog("dialog");
        dialog.setTitle(new ResourceModel("title"));
        dialog.setWidth(500);
        dialog.setHeight(500);
        add(dialog);


        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        dialog.add(container);

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

        final AjaxLink button = new AjaxLink("select") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.close(target);
                if (isFile()) {
                    selectedModel.setObject(selected.getModelObject());
                    target.add(refreshComponent);
                }
            }
        };
        button.setOutputMarkupId(true);
        button.setEnabled(isFile());
        button.add(styleDisable);

        form.add(button);

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

        final DataTable<File> table = new DataTable<File>("datatable", COLUMNS, provider, 1000) {
            private AttributeAppender style = new AttributeAppender("class", new Model<>("selected"));

            @Override
            protected Item<File> newRowItem(String id, int index, final IModel<File> model) {
                final Item<File> rowItem = super.newRowItem(id, index, model);
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
                        updateButtonState(button, target);

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

                        target.add(selectedFile);
                        updateButtonState(button, target);
                    }
                });
                return rowItem;
            }
        };
        table.addTopToolbar(new HeadersToolbar(table, new ISortStateLocator() {
            private SingleSortState state = new SingleSortState();
            @Override
            public ISortState getSortState() {
                return state;
            }
        }));
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
        if (parent == null) {
            parent = new File(fileService.getWorkDir());
        }
        return new ListDataProvider<File>() {
            @Override
            protected List<File> getData() {
                if (parent != null && StringUtils.equals(parent.getPath(), fileService.getWorkDir())) {
                    return Lists.newArrayList(getFiles(parent));
                }
                return parent != null?
                        Lists.asList(new File(parent.getParentFile(), "..."), getFiles(parent)) :
                        Lists.newArrayList(new File("..."));
            }
        };
    }

    private File[] getFiles(File parent) {
        File[] files = parent.listFiles();
        Arrays.sort(files, new Comparator() {
            public int compare(Object o1, Object o2) {

                if (((File) o1).isDirectory() && ((File) o2).isFile()) {
                    return -1;
                } else if (((File) o1).isFile() && ((File) o2).isDirectory()) {
                    return 1;
                }
                return ((File) o1).getName().compareTo(((File) o2).getName());
            }

        });
        return files;
    }

    private boolean isUpControl(File file) {
        return StringUtils.equals(file.getName(), "...");
    }

    public void open(AjaxRequestTarget target) {
        target.add(container);
        dialog.open(target);
    }

    private class IFileColumn extends AbstractColumn<File> {
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
            if (rowModel.getObject().isFile()) {
                cellItem.add(new AttributeAppender("class", new Model<>(" file")));
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
