/*
 * OpenBench LogicSniffer / SUMP project 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.api.tools;


import nl.lxtreme.ols.api.*;


/**
 * Denotes a callback for use in {@link Tool}s.
 */
public interface AnalysisCallback extends ProgressCallback
{
  // METHODS

  /**
   * Called upon abnormal termination of the analysis.
   * 
   * @param aReason
   *          an optional reason why the analysis might be aborted, can be <code>null</code> or empty.
   */
  void analysisAborted( final String aReason );

  /**
   * Called when the analysis is complete.
   * 
   * @param aNewCapturedData
   *          the (optional) new captured data, can be <code>null</code>.
   */
  void analysisComplete( final CapturedData aNewCapturedData );
}

/* EOF */
