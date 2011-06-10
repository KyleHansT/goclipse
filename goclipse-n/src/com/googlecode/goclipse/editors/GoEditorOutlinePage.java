package com.googlecode.goclipse.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.googlecode.goclipse.Activator;
import com.googlecode.goclipse.SysUtils;
import com.googlecode.goclipse.go.lang.parser.CodeContext;
import com.googlecode.goclipse.model.Function;
import com.googlecode.goclipse.model.Import;
import com.googlecode.goclipse.model.Node;

// TODO: look into supporting a custom reconciler on the editor, which would notify the outline view on changes


/**
 * 
 * 
 */
public class GoEditorOutlinePage extends ContentOutlinePage {
	private IDocumentProvider documentProvider;
	private GoEditor editor;

	public GoEditorOutlinePage(IDocumentProvider documentProvider, GoEditor editor) {
		this.documentProvider = documentProvider;
		this.editor = editor;
		
		documentProvider.addElementStateListener(new IElementStateListener() {
			@Override
			public void elementMoved(Object originalElement, Object movedElement) {
				
			}
			
			@Override
			public void elementDirtyStateChanged(Object element, boolean isDirty) {
				if (!isDirty) {
					refreshAsync();
				}
			}
			
			@Override
			public void elementDeleted(Object element) {
				
			}
			
			@Override
			public void elementContentReplaced(Object element) {
				
			}
			
			@Override
			public void elementContentAboutToBeReplaced(Object element) {
				
			}
		});
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		
		getTreeViewer().setContentProvider(new OutlinePageContentProvider());
		getTreeViewer().setLabelProvider(new DelegatingStyledCellLabelProvider(new NodeLabelProvider()));
		getTreeViewer().addSelectionChangedListener(this);
		getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged();
			}
		});
		
		refresh();
	}

	private void refreshAsync() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}
	
	private void refresh() {
		try {
			IDocument document = documentProvider.getDocument(editor.getEditorInput());
			
			CodeContext codeContext = CodeContext.getCodeContext(
				editor.getPartName(), document.get(), false);
			
			getTreeViewer().setInput(codeContext);
		} 
		catch (Throwable exception) {
			SysUtils.severe(exception);
			
			getTreeViewer().setInput(null);
		}
	}
	
	protected void handleSelectionChanged() {
		Object sel = ((IStructuredSelection)getTreeViewer().getSelection()).getFirstElement();
		
		if (sel instanceof Node) {
			IDocument document = documentProvider.getDocument(editor.getEditorInput());
			
			Node node = (Node)sel;
			
			int line = node.getLine() - 1;
			
			if (line != -1) {
				try {
					editor.selectAndReveal(document.getLineOffset(line), document.getLineLength(line));
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
			}
		}
	}

	private static Object IMPORT_CONTAINER = "imports";
	
	private static class NodeLabelProvider extends LabelProvider implements IStyledLabelProvider {
		
		public NodeLabelProvider() {
			
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof Node) {
				Node node = (Node)element;
				
				return node.getName();
			} else {
				return element.toString();
			}
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof Node) {
				Node node = (Node)element;
				
				return node.getImage();
			} else if (element == IMPORT_CONTAINER) {
				return Activator.getImage("icons/impc_obj.gif");
			} else {
				return null;
			}
		}

		@Override
		public StyledString getStyledText(Object element) {
			StyledString str = new StyledString();
			
			if (element instanceof Function) {
				Function function = (Function)element;
				
				String[] strs = splitFunctionName(function.getName());
				
				str.append(strs[0]);
				
				if (strs.length > 0) {
					str.append(strs[1], StyledString.DECORATIONS_STYLER);
				}
			} else if (element instanceof Node) {
				Node node = (Node)element;
				
				if (node.getName() != null) {
					str.append(node.getName());
				} else {
					str.append(node.toString());
				}
			} else {
				str.append(element.toString());
			}
			
			return str;
		}
	}
	
	private static class OutlinePageContentProvider implements ITreeContentProvider {
		private Object[] NO_CHILDREN = new Object[0];
		
		private CodeContext codeContext;
		
		public OutlinePageContentProvider() {
			
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			codeContext = (CodeContext)newInput;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (codeContext != null) {
				List<Node> nodes = new ArrayList<Node>();

				nodes.addAll(codeContext.methods);
				nodes.addAll(codeContext.functions);
				
				Collections.sort(nodes, new LineBasedComparator());
				
				List<Object> children = new ArrayList<Object>();
				
				if (codeContext.pkg != null) {
					children.add(codeContext.pkg);
				}
				
				if (codeContext.imports.size() > 0) {
					children.add(IMPORT_CONTAINER);
				}

				children.addAll(nodes);
				
				return children.toArray();
			} else {
				return NO_CHILDREN;
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement == IMPORT_CONTAINER) {
				return codeContext.imports.toArray();
			} else {
				return NO_CHILDREN;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Import) {
				return IMPORT_CONTAINER;
			} else {
				return codeContext;
			}
		}

		@Override
		public void dispose() {
			
		}
	}

	private static String[] splitFunctionName(String name) {
		int index = name.indexOf(')');
		
		if (name.startsWith("(")) {
			index = name.indexOf(')', index + 1);
		}
		
		if (index + 1 < name.length()) {
			return new String[] {
				name.substring(0, index + 1),
				name.substring(index + 1)
			};
		} else {
			return new String[] { name };
		}
	}
	
	private static class LineBasedComparator implements Comparator<Node> {
		@Override
		public int compare(Node node1, Node node2) {
			return node1.getLine() - node2.getLine();
		}
	}
	
}
