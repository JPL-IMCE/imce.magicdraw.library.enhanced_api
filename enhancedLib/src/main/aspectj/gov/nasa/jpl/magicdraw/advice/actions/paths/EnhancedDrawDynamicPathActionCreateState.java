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
package gov.nasa.jpl.magicdraw.advice.actions.paths;

import gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction;

import javax.swing.Icon;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.nomagic.magicdraw.uml.symbols.PresentationElement;

@SuppressWarnings("deprecation")
@Aspect
public class EnhancedDrawDynamicPathActionCreateState {

  @Pointcut(
      "within(gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction) && "+
      "target(gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction) && "+
      "this(that) && execution(com.nomagic.magicdraw.ui.states.State *.createState())")
	public void enhancedDrawPathActionCreateState(EnhancedDrawPathAction that) {}

	@Pointcut(
      "call(com.nomagic.magicdraw.ui.states.SymbolDrawState.new"+
      "(com.nomagic.magicdraw.ui.DiagramCanvas,"+
      " com.nomagic.magicdraw.uml.symbols.PresentationElement,"+
      " com.nomagic.magicdraw.uml.symbols.manipulators.SymbolDrawManipulator)) && "+
      "args(dc, pe, sdm)")
	public void createSymbolDrawStateWithoutIcon(
      com.nomagic.magicdraw.ui.DiagramCanvas dc,
			PresentationElement pe,
			com.nomagic.magicdraw.uml.symbols.manipulators.SymbolDrawManipulator sdm) {}
	
	@Around(
      "cflow(enhancedDrawPathActionCreateState(that)) && "+
      "createSymbolDrawStateWithoutIcon(dc, pe, sdm)")
	public com.nomagic.magicdraw.ui.states.SymbolDrawState createSymbolDrawStateWithIcon(
			ProceedingJoinPoint pjp,
			EnhancedDrawPathAction that,
			com.nomagic.magicdraw.ui.DiagramCanvas dc,
			PresentationElement pe,
			com.nomagic.magicdraw.uml.symbols.manipulators.SymbolDrawManipulator sdm) throws Throwable {
		Icon icon = that.getLargeIcon();
		com.nomagic.magicdraw.ui.states.SymbolDrawState s =
				new com.nomagic.magicdraw.ui.states.SymbolDrawState(dc, pe, icon, sdm);
		return s;
	}
}