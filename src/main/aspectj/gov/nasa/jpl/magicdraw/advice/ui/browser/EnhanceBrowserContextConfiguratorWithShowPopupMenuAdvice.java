/*
 * Copyright 2015 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
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