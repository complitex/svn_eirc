package ru.flexpay.eirc.mb_transformer.web.component;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.odlabs.wiquery.ui.dialog.Dialog;
import ru.flexpay.eirc.mb_transformer.service.FileService;

import javax.ejb.EJB;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.Serializable;
import java.util.Enumeration;


/**
 * @author Pavel Sknar
 */
public class SelectFileDialog extends Panel {

    private Dialog dialog;

    @EJB
    private FileService fileService;

    public SelectFileDialog(String id) {
        super(id);

        dialog = new Dialog("dialog");
        dialog.setTitle(new ResourceModel("title"));
        dialog.setWidth(500);
        add(dialog);

        TreeModel treeModel = new DefaultTreeModel(new FolderTreeNode(new File("/")));

        Tree tree = new Tree("files", treeModel) {
            @Override
            protected String renderNode(TreeNode node) {
                return ((FolderTreeNode)node).getFile().getName();
            }
        };
        dialog.add(tree);
    }

    class FolderTreeNode implements TreeNode, Serializable {
        File file;

        FolderTreeNode(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            String child = file.list()[childIndex];
            return new FolderTreeNode(new File(file, child));
        }

        @Override
        public int getChildCount() {
            return isLeaf() ? 0 :file.list().length;
        }

        @Override
        public TreeNode getParent() {
            File parentFile = file.getParentFile();
            return parentFile == null? null : new FolderTreeNode(parentFile);
        }

        @Override
        public int getIndex(TreeNode node) {
            return ArrayUtils.indexOf(file.listFiles(), ((FolderTreeNode)node).getFile());
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return file.isFile();
        }

        @Override
        public Enumeration children() {
            return new Enumeration() {
                private int index = -1;

                public boolean hasMoreElements() {
                    return (index + 1) < getChildCount();
                }

                public Object nextElement() {
                    return getChildAt(++index);
                }
            };
        }
    }

    public void open(AjaxRequestTarget target) {
        dialog.open(target);
    }
}
