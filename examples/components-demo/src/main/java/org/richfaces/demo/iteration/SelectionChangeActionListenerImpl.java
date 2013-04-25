package org.richfaces.demo.iteration;

import javax.faces.event.AbortProcessingException;

import org.richfaces.ui.iteration.TreeSelectionChangeEvent;
import org.richfaces.ui.iteration.TreeSelectionChangeListener;

/**
 * User: Gleb Galkin Date: 23.02.11
 */
public class SelectionChangeActionListenerImpl implements TreeSelectionChangeListener {
    public void processTreeSelectionChange(TreeSelectionChangeEvent event) throws AbortProcessingException {
        System.out.println("Inside Selection Change Action");
    }
}
