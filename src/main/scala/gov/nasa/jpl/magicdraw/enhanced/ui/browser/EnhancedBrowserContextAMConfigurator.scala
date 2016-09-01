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

package gov.nasa.jpl.magicdraw.enhanced.ui.browser

import java.awt.event.MouseEvent

import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator
import com.nomagic.magicdraw.actions.BrowserToolbarAMConfigurator
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.ui.browser.Tree

import scala.Unit

/**
  * @author Nicolas.F.Rouquette@jpl.nasa.gov
  */
abstract class EnhancedBrowserContextAMConfigurator
  extends BrowserContextAMConfigurator
    with BrowserToolbarAMConfigurator {

  /**
    * - implements com.nomagic.magicdraw.actions.BrowserToolbarAMConfigurator.configure
    * - implements com.nomagic.magicdraw.actions.BrowserContextAMConfigurator.configure
    *
    * Calls to the implemented configure() method above will match the AspectJ pointcut below:
    *
    * @Pointcut ("within(gov.nasa.jpl.magicdraw.ui.browser.EnhancedBrowserContextAMConfigurator+)
    *           && target(gov.nasa.jpl.magicdraw.ui.browser.EnhancedBrowserContextAMConfigurator+)
    *           && this(that)
    *           && execution(
    *               void gov.nasa.jpl.magicdraw.ui.browser.EnhancedBrowserContextAMConfigurator.configure(
    *                      com.nomagic.actions.ActionsManager, com.nomagic.magicdraw.ui.browser.Tree))
    *           && args(manager, tree)")
    *
    *           public void enhancedBrowserContextConfigureMenu(
    *                       EnhancedBrowserContextAMConfigurator that, ActionsManager manager, Tree tree) {}
    *
    *           This pointcut is the "callee" 1/2 of the AspectJ wormhole pattern with which
    *           we will gain access to the MouseEvent that triggered the context menu.
    *
    *           Thread [AWT-EventQueue-0] (Suspended (breakpoint at line 72 in DynamicScriptsBrowserConfigurator))
    *           DynamicScriptsBrowserConfigurator.configure(ActionsManager, Tree) line: 72
    *           ActionsConfiguratorsManager.configureBrowserContextAM(String, ActionsManager, Tree) line: 1595
    *           ActionsConfiguratorsManager.configureContainmentBrowserContextAM(ActionsManager, Tree) line: 833
    *           ContainmentTree.configureContextActions(ActionsManager) line: 209
    *           ContainmentTree(BrowserTabTree).showPopupMenu(MouseEvent) line: 481
    */
  final def configure
  (manager: ActionsManager,
   tree: Tree)
  : Unit = {}

  /**
    * This method will be invoked after a call to the final method above via the AspectJ wormhole pattern:
    *
    * @After ("cflow(mdBrowserTabTreeShowPopupMenu(mouseEvent)) &&
    *          enhancedBrowserContextConfigureMenu(that, manager, tree)")
    *        public void enhanceBrowserContextConfigureMenuWithMouseEvent(
    *           MouseEvent mouseEvent,
    *           EnhancedBrowserContextAMConfigurator that,
    *           ActionsManager manager,
    *           Tree tree) {
    *        that.configure(manager, tree, mouseEvent);
    *        }
    */
  def configure
  (manager: ActionsManager,
   tree: Tree,
   mouseEvent: MouseEvent,
   trigger: Node,
   selection: java.util.Collection[Node])
  : Unit

}