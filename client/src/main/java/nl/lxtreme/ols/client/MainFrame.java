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
package nl.lxtreme.ols.client;


import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.*;

import javax.swing.*;

import org.osgi.service.prefs.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.client.action.*;
import nl.lxtreme.ols.client.diagram.*;
import nl.lxtreme.ols.client.diagram.settings.*;
import nl.lxtreme.ols.client.icons.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.swing.*;
import nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.*;
import nl.lxtreme.ols.util.swing.component.*;


/**
 * Denotes the main UI.
 */
public final class MainFrame extends JFrame implements Configurable, Closeable
{
  // INNER TYPES

  /**
   * Provides an about box dialog.
   */
  static final class AboutBox extends JDialog implements Closeable
  {
    // CONSTANTS

    private static final long serialVersionUID = 1L;

    // CONSTRUCTORS

    /**
     * Creates a new AboutBox instance.
     */
    public AboutBox( final Window aOwner, final String aVersion )
    {
      super( aOwner, "About ...", ModalityType.APPLICATION_MODAL );

      final String message = String.format( "<html><body><h3>%s</h3>" //
          + "<p>Copyright 2006-2010 Michael Poppitz<br>" //
          + "Copyright 2010 J.W. Janssen<br><br></p>" //
          + "<p>This software is released under the GNU GPL.<br><br></p>" //
          + "<p>Version: %s<br><br></p>" //
          + "<p>For more information see:</p>" //
          + "<ul>" //
          + "<li>&lt;http://www.lxtreme.nl/ols/&gt;</li>" //
          + "<li>&lt;http://dangerousprototypes.com/open-logic-sniffer/&gt;</li>" //
          + "<li>&lt;http://www.gadgetfactory.net/gf/project/butterflylogic/&gt;</li>" //
          + "<li>&lt;http://www.sump.org/projects/analyzer/&gt;</li>" //
          + "</ul></p></body></html>", Host.FULL_NAME, aVersion );

      final JLabel messageLabel = new JLabel( message );

      final URL url = IconLocator.class.getResource( IconLocator.LOGO );
      final ImageIcon icon = new ImageIcon( url );

      final JLabel iconLabel = new JLabel( icon );
      iconLabel.setBackground( Color.WHITE );

      final JButton closeButton = StandardActionFactory.createCloseButton();

      final JPanel buttonPane = new JPanel();
      buttonPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.LINE_AXIS ) );

      buttonPane.add( Box.createHorizontalGlue() );
      buttonPane.add( closeButton );

      final JPanel contentPane = new JPanel( new GridBagLayout() );
      contentPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      setContentPane( contentPane );

