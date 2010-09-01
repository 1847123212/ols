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
package nl.lxtreme.ols.tool.base;


import java.beans.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.SwingWorker.*;

import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.tools.*;


/**
 * Provides a base class for tools that want to do its processing in the
 * background, or outside the EDT.
 */
public abstract class BaseAsyncTool<DIALOG extends JDialog & ToolDialog & AsyncToolDialog<RESULT_TYPE, WORKER>, RESULT_TYPE, WORKER extends BaseAsyncToolWorker<RESULT_TYPE>>
    extends BaseTool<DIALOG>
{
  // INNER TYPES

  /**
   * Provides an interface to create a tool worker on the fly.
   * 
   * @param <RESULT_TYPE>
   *          the result type of the tool worker;
   * @param <WORKER>
   *          the actual type of the tool worker.
   */
  static interface ToolWorkerFactory<RESULT_TYPE, WORKER extends BaseAsyncToolWorker<RESULT_TYPE>>
  {
    /**
     * Factory method for creating a tool worker.
     */
    WORKER createToolWorker();
  }

  /**
   * Provides a default implementation of a tool worker factory, using the
   * defined {@link BaseAsyncTool#createToolWorker(DataContainer)} method for
   * the actual creation of the tool worker itself.
   */
  final class ToolWorkerFactoryImpl implements ToolWorkerFactory<RESULT_TYPE, WORKER>
  {
    private final AnalysisCallback callback;
    private final DataContainer data;

    /**
     * 
     */
    public ToolWorkerFactoryImpl( final DataContainer aData, final AnalysisCallback aCallback )
    {
      this.data = aData;
      this.callback = aCallback;
    }

    /**
     * @see nl.lxtreme.ols.tool.base.BaseAsyncTool.ToolWorkerFactory#createToolWorker()
     */
    @Override
    public WORKER createToolWorker()
    {
      final WORKER toolWorker = BaseAsyncTool.this.createToolWorker( this.data );
      toolWorker.addPropertyChangeListener( new ToolWorkerPropertyChangeListener( toolWorker, this.callback ) );
      return toolWorker;
    }
  }

  /**
   * Provides a tool worker property change listener that reports back the state
   * of the tool worker to this class and the analysis callback.
   */
  final class ToolWorkerPropertyChangeListener implements PropertyChangeListener
  {
    // VARIABLES

    private final WORKER toolWorker;
    private final AnalysisCallback callback;

    // CONSTRUCTORS

    /**
     * @param aCallback
     */
    public ToolWorkerPropertyChangeListener( final WORKER aToolWorker, final AnalysisCallback aCallback )
    {
      this.toolWorker = aToolWorker;
      this.callback = aCallback;
    }

    // METHODS

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange( final PropertyChangeEvent aEvent )
    {
      final String name = aEvent.getPropertyName();
      final Object value = aEvent.getNewValue();

      if ( PROPERTY_PROGRESS.equals( name ) )
      {
        // Progress update...
        final Integer percentage = ( Integer )value;
        this.callback.updateProgress( percentage );
      }
      else if ( PROPERTY_STATE.equals( name ) )
      {
        // State change...
        final StateValue state = ( StateValue )value;
        if ( StateValue.DONE.equals( state ) )
        {
          RESULT_TYPE analysisResults = null;
          String abortReason = null;

          try
          {
            analysisResults = this.toolWorker.get();
          }
          catch ( CancellationException exception )
          {
            abortReason = "Cancelled by user.";
          }
          catch ( ExecutionException exception )
          {
            abortReason = exception.getCause().getMessage();
          }
          catch ( InterruptedException exception )
          {
            abortReason = exception.getMessage();
          }

          if ( this.toolWorker.isCancelled() || ( abortReason != null ) )
          {
            if ( abortReason == null )
            {
              abortReason = "";
            }
            this.callback.analysisAborted( abortReason );
          }
          else
          {
            CapturedData newData = null;
            if ( analysisResults instanceof CapturedData )
            {
              newData = ( CapturedData )analysisResults;
            }

            this.callback.analysisComplete( newData );
          }
        }
      }

      // Pass through our local event handling method...
      BaseAsyncTool.this.onPropertyChange( aEvent );
    }
  }

  // CONSTANTS

  protected static final String PROPERTY_PROGRESS = "progress";
  protected static final String PROPERTY_STATE = "state";

  // CONSTRUCTORS

  /**
   * Creates a new BaseTool instance.
   * 
   * @param aName
   *          the name of the tool as it should appear in the main UI.
   */
  protected BaseAsyncTool( final String aName )
  {
    super( aName );
  }

  // METHODS

  /**
   * Factory method for creating a tool worker.
   * 
   * @return a new instance of the intended tool worker, cannot be
   *         <code>null</code>.
   */
  protected abstract WORKER createToolWorker( final DataContainer aData );

  /**
   * Does the actual processing of data.
   * <p>
   * By default, this method does <tt>getToolWorker().execute()</tt> to start
   * the tool worker in the background. Override this method to do something
   * different, for example, to wrap the tool worker in a UI-dialog.
   * </p>
   * 
   * @param aData
   *          the captured data to process in this tool;
   * @param aContext
   *          the tool context to use during the processing.
   */
  @Override
  protected final void doProcess( final DataContainer aData, final ToolContext aContext,
      final AnalysisCallback aCallback )
  {
    final DIALOG dialog = getDialog();

    // Update the tool worker to the new one...
    dialog.setToolWorkerFactory( new ToolWorkerFactoryImpl( aData, aCallback ) );
    // Show the actual dialog...
    dialog.showDialog( aData );
  }

  /**
   * Allows for custom property change events.
   * 
   * @param aEvent
   *          the property change event, never <code>null</code>.
   */
  protected void onPropertyChange( final PropertyChangeEvent aEvent )
  {
    // NO-op
  }
}

/* EOF */
