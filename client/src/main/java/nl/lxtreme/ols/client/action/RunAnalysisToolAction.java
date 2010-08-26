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
package nl.lxtreme.ols.client.action;


import java.awt.*;
import java.awt.event.*;

import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.client.Host.MainFrame;
import nl.lxtreme.ols.util.swing.*;


/**
 * 
 */
public class RunAnalysisToolAction extends BaseAction
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  public static final String ID = "RunTool";

  // VARIABLES

  private final MainFrame frame;
  private final Tool tool;

  // CONSTRUCTORS

  /**
   * @param aID
   * @param aName
   * @param aDescription
   */
  public RunAnalysisToolAction( final MainFrame aFrame, final Tool aTool )
  {
    super( ID + aTool.getName(), aTool.getName(), "Run " + aTool.getName() );

    this.frame = aFrame;
    this.tool = aTool;
  }

  // METHODS

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed( final ActionEvent aEvent )
  {
    final Window owner = SwingComponentUtils.getOwningWindow( aEvent );

    final AnnotatedData data = this.frame.getAnnotatedData();
    final ToolContext toolContext = null;

    this.tool.process( owner, data, toolContext, this.frame );
  }
}

/* EOF */
