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

package gov.nasa.jpl.magicdraw.enhanced.actions.paths

import javax.swing.Icon
import javax.swing.KeyStroke

import com.nomagic.magicdraw.ui.actions.DrawPathDiagramAction
import com.nomagic.magicdraw.ui.states.State
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction

import scala.deprecated
import scala.Predef.String

/**
  * @BUG : Report the following to NoMagic.
  *
  *      The MD Open API actions for creating paths & shapes are very similar:
  *
  *      com.nomagic.magicdraw.ui.actions.DrawPathDiagramAction
  *      com.nomagic.magicdraw.ui.actions.DrawShapeDiagramAction
  *
  *      Both call the MD Open API:
  *      com.nomagic.magicdraw.ui.diagrams.BaseCustomizableDiagramAction.createState()
  *      : com.nomagic.magicdraw.ui.states.State
  *
  *      Both return a kind of State defined in the MD Internal API:
  *      com.nomagic.magicdraw.ui.states.SymbolDrawState
  *
  *      The difference between the two actions is the way they construct the SymbolDrawState:
  *
  *      DrawPathDiagramAction.createState()
  *      => new SymbolDrawState(
  *         DiagramCanvas, PresentationElement, SymbolDrawManipulator)
  *
  *      DrawShapeDiagramAction.createState()
  *      => new SymbolDrawState(
  *         DiagramCanvas, PresentationElement, DrawShapeDiagramAction.getLargeIcon(), SymbolDrawManipulator)
  *
  *      If DrawShapeDiagramAction.getLargeIcon() is non-null, it will be used as the cursor icon when
  *      drawing the new shape symbol until the user clicks on the diagram where
  *      the new shape symbol & element will be created.
  */

/**
  * @author Nicolas.F.Rouquette@jpl.nasa.gov
  *
  *         Workaround to the limitations to DrawPathDiagramAction using an AspectJ wormhole technique
  *         for injecting the action's icon into the SymbolDrawState constructor when
  *         EnhancedDrawPathAction.createState() is called.
  */
@deprecated("", "")
abstract class EnhancedDrawPathAction
( finalizationAction: AdditionalDrawAction,
  diagram: DiagramPresentationElement,
  ID: String,
  name: String,
  key: KeyStroke,
  largeIcon: Icon)
  extends DrawPathDiagramAction(ID, name, key) {

  setDiagram(diagram)

  override def getLargeIcon: Icon = largeIcon

  /**
    * This will be advised...
    */
  override final def createState(): State = {
    val s = super.createState()
    s
  }
}