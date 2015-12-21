/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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
package gov.nasa.jpl.magicdraw.enhanced.ui.browser

import java.awt.event.MouseEvent

import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator
import com.nomagic.magicdraw.actions.BrowserToolbarAMConfigurator
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.ui.browser.Tree

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