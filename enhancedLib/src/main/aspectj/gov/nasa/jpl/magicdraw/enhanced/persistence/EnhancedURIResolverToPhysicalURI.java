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
package gov.nasa.jpl.magicdraw.enhanced.persistence;

import java.io.File;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.eclipse.emf.common.util.URI;

import gov.nasa.jpl.magicdraw.enhanced.migration.LocalModuleMigrationInterceptor;

/**
 * To migrate a MagicDraw project P that uses a local module M1 that has been revised as a new local module M2, 
 * it is necessary to open P with M1 missing as if M1 was not available.
 * 
 * If M1 is actually missing, MagicDraw opens an UI dialog for the user to select the file for M1.
 * If the user cancels this UI dialog, MD will not load M1. 
 * This is the behavior we want to automate without the UI dialog.
 * 
 * Based on inspecting the stack when MD opens the UI dialog for a missing module, 
 * there are 2 methods that provide the information necessary to determine whether to force skipping the loading of M1
 * when we want to migrate the project P to a newer version of M1, i.e., M2
 * 
 * This class enhances the MD internal API to provide an opportunity to intercept loading a local module
 * if that local module should be skipped for migration.
 */
@SuppressWarnings("deprecation")
@Aspect
public class EnhancedURIResolverToPhysicalURI {

	/**
	 * toPhysicalInternal is a method that attempts to convert the URI of a module
	 * to a physical location where it can be loaded from.
	 * 
	 * @param that1 MD's URIResolver
	 * @param uri  the URI of the module to load
	 * @param config1 MD internal
	 * @param filter1 MD internal
	 */
	@Pointcut(
      "within(com.nomagic.magicdraw.persistence.URIResolver) && "+
      "target(com.nomagic.magicdraw.persistence.URIResolver) && "+
      "this(that1) && "+
      "execution(* com.nomagic.magicdraw.persistence.URIResolver.toPhysicalInternal"+
      "(org.eclipse.emf.common.util.URI,"+
      " com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig,"+
      " com.nomagic.magicdraw.persistence.ModuleFilterContainer)) && "+
      "args(uri, config1, filter1)")
	public void callURIResolverToPhysicalInternal(
			com.nomagic.magicdraw.persistence.URIResolver that1,
			URI uri,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config1,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter1) {}
	
	/**
	 * expandURIWithCustomizators is a method where MD may open a UI dialog to prompt the user
	 * for an alternative location of a local module that cannot be found per its original URI
	 * 
	 * @param that2 MD's URIResolver
	 * @param file the file name of the module to load
	 * @param config2 MD internal
	 * @param filter2 MD internal
	 */
	@Pointcut(
      "within(com.nomagic.magicdraw.persistence.URIResolver) && "+
      "target(com.nomagic.magicdraw.persistence.URIResolver) && "+
      "this(that2) && "+
      "call(* com.nomagic.magicdraw.persistence.URIResolver.expandURIWithCustomizators"+
      "(java.io.File, com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig,"+
      " com.nomagic.magicdraw.persistence.ModuleFilterContainer)) && "+
      "args(file, config2, filter2)")
	public void callURIResolverExpandURIWithCustomizators(
			com.nomagic.magicdraw.persistence.URIResolver that2,
			File file,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config2,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter2) {}
	
	/**
	 * Wormhole interceptor:
	 * toPhysicalInternal(that1, uri, ...)
	 * expandURIWithCustomizators(...)
	 * 
	 * Check if uri is a local module file that should be skipped for migration purposes.
	 * If it should be skipped, return a uri that should be a non-existent file location.
	 * 
	 * @param that1
	 * @param uri
	 * @param config1
	 * @param filter1
	 * @param that2
	 * @param file
	 * @param config2
	 * @param filter2
	 */
	@Pointcut(
      "cflow(callURIResolverToPhysicalInternal(that1, uri, config1, filter1)) && "+
      "callURIResolverExpandURIWithCustomizators(that2, file, config2, filter2)")
	public void callURIResolverToPhysicalInternalCallsExpandURIWithCustomizators(
			com.nomagic.magicdraw.persistence.URIResolver that1,
			URI uri,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config1,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter1,
			com.nomagic.magicdraw.persistence.URIResolver that2, File file,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config2,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter2) {}
	
	@Around(
      "callURIResolverToPhysicalInternalCallsExpandURIWithCustomizators"+
      "(that1, uri, config1, filter1, that2, file, config2, filter2)")
	public URI checkExpandURIWithCustomizators(
			ProceedingJoinPoint thisJoinPoint,
			com.nomagic.magicdraw.persistence.URIResolver that1,
			URI uri,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config1,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter1,
			com.nomagic.magicdraw.persistence.URIResolver that2,
			File file,
			com.nomagic.magicdraw.persistence.URIResolver.ResolveConfig config2,
			com.nomagic.magicdraw.persistence.ModuleFilterContainer filter2) throws Throwable {
		if (uri.isFile() && LocalModuleMigrationInterceptor.forceSkipLocalModuleLoad( uri )) {
			File migrateFile = new File( uri.path().replace(uri.lastSegment(), "migrate/" + uri.lastSegment() ) );
			if (migrateFile.exists()) {
					throw new IllegalArgumentException("Migration File: " + migrateFile + " should not exist!");
			} 
			URI migrateURI = URI.createFileURI( migrateFile.getAbsolutePath() );
			return migrateURI;
		}
		
		Object result = thisJoinPoint.proceed(new Object[]{ that2, file, config2, filter2});
		return (URI) result;
	}
	
}