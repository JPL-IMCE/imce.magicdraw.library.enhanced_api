/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.magicdraw.advice.ui.browser;

import gov.nasa.jpl.magicdraw.enhanced.ui.browser.EnhancedBrowserContextAMConfigurator;

import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;

/**
 * MD Open API: com.nomagic.magicdraw.ui.browser.BrowserTabTree
 * <p>
 * The MD Open API manual describes how to add actions in the browser context menu using
 * BrowserContextAMConfigurator.configure(ActionsManager manager, com.nomagic.magicdraw.ui.browser.Tree browser)
 * <p>
 * We can retrieve the selection in the browser via:
 * com.nomagic.magicdraw.ui.browser.Tree.getSelectedNode()
 * com.nomagic.magicdraw.ui.browser.Tree.getSelectedNodes()
 * <p>
 * If multiple nodes are selected, Tree.getSelectedNode() remains "stuck" to the first selection
 * even if the user invokes the context menu from a different selected node!
 * <p>
 * For usability sake, it is important to avoid confusing the end user who,
 * after selecting multiple nodes in the browser tree,
 * could invoke the context menu when the mouse is over one of the selected nodes -- call it the trigger node --
 * and legitimately expect the context menu actions to reflect primarily the element corresponding
 * to the trigger node instead of that of other selected nodes.
 * <p>
 * From the MD Open API, it is possible to retrieve the javax.swing.JTree corresponding to MD's browser tree.
 * If we can get the MouseEvent that triggered the browser context menu,
 * then we can easily retrieve the trigger node as described in the Java SE doc:
 * http://docs.oracle.com/javase/7/docs/api/javax/swing/JTree.html
 * <p>
 * Retrieving this MouseEvent is possible thanks to AspectJ's "wormhole" pattern as done in this class.
 *
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
@Aspect
public class EnhanceBrowserContextConfiguratorWithShowPopupMenuAdvice {

  @Pointcut(
      "within(com.nomagic.magicdraw.ui.browser.BrowserTabTree) && "+
      "execution(void com.nomagic.magicdraw.ui.browser.BrowserTabTree.showPopupMenu(java.awt.event.MouseEvent)) && "+
      "args(mouseEvent)")
  public void mdBrowserTabTreeShowPopupMenu(
      MouseEvent mouseEvent) {
  }

  @Pointcut(
      "within(gov.nasa.jpl.magicdraw.enhanced.ui.browser.EnhancedBrowserContextAMConfigurator+) && "+
      "target(gov.nasa.jpl.magicdraw.enhanced.ui.browser.EnhancedBrowserContextAMConfigurator+) && "+
      "this(that) && "+
      "execution(void *.configure(com.nomagic.actions.ActionsManager, com.nomagic.magicdraw.ui.browser.Tree)) && "+
      "args(manager, tree)")
  public void enhancedBrowserContextConfigureMenu(
      EnhancedBrowserContextAMConfigurator that,
      ActionsManager manager,
      Tree tree) {
  }

  @Around(
      "cflow(mdBrowserTabTreeShowPopupMenu(mouseEvent)) && "+
      "enhancedBrowserContextConfigureMenu(that, manager, tree)")
  public void enhanceBrowserContextConfigureMenuWithMouseEvent(
      ProceedingJoinPoint pjp,
      MouseEvent mouseEvent,
      EnhancedBrowserContextAMConfigurator that,
      ActionsManager manager,
      Tree tree) {
    if (mouseEvent == null || manager == null || tree == null)
      return;

    JTree jtree = tree.getTree();
    if (jtree == null)
      return;

    TreePath triggerPath = jtree.getClosestPathForLocation(mouseEvent.getX(), mouseEvent.getY());
    if (triggerPath == null || !jtree.isPathSelected(triggerPath))
      return;

    Object trigger = triggerPath.getLastPathComponent();
    if (!(trigger instanceof Node))
      return;

    Node[] nodes = tree.getSelectedNodes();

    that.configure(manager, tree, mouseEvent, (Node) trigger, java.util.Arrays.asList(nodes));
  }
}