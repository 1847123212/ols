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
package nl.lxtreme.ols.tool.measure;


import java.awt.*;

import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.tool.base.*;


/**
 * @author jawi
 */
public class MeasurementTool extends BaseTool<MeasurementDialog>
{
  // CONSTRUCTORS

  /**
   * Creates a new MeasurementTool instance.
   */
  public MeasurementTool()
  {
    super( "Measure ..." );
  }

  // METHODS

  /**
   * @see nl.lxtreme.ols.tool.base.BaseTool#createDialog(java.awt.Window,
   *      java.lang.String, nl.lxtreme.ols.api.data.AnnotatedData,
   *      nl.lxtreme.ols.api.tools.ToolContext,
   *      nl.lxtreme.ols.api.tools.AnalysisCallback)
   */
  @Override
  protected MeasurementDialog createDialog( final Window aOwner, final String aName, final AnnotatedData aData,
      final ToolContext aContext, final AnalysisCallback aCallback )
  {
    return new MeasurementDialog( aOwner, aName );
  }
}