      contentPane.add( iconLabel, //
          new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
              new Insets( 0, 0, 5, 0 ), 0, 0 ) );

      contentPane.add( messageLabel, //
          new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(
              5, 10, 5, 10 ), 0, 0 ) );

      contentPane.add( buttonPane, //
          new GridBagConstraints( 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
              new Insets( 5, 0, 5, 0 ), 0, 0 ) );

      pack();

      setLocationRelativeTo( aOwner );
      setResizable( false );
    }

    // METHODS

    /**
     * Closes this dialog and disposes it.
     */
    public final void close()
    {
      setVisible( false );
      dispose();
    }

    /**
     * @see java.awt.Dialog#show()
     */
    public void showDialog()
    {
      setVisible( true );
    }
  }

  /**
   * Listens to window-close events for our main frame, explicitly invoking code
   * to close it on all platforms.
   */
  static final class MainFrameListener extends WindowAdapter
  {
    /**
     * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing( final WindowEvent aEvent )
    {
      final MainFrame mainFrame = ( MainFrame )aEvent.getSource();
      mainFrame.close();
    }
  }

  // CONSTANTS

  private static final long serialVersionUID = 1L;

  // VARIABLES

  private final Diagram diagram;
  private final JTextStatusBar status;

  private JMenu deviceMenu;
  private JMenu toolsMenu;
  private JMenu windowMenu;

  private final JMenuItem noDevicesItem;
  private final JMenuItem noToolsItem;
  private final ButtonGroup deviceGroup;

  private final ClientController controller;

  // CONSTRUCTORS

  /**
   * Creates a new MainFrame instance.
   * 
   * @param aController
   *          the client controller to use, cannot be <code>null</code>.
   */
  public MainFrame( final ClientController aController )
  {
    super( Host.FULL_NAME );

    this.controller = aController;

    this.noDevicesItem = new JMenuItem( "No Devices." );
    this.noDevicesItem.setEnabled( false );

    this.noToolsItem = new JMenuItem( "No Tools." );
    this.noToolsItem.setEnabled( false );

    this.deviceGroup = new ButtonGroup();

    this.diagram = new Diagram( this.controller );
    this.status = new JTextStatusBar();

    setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
    setSize( 1200, 600 );

    final JToolBar tools = createMenuBars();

    // !!! Always add these after the toolbar/menubar is created !!!
    this.deviceMenu.add( this.noDevicesItem );
    this.toolsMenu.add( this.noToolsItem );

    // Create a scrollpane for the diagram...
    final JScrollPane scrollPane = new JScrollPane( this.diagram );

    final Container contentPane = getContentPane();
    contentPane.setLayout( new BorderLayout() );

    contentPane.add( tools, BorderLayout.PAGE_START );
    contentPane.add( scrollPane, BorderLayout.CENTER );
    contentPane.add( this.status, BorderLayout.PAGE_END );

    // Support closing of this window on Windows/Linux platforms...
    addWindowListener( new MainFrameListener() );

    // Support CMD + W on MacOSX (closes this frame)...
  }

  /**
   * @param aDevController
   */
  public final void addDeviceMenuItem( final String aDeviceName )
  {
    // We're adding one, so, there's at least one device available...
    this.deviceMenu.remove( this.noDevicesItem );

    final JMenuItem menuItem = createDeviceMenuItem( aDeviceName );
    // Determine where in the menu we should add the menu item, this way, we
    // can make the menu appear consistent...
    final int idx = determineDeviceMenuItemIndex( menuItem );

    this.deviceGroup.add( menuItem );
    this.deviceMenu.add( menuItem, idx );

    updateDeviceMenuState( aDeviceName, menuItem, true /* aAdded */);
  }

  // METHODS

  /**
   * @param aTool
   */
  public final void addToolMenuItem( final String aToolName )
  {
    // We're adding one, so, there's at least one device available...
    this.toolsMenu.remove( this.noToolsItem );

    final JMenuItem menuItem = createToolMenuItem( aToolName );
    // Determine where in the menu we should add the menu item, this way, we
    // can make the menu appear consistent...
    final int idx = determineToolMenuItemIndex( menuItem );

    this.toolsMenu.add( menuItem, idx );

    updateToolMenuState( aToolName, menuItem, true /* aAdded */);
  }

  /**
   * @see nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.Closeable#close()
   */
  @Override
  public void close()
  {
    setVisible( false );
    dispose();

    // Make sure that if this frame is closed, the entire application is
    // shutdown as well...
    this.controller.exit();
  }

  /**
   * @param aLocation
   * @return
   */
  public long convertMousePositionToSampleIndex( final Point aLocation )
  {
    return this.diagram.convertPointToSampleIndex( aLocation );
  }

  /**
   * Returns the current diagram settings.
   * 
   * @return the diagram settings, never <code>null</code>.
   */
  public final DiagramSettings getDiagramSettings()
  {
    return this.diagram.getDiagramSettings();
  }

  /**
   * Returns the current zoom scale.
   * 
   * @return a zoom scale, > 0.0
   */
  public double getZoomScale()
  {
    return this.diagram.getScale();
  }

  /**
   * @param aCursorIdx
   */
  public void gotoPosition( final long aSamplePos )
  {
    this.diagram.gotoPosition( aSamplePos );
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#readPreferences(org.osgi.service.prefs.Preferences)
   */
  @Override
  public void readPreferences( final Preferences aPreferences )
  {
    // NO-op
  }

  /**
   * @param aDevController
   */
  public final void removeDeviceMenuItem( final String aDeviceName )
  {
    JMenuItem menuItem = null;
    for ( int i = 0; i < this.deviceMenu.getItemCount(); i++ )
    {
      final JMenuItem comp = this.deviceMenu.getItem( i );
      if ( aDeviceName.equals( comp.getName() ) )
      {
        menuItem = comp;
        break;
      }
    }

    if ( menuItem != null )
    {
      this.deviceGroup.remove( menuItem );
      this.deviceMenu.remove( menuItem );
    }

    updateDeviceMenuState( aDeviceName, menuItem, false /* aAdded */);
  }

  /**
   * @param aTool
   */
  public final void removeToolMenuItem( final String aToolName )
  {
    JMenuItem menuItem = null;
    for ( int i = 0; i < this.toolsMenu.getItemCount(); i++ )
    {
      final JMenuItem comp = this.toolsMenu.getItem( i );
      if ( aToolName.equals( comp.getName() ) )
      {
        menuItem = comp;
        break;
      }
    }

    if ( menuItem != null )
    {
      this.toolsMenu.remove( menuItem );
    }

    updateToolMenuState( aToolName, menuItem, false /* aAdded */);
  }

  /**
   * @param aChannelLabels
   */
  public void setChannelLabels( final String[] aChannelLabels )
  {
    this.diagram.updatePreferredSize();
    this.diagram.revalidate();
  }

  /**
   * Sets the current diagram settings.
   * 
   * @param aDiagramSettings
   *          the diagram settings to set, cannot be <code>null</code>.
   */
  public final void setDiagramSettings( final DiagramSettings aDiagramSettings )
  {
    this.diagram.setDiagramSettings( aDiagramSettings );
    this.diagram.revalidate();
  }

  /**
   * @param aPercentage
   */
  public void setProgress( final int aPercentage )
  {
    this.status.setProgress( aPercentage );
  }

  /**
   * Sets the status bar message to the message given.
   * 
   * @param aMessage
   *          the message to set as status text;
   * @param aMessageArgs
   *          the (optional) message arguments.
   */
  public void setStatus( final String aMessage, final Object... aMessageArgs )
  {
    this.status.showProgressBar( false );

    String message = aMessage;
    if ( ( aMessageArgs != null ) && ( aMessageArgs.length > 0 ) )
    {
      message = MessageFormat.format( message, aMessageArgs );
    }
    this.status.setText( message );
  }

  /**
   * Shows the main about box.
   * 
   * @param aVersion
   *          the version to display in this about box.
   */
  public void showAboutBox( final String aVersion )
  {
    final AboutBox aboutDialog = new AboutBox( this, aVersion );
    aboutDialog.showDialog();
  }

  /**
   * @see nl.lxtreme.ols.api.ProgressCallback#updateProgress(int)
   */
  public void updateProgress( final int aPercentage )
  {
    this.status.setProgress( aPercentage );
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#writePreferences(org.osgi.service.prefs.Preferences)
   */
  @Override
  public void writePreferences( final Preferences aPreferences )
  {
    // NO-op
  }

  /**
   * 
   */
  public void zoomDefault()
  {
    this.diagram.zoomDefault();
  }

  /**
   * 
   */
  public void zoomIn()
  {
    this.diagram.zoomIn();
  }

  /**
   * 
   */
  public void zoomOut()
  {
    this.diagram.zoomOut();
  }

  /**
   * 
   */
  public void zoomToFit()
  {
    this.diagram.zoomToFit();
  }

  /**
   * @param aDevController
   * @return
   */
  private JMenuItem createDeviceMenuItem( final String aDeviceName )
  {
    final JMenuItem menuItem = new JRadioButtonMenuItem( new SelectDeviceAction( this.controller, aDeviceName ) );
    menuItem.setName( aDeviceName );
    return menuItem;
  }

  /**
   * Creates the menu bar with all menu's and the accompanying toolbar.
   * 
   * @return the toolbar, never <code>null</code>.
   */
  private JToolBar createMenuBars()
  {
    final JMenuBar bar = new JMenuBar();
    setJMenuBar( bar );

    final JMenu file = new JMenu( "File" );
    bar.add( file );

    file.add( this.controller.getAction( OpenProjectAction.ID ) );
    file.add( this.controller.getAction( SaveProjectAction.ID ) );
    file.addSeparator();
    file.add( this.controller.getAction( OpenDataFileAction.ID ) );
    file.add( this.controller.getAction( SaveDataFileAction.ID ) );

    if ( HostUtils.needsExitMenuItem() )
    {
      file.add( new JSeparator() );
      file.add( this.controller.getAction( ExitAction.ID ) );
    }

    this.deviceMenu = bar.add( new JMenu( "Device" ) );
    this.toolsMenu = bar.add( new JMenu( "Tools" ) );

    final JMenu diagramMenu = bar.add( new JMenu( "Diagram" ) );

    diagramMenu.add( this.controller.getAction( ZoomInAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomOutAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomDefaultAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomFitAction.ID ) );
    diagramMenu.addSeparator();
    diagramMenu.add( this.controller.getAction( GotoTriggerAction.ID ) );
    diagramMenu.add( this.controller.getAction( GotoCursor1Action.ID ) );
    diagramMenu.add( this.controller.getAction( GotoCursor2Action.ID ) );
    diagramMenu.addSeparator();
    diagramMenu.add( new JCheckBoxMenuItem( this.controller.getAction( SetCursorModeAction.ID ) ) );
    diagramMenu.add( this.controller.getAction( ShowModeSettingsAction.ID ) );
    diagramMenu.add( this.controller.getAction( ShowDiagramLabelsAction.ID ) );

    if ( HostUtils.isMacOSX() )
    {
      this.windowMenu = bar.add( new JMenu( "Window" ) );

      this.windowMenu.add( new JMenuItem( StandardActionFactory.createCloseAction() ) );
      this.windowMenu.addSeparator();

      for ( Window window : Window.getWindows() )
      {
        this.windowMenu.add( new JMenuItem( new FocusWindowAction( window ) ) );
      }
    }

    final JMenu helpMenu = bar.add( new JMenu( "Help" ) );
    helpMenu.add( this.controller.getAction( ShowGeneralSettingsAction.ID ) );
    helpMenu.addSeparator();
    helpMenu.add( this.controller.getAction( HelpAboutAction.ID ) );

    final JToolBar toolbar = new JToolBar();
    toolbar.setRollover( true );

    toolbar.add( this.controller.getAction( OpenDataFileAction.ID ) );
    toolbar.add( this.controller.getAction( SaveDataFileAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( CaptureAction.ID ) );
    toolbar.add( this.controller.getAction( CancelCaptureAction.ID ) );
    toolbar.add( this.controller.getAction( RepeatCaptureAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( ZoomInAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomOutAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomDefaultAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomFitAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( GotoTriggerAction.ID ) );
    toolbar.add( this.controller.getAction( GotoCursor1Action.ID ) );
    toolbar.add( this.controller.getAction( GotoCursor2Action.ID ) );
    // toolbar.addSeparator();

    return toolbar;
  }

  /**
   * @param aTool
   * @return
   */
  private JMenuItem createToolMenuItem( final String aToolName )
  {
    final JMenuItem menuItem = new JMenuItem( new RunAnalysisToolAction( this.controller, aToolName ) );
    menuItem.setName( aToolName );
    return menuItem;
  }

  /**
   * Determines the index in the menu where the given menu item should be
   * inserted.
   * 
   * @param aMenuItem
   *          the menu item to add, cannot be <code>null</code>.
   * @return the position in the menu to add the given menu item, -1 if the menu
   *         item should be added as last item.
   */
  private int determineDeviceMenuItemIndex( final JMenuItem aMenuItem )
  {
    final String newMenuItem = aMenuItem.getText();

    int idx = -1;
    for ( int i = 0; ( idx < 0 ) && ( i < this.deviceMenu.getItemCount() ); i++ )
    {
      final String nameA = this.deviceMenu.getItem( i ).getText();
      final int comparison = newMenuItem.compareTo( nameA );
      if ( comparison < 0 )
      {
        idx = i;
      }
    }
    return idx;
  }

  /**
   * Determines the index in the menu where the given menu item should be
   * inserted.
   * 
   * @param aMenuItem
   *          the menu item to add, cannot be <code>null</code>.
   * @return the position in the menu to add the given menu item, -1 if the menu
   *         item should be added as last item.
   */
  private int determineToolMenuItemIndex( final JMenuItem aMenuItem )
  {
    final String newMenuItem = aMenuItem.getText();

    int idx = -1;
    for ( int i = 0; ( idx < 0 ) && ( i < this.toolsMenu.getItemCount() ); i++ )
    {
      final String nameA = this.toolsMenu.getItem( i ).getText();
      final int comparison = newMenuItem.compareTo( nameA );
      if ( comparison < 0 )
      {
        idx = i;
      }
    }
    return idx;
  }

  /**
   * @param aDevController
   * @param aMenuItem
   * @param aAdded
   */
  private void updateDeviceMenuState( final String aDeviceName, final JMenuItem aMenuItem, final boolean aAdded )
  {
    if ( aAdded )
    {
      // Always select the first added device...
      if ( this.deviceMenu.getItemCount() == 1 )
      {
        aMenuItem.setSelected( true );
      }
    }
    else
    {
      if ( this.deviceMenu.getItemCount() == 0 )
      {
        // We've removed the last one...
        this.deviceMenu.add( this.noDevicesItem );
      }
    }

    this.deviceMenu.revalidate();
    this.deviceMenu.repaint();
  }

  /**
   * @param aTool
   * @param aMenuItem
   * @param aAdded
   */
  private void updateToolMenuState( final String aToolName, final JMenuItem aMenuItem, final boolean aAdded )
  {
    if ( !aAdded )
    {
      if ( this.toolsMenu.getItemCount() == 0 )
      {
        // We've removed the last one...
        this.toolsMenu.add( this.noToolsItem );
      }
    }

    this.toolsMenu.revalidate();
    this.toolsMenu.repaint();
  }
}
