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

package gov.nasa.jpl.magicdraw.advice.actions.paths;

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
	public void enhancedDrawPathActionCreateState(gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction that) {}

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
			gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction that,
			com.nomagic.magicdraw.ui.DiagramCanvas dc,
			PresentationElement pe,
			com.nomagic.magicdraw.uml.symbols.manipulators.SymbolDrawManipulator sdm) throws Throwable {
		Icon icon = that.getLargeIcon();
		com.nomagic.magicdraw.ui.states.SymbolDrawState s =
				new com.nomagic.magicdraw.ui.states.SymbolDrawState(dc, pe, icon, sdm);
		return s;
	}
}