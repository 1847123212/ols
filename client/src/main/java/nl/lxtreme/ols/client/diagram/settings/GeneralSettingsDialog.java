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
package nl.lxtreme.ols.client.diagram.settings;


import static nl.lxtreme.ols.util.swing.SwingComponentUtils.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.client.diagram.settings.DiagramSettings.ColorScheme;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.swing.*;
import nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.Closeable;

import org.osgi.service.prefs.*;


/**
 * Stores diagram "mode" settings and provides a dialog for changing them.
 * 
 * @author Michael "Mr. Sump" Poppitz
 * @author J.W. Janssen
 */
public class GeneralSettingsDialog extends JDialog implements Configurable, Closeable
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  private static final int CHANNELS_PER_BLOCK = 8;

  // VARIABLES

  private final MutableDiagramSettings settings;
  private boolean result;

  private JTabbedPane tabbedPane;
  private JComboBox colorScheme;
  private JRadioButton colorLabels;
  private JRadioButton colorSignals;
  private JTextField channelHeight;
  private JTextField signalHeight;
  private JTextField scopeHeight;
  private JTextField backgroundColor;
  private JTextField gridColor;
  private JTextField labelColor;
  private JTextField scopeColor;
  private JTextField signalColor;
  private JTextField textColor;
  private JTextField timeColor;
  private JTextField triggerColor;
  private JTextField[] cursorColor;
  private JTextField[] channelColor;

  // CONSTRUCTORS

  /**
   * Constructs diagram settings component.
   * 
   * @param aParent
   * @param aSettings
   */
  public GeneralSettingsDialog( final Window aParent, final DiagramSettings aSettings )
  {
    super( aParent, "Preferences", ModalityType.DOCUMENT_MODAL );

    this.settings = new MutableDiagramSettings( aSettings );

    initDialog();

    syncUiWithSettings();
  }

  // METHODS

  /**
   * @param aTextField
   * @return
   */
  private static Color getColorValue( final JTextField aTextField )
  {
    if ( aTextField == null )
    {
      return null;
    }

    final String text = aTextField.getText();
    if ( DisplayUtils.isEmpty( text ) )
    {
      return null;
    }

    return SwingComponentUtils.parseColor( text );
  }

  /**
   * @param aTextField
   * @return
   */
  private static int getIntegerValue( final JTextField aTextField )
  {
    if ( aTextField == null )
    {
      return -1;
    }

    final String text = aTextField.getText();
    if ( DisplayUtils.isEmpty( text ) )
    {
      return -1;
    }

    int result = -1;
    try
    {
      result = Integer.valueOf( text );
    }
    catch ( NumberFormatException exception )
    {
      // Ignore; text doesn't compute to an integer value...
    }

    return result;
  }

  /**
   * @see nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.Closeable#close()
   */
  @Override
  public void close()
  {
    setVisible( false );
    dispose();
  }

  /**
   * Returns the (mutated) diagram settings.
   * 
   * @return the diagram settings, never <code>null</code>.
   */
  public final DiagramSettings getDiagramSettings()
  {
    return this.settings;
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#readPreferences(org.osgi.service.prefs.Preferences)
   */
  public void readPreferences( final Preferences aProperties )
  {
    // NO-op
  }

  /**
   * Display the settings dialog. If the user clicks ok, all changes are
   * reflected in the properties of this object. Otherwise changes are
   * discarded.
   * 
   * @return <code>OK</code> when user accepted changes, <code>CANCEL</code>
   *         otherwise
   */
  public boolean showDialog()
  {
    setVisible( true );
    return ( this.result );
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#writePreferences(org.osgi.service.prefs.Preferences)
   */
  public void writePreferences( final Preferences aProperties )
  {
    // NO-op
  }

  /**
   * Enables/disables all color scheme-related tabs
   * 
   * @param aEnable
   *          <code>true</code> to enable all color scheme-related tabs,
   *          <code>false</code> to disable them.
   */
  final void setColorSchemeTabState( final boolean aEnable )
  {
    for ( int i = 1; i < this.tabbedPane.getTabCount(); i++ )
    {
      this.tabbedPane.setEnabledAt( i, aEnable );
    }
  }

  /**
   * @return
   */
  private JComponent createButtonsPane()
  {
    final JButton cancel = StandardActionFactory.createCloseButton();
    final JButton ok = new JButton( "Ok" );
    ok.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent aEvent )
      {
        final boolean validSettings = syncSettingsWithUi();
        if ( validSettings )
        {
          GeneralSettingsDialog.this.result = validSettings;
          close();
        }
      }
    } );

    return SwingComponentUtils.createButtonPane( new JButton[] { ok, cancel } );
  }

  /**
   * @param aBlockNr
   *          the channel block number to create a color scheme editor for, >= 0
   *          && < 4.
   * @return
   */
  private JPanel createChannelColorSchemePane( final int aBlockNr )
  {
    if ( this.channelColor == null )
    {
      this.channelColor = new JTextField[DataContainer.MAX_CHANNELS];
    }

    final JPanel editorsPane = new JPanel( new SpringLayout() );

    for ( int i = 0; i < CHANNELS_PER_BLOCK; i++ )
    {
      final int channelIdx = ( aBlockNr * CHANNELS_PER_BLOCK ) + i;

      final JLabel label = createRightAlignedLabel( String.format( "Channel %d color", ( channelIdx + 1 ) ) );

      this.channelColor[channelIdx] = new JTextField( 10 );

      editorsPane.add( label );
      editorsPane.add( this.channelColor[channelIdx] );
    }

    SpringLayoutUtils.makeCompactGrid( editorsPane, CHANNELS_PER_BLOCK, 2, 6, 6, 6, 6 );

    final JPanel result = new JPanel( new GridBagLayout() );

    result.add( editorsPane, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
    result.add( new JLabel(), new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

    return result;
  }

  /**
   * @return
   */
  private JPanel createColorSchemePane()
  {
    this.backgroundColor = new JTextField( 10 );
    this.gridColor = new JTextField( 10 );
    this.labelColor = new JTextField( 10 );
    this.scopeColor = new JTextField( 10 );
    this.signalColor = new JTextField( 10 );
    this.textColor = new JTextField( 10 );
    this.timeColor = new JTextField( 10 );
    this.triggerColor = new JTextField( 10 );

    // Create panel...
    final JPanel editorsPane = new JPanel( new SpringLayout() );
    editorsPane.add( createRightAlignedLabel( "Background color" ) );
    editorsPane.add( this.backgroundColor );
    editorsPane.add( createRightAlignedLabel( "Grid color" ) );
    editorsPane.add( this.gridColor );
    editorsPane.add( createRightAlignedLabel( "Label color" ) );
    editorsPane.add( this.labelColor );
    editorsPane.add( createRightAlignedLabel( "Scope color" ) );
    editorsPane.add( this.scopeColor );
    editorsPane.add( createRightAlignedLabel( "Signal color" ) );
    editorsPane.add( this.signalColor );
    editorsPane.add( createRightAlignedLabel( "Text color" ) );
    editorsPane.add( this.textColor );
    editorsPane.add( createRightAlignedLabel( "Time color" ) );
    editorsPane.add( this.timeColor );
    editorsPane.add( createRightAlignedLabel( "Trigger color" ) );
    editorsPane.add( this.triggerColor );

    SpringLayoutUtils.makeCompactGrid( editorsPane, 8, 2, 6, 6, 6, 6 );

    final JPanel result = new JPanel( new GridBagLayout() );

    result.add( editorsPane, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
    result.add( new JLabel(), new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

    return result;
  }

  /**
   * @return
   */
  private JPanel createCursorColorSchemePane()
  {
    final JPanel editorsPane = new JPanel( new SpringLayout() );

    this.cursorColor = new JTextField[DataContainer.MAX_CURSORS];

    for ( int i = 0; i < this.cursorColor.length; i++ )
    {
      final JLabel label = createRightAlignedLabel( String.format( "Cursor %d color", ( i + 1 ) ) );
      this.cursorColor[i] = new JTextField( 10 );

      editorsPane.add( label );
      editorsPane.add( this.cursorColor[i] );
    }

    SpringLayoutUtils.makeCompactGrid( editorsPane, DataContainer.MAX_CURSORS, 2, 6, 6, 6, 6 );

    final JPanel result = new JPanel( new GridBagLayout() );

    result.add( editorsPane, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
    result.add( new JLabel(), new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

    return result;
  }

  /**
   * @return
   */
  private JPanel createGeneralSettingsPane()
  {
    this.channelHeight = new JTextField( 10 );
    this.signalHeight = new JTextField( 10 );
    this.scopeHeight = new JTextField( 10 );

    this.colorLabels = new JRadioButton( "Labels" );
    this.colorSignals = new JRadioButton( "Signals" );

    final ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add( this.colorLabels );
    buttonGroup.add( this.colorSignals );

    final JPanel colorLabelsOrSignals = new JPanel();
    colorLabelsOrSignals.add( this.colorLabels, BorderLayout.WEST );
    colorLabelsOrSignals.add( this.colorSignals, BorderLayout.EAST );

    this.colorScheme = new JComboBox( new ColorScheme[] { ColorScheme.LIGHT, ColorScheme.DARK } );
    this.colorScheme.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent aEvent )
      {
        final boolean customSchemeSelected = ColorScheme.CUSTOM.equals( aEvent.getItem() );
        setColorSchemeTabState( customSchemeSelected );
      }
    } );

    // Create panel...
    final JPanel editorsPane = new JPanel( new SpringLayout() );

    SpringLayoutUtils.addSeparator( editorsPane, "Layout" );

    editorsPane.add( createRightAlignedLabel( "Channel height" ) );
    editorsPane.add( this.channelHeight );
    editorsPane.add( createRightAlignedLabel( "Signal height" ) );
    editorsPane.add( this.signalHeight );
    editorsPane.add( createRightAlignedLabel( "Scope height" ) );
    editorsPane.add( this.scopeHeight );

    SpringLayoutUtils.addSeparator( editorsPane, "Color scheme" );

    editorsPane.add( createRightAlignedLabel( "Apply channel colors to" ) );
    editorsPane.add( colorLabelsOrSignals );
    editorsPane.add( createRightAlignedLabel( "Color scheme" ) );
    editorsPane.add( this.colorScheme );

    SpringLayoutUtils.makeEditorGrid( editorsPane, 10, 10 );

    final JPanel result = new JPanel( new GridBagLayout() );

    result.add( editorsPane, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
    result.add( new JLabel(), new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

    return result;
  }

  /**
   * Initializes and builds this dialog.
   */
  private void initDialog()
  {
    this.tabbedPane = new JTabbedPane( SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT );
    this.tabbedPane.addTab( "General", createGeneralSettingsPane() );
    this.tabbedPane.addTab( "General colors", createColorSchemePane() );
    this.tabbedPane.addTab( "Cursor colors", createCursorColorSchemePane() );
    this.tabbedPane.addTab( "Channel colors", createChannelColorSchemePane( 0 ) );

    final JComponent buttonPane = createButtonsPane();

    SwingComponentUtils.setupDialogContentPane( this, this.tabbedPane, buttonPane );
  }

  /**
   * Shows an error message.
   * 
   * @param aMessage
   *          the error message to show, cannot be <code>null</code>.
   */
  private void showErrorMessage( final String aMessage )
  {
    JOptionPane.showMessageDialog( this, aMessage, "Error", JOptionPane.ERROR_MESSAGE );
  }

  /**
   * Synchronizes the settings with the values chosen in the UI.
   * 
   * @return <code>true</code> if no errors were found in the settings,
   *         <code>false</code> otherwise.
   */
  private boolean syncSettingsWithUi()
  {
    final int channelHeightValue = getIntegerValue( this.channelHeight );
    if ( channelHeightValue <= 0 )
    {
      showErrorMessage( "Invalid channel height." );
      return false;
    }

    final int signalHeightValue = getIntegerValue( this.signalHeight );
    if ( ( signalHeightValue <= 0 ) || ( signalHeightValue > ( channelHeightValue - 2 ) ) )
    {
      showErrorMessage( "Invalid signal height. Should be larger than zero and less than the channel height." );
      return false;
    }

    final int scopeHeightValue = getIntegerValue( this.scopeHeight );
    if ( scopeHeightValue <= 0 )
    {
      showErrorMessage( "Invalid scope height." );
      return false;
    }

    final boolean colorLabelsValue = this.colorLabels.isSelected();
    final ColorScheme colorSchemeValue = ( ColorScheme )this.colorScheme.getSelectedItem();

    final Color backgroundColorValue = getColorValue( this.backgroundColor );
    if ( backgroundColorValue == null )
    {
      showErrorMessage( "Invalid background color." );
      return false;
    }

    final Color gridColorValue = getColorValue( this.gridColor );
    if ( gridColorValue == null )
    {
      showErrorMessage( "Invalid grid color." );
      return false;
    }

    final Color labelColorValue = getColorValue( this.labelColor );
    if ( labelColorValue == null )
    {
      showErrorMessage( "Invalid label color." );
      return false;
    }

    final Color scopeColorValue = getColorValue( this.scopeColor );
    if ( scopeColorValue == null )
    {
      showErrorMessage( "Invalid scope color." );
      return false;
    }

    final Color signalColorValue = getColorValue( this.signalColor );
    if ( signalColorValue == null )
    {
      showErrorMessage( "Invalid signal color." );
      return false;
    }

    final Color textColorValue = getColorValue( this.textColor );
    if ( textColorValue == null )
    {
      showErrorMessage( "Invalid text color." );
      return false;
    }

    final Color timeColorValue = getColorValue( this.timeColor );
    if ( timeColorValue == null )
    {
      showErrorMessage( "Invalid time color." );
      return false;
    }

    final Color triggerColorValue = getColorValue( this.triggerColor );
    if ( triggerColorValue == null )
    {
      showErrorMessage( "Invalid trigger color." );
      return false;
    }

    for ( int i = 0; i < this.cursorColor.length; i++ )
    {
      final Color color = getColorValue( this.cursorColor[i] );
      if ( color == null )
      {
        showErrorMessage( "Invalid cursor " + ( i + 1 ) + " color." );
        return false;
      }
    }

    for ( int i = 0; i < this.channelColor.length; i++ )
    {
      final Color color = getColorValue( this.channelColor[i] );
      if ( ( this.channelColor[i] != null ) && ( color == null ) )
      {
        showErrorMessage( "Invalid channel " + i + " color." );
        return false;
      }
    }

    // Update all settings in one go; this way, we make it more or less
    // atomic...
    synchronized ( this.settings )
    {
      this.settings.setChannelHeight( channelHeightValue );
      this.settings.setSignalHeight( signalHeightValue );
      this.settings.setScopeHeight( scopeHeightValue );
      this.settings.setColorLabels( colorLabelsValue );
      this.settings.setColorScheme( colorSchemeValue );

      if ( ColorScheme.CUSTOM.equals( colorSchemeValue ) )
      {
        this.settings.setBackgroundColor( backgroundColorValue );
        this.settings.setGridColor( gridColorValue );
        this.settings.setLabelColor( labelColorValue );
        this.settings.setScopeColor( scopeColorValue );
        this.settings.setSignalColor( signalColorValue );
        this.settings.setTextColor( textColorValue );
        this.settings.setTimeColor( timeColorValue );
        this.settings.setTriggerColor( triggerColorValue );

        for ( int i = 0; i < this.cursorColor.length; i++ )
        {
          final Color color = getColorValue( this.cursorColor[i] );
          this.settings.setCursorColor( i, color );
        }

        for ( int i = 0; i < this.channelColor.length; i++ )
        {
          final Color color = getColorValue( this.channelColor[i] );
          this.settings.setChannelColor( i, color );
        }
      }
    }

    return true;
  }

  /**
   * Synchronizes the UI with the current settings.
   */
  private void syncUiWithSettings()
  {
    synchronized ( this.settings )
    {
      final ColorScheme selectedScheme = this.settings.getColorScheme();
      final boolean customSchemeSelected = ColorScheme.CUSTOM.equals( selectedScheme );

      this.channelHeight.setText( String.valueOf( this.settings.getChannelHeight() ) );
      this.signalHeight.setText( String.valueOf( this.settings.getSignalHeight() ) );
      this.scopeHeight.setText( String.valueOf( this.settings.getScopeHeight() ) );
      this.colorLabels.setSelected( this.settings.isColorLabels() );
      this.colorSignals.setSelected( this.settings.isColorSignals() );
      this.colorScheme.setSelectedItem( selectedScheme );

      this.backgroundColor.setText( SwingComponentUtils.toString( this.settings.getBackgroundColor() ) );
      this.gridColor.setText( SwingComponentUtils.toString( this.settings.getGridColor() ) );
      this.labelColor.setText( SwingComponentUtils.toString( this.settings.getLabelColor() ) );
      this.scopeColor.setText( SwingComponentUtils.toString( this.settings.getScopeColor() ) );
      this.signalColor.setText( SwingComponentUtils.toString( this.settings.getSignalColor() ) );
      this.textColor.setText( SwingComponentUtils.toString( this.settings.getTextColor() ) );
      this.timeColor.setText( SwingComponentUtils.toString( this.settings.getTimeColor() ) );
      this.triggerColor.setText( SwingComponentUtils.toString( this.settings.getTriggerColor() ) );

      for ( int i = 0; i < this.cursorColor.length; i++ )
      {
        final JTextField textfield = this.cursorColor[i];
        if ( textfield != null )
        {
          textfield.setText( SwingComponentUtils.toString( this.settings.getCursorColor( i ) ) );
        }
      }

      for ( int i = 0; i < this.channelColor.length; i++ )
      {
        final JTextField textfield = this.channelColor[i];
        if ( textfield != null )
        {
          textfield.setText( SwingComponentUtils.toString( this.settings.getChannelColor( i ) ) );
        }
      }

      setColorSchemeTabState( customSchemeSelected );
    }
  }
}
