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