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
package nl.lxtreme.ols.tool.spi;


import static nl.lxtreme.ols.util.swing.SwingComponentUtils.*;
import static nl.lxtreme.ols.util.ExportUtils.HtmlExporter.*;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;

import org.osgi.service.prefs.*;

import nl.lxtreme.ols.tool.base.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.ExportUtils.CsvExporter;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter.Element;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter.MacroResolver;
import nl.lxtreme.ols.util.ExportUtils.HtmlFileExporter;
import nl.lxtreme.ols.util.NumberUtils.BitOrder;
import nl.lxtreme.ols.util.swing.*;


/**
 * The Dialog Class
 * 
 * @author Frank Kunz The dialog class draws the basic dialog with a grid
 *         layout. The dialog consists of three main parts. A settings panel, a
 *         table panel and three buttons.
 */
public final class SPIProtocolAnalysisDialog extends BaseAsyncToolDialog<SPIDataSet, SPIAnalyserWorker>
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger( SPIProtocolAnalysisDialog.class.getName() );

  // VARIABLES

  private String[] modearray;
  private String[] bitarray;
  private String[] orderarray;
  private JComboBox sck;
  private JComboBox miso;
  private JComboBox mosi;
  private JComboBox cs;
  private JComboBox mode;
  private JComboBox bits;
  private JComboBox order;
  private JEditorPane outText;
  private JCheckBox reportCS;
  private JCheckBox honourCS;

  private RestorableAction runAnalysisAction;
  private Action exportAction;
  private Action closeAction;

  // CONSTRUCTORS

  /**
   * @param aOwner
   * @param aName
   */
  public SPIProtocolAnalysisDialog( final Window aOwner, final String aName )
  {
    super( aOwner, aName );

    initDialog();
  }

  // METHODS

  /**
   * This is the SPI protocol decoder core The decoder scans for a decode start
   * event like CS high to low edge or the trigger of the captured data. After
   * this the decoder starts to decode the data by the selected mode, number of
   * bits and bit order. The decoded data are put to a JTable object directly.
   */
  @Override
  public void onToolWorkerReady( final SPIDataSet aAnalysisResult )
  {
    super.onToolWorkerReady( aAnalysisResult );

    try
    {
      final String htmlPage;
      if ( aAnalysisResult != null )
      {
        htmlPage = toHtmlPage( null /* aFile */, aAnalysisResult );
        this.exportAction.setEnabled( !aAnalysisResult.isEmpty() );
      }
      else
      {
        htmlPage = getEmptyHtmlPage();
        this.exportAction.setEnabled( false );
      }

      this.outText.setText( htmlPage );
      this.outText.setEditable( false );

      this.runAnalysisAction.restore();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      // Should not happen in this situation!
      throw new RuntimeException( exception );
    }
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#readPreferences(org.osgi.service.prefs.Preferences)
   */
  public void readPreferences( final Preferences aPrefs )
  {
    SwingComponentUtils.setSelected( this.reportCS, aPrefs.getBoolean( "reportCS", Boolean.TRUE ) );
    SwingComponentUtils.setSelected( this.honourCS, aPrefs.getBoolean( "honourCS", Boolean.FALSE ) );
    SwingComponentUtils.setSelectedIndex( this.sck, aPrefs.getInt( "sck", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.miso, aPrefs.getInt( "miso", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.mosi, aPrefs.getInt( "mosi", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.cs, aPrefs.getInt( "cs", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.mode, aPrefs.getInt( "mode", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.bits, aPrefs.getInt( "bits", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.order, aPrefs.getInt( "order", -1 ) );
  }

  /**
   * @see nl.lxtreme.ols.tool.base.ToolDialog#reset()
   */
  @Override
  public void reset()
  {
    this.outText.setText( getEmptyHtmlPage() );
    this.outText.setEditable( false );

    this.exportAction.setEnabled( false );

    this.runAnalysisAction.restore();

    setControlsEnabled( true );
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#writePreferences(org.osgi.service.prefs.Preferences)
   */
  public void writePreferences( final Preferences aProperties )
  {
    aProperties.putBoolean( "reportCS", this.reportCS.isSelected() );
    aProperties.putBoolean( "honourCS", this.honourCS.isSelected() );
    aProperties.putInt( "sck", this.sck.getSelectedIndex() );
    aProperties.putInt( "miso", this.miso.getSelectedIndex() );
    aProperties.putInt( "mosi", this.mosi.getSelectedIndex() );
    aProperties.putInt( "cs", this.cs.getSelectedIndex() );
    aProperties.putInt( "mode", this.mode.getSelectedIndex() );
    aProperties.putInt( "bits", this.bits.getSelectedIndex() );
    aProperties.putInt( "order", this.order.getSelectedIndex() );
  }

  /**
   * set the controls of the dialog enabled/disabled
   * 
   * @param aEnable
   *          status of the controls
   */
  @Override
  protected void setControlsEnabled( final boolean aEnable )
  {
    this.reportCS.setEnabled( aEnable );
    this.honourCS.setEnabled( aEnable );
    this.sck.setEnabled( aEnable );
    this.miso.setEnabled( aEnable );
    this.mosi.setEnabled( aEnable );
    this.cs.setEnabled( aEnable );
    this.mode.setEnabled( aEnable );
    this.bits.setEnabled( aEnable );
    this.order.setEnabled( aEnable );

    this.closeAction.setEnabled( aEnable );
  }

  /**
   * @see nl.lxtreme.ols.tool.base.BaseAsyncToolDialog#setupToolWorker(nl.lxtreme.ols.tool.base.BaseAsyncToolWorker)
   */
  @Override
  protected void setupToolWorker( final SPIAnalyserWorker aToolWorker )
  {
    aToolWorker.setBitCount( Integer.parseInt( ( String )this.bits.getSelectedItem() ) - 1 );
    aToolWorker.setCSIndex( this.cs.getSelectedIndex() );
    aToolWorker.setSCKIndex( this.sck.getSelectedIndex() );
    aToolWorker.setMisoIndex( this.miso.getSelectedIndex() );
    aToolWorker.setMosiIndex( this.mosi.getSelectedIndex() );
    aToolWorker.setOrder( "MSB first".equals( this.order.getSelectedItem() ) ? BitOrder.MSB_FIRST : BitOrder.LSB_FIRST );
    aToolWorker.setMode( SPIMode.parse( ( String )this.mode.getSelectedItem() ) );
    aToolWorker.setReportCS( this.reportCS.isSelected() );
    aToolWorker.setHonourCS( this.honourCS.isSelected() );
  }

  /**
   * exports the table data to a CSV file
   * 
   * @param aFile
   *          File object
   */
  @Override
  protected void storeToCsvFile( final File aFile, final SPIDataSet aDataSet )
  {
    try
    {
      final CsvExporter exporter = ExportUtils.createCsvExporter( aFile );

      exporter.setHeaders( "index", "start-time", "end-time", "event?", "event-type", "MOSI data", "MISO data" );

      final List<SPIData> decodedData = aDataSet.getData();
      for ( int i = 0; i < decodedData.size(); i++ )
      {
        final SPIData ds = decodedData.get( i );

        final String startTime = aDataSet.getDisplayTime( ds.getStartSampleIndex() );
        final String endTime = aDataSet.getDisplayTime( ds.getStartSampleIndex() );
        final String mosiDataValue = ds.isMosiData() ? Integer.toString( ds.getDataValue() ) : null;
        final String misoDataValue = ds.isMisoData() ? Integer.toString( ds.getDataValue() ) : null;

        exporter.addRow( i, startTime, endTime, ds.isEvent(), ds.getEventName(), mosiDataValue, misoDataValue );
      }

      exporter.close();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      if ( LOG.isLoggable( Level.WARNING ) )
      {
        LOG.log( Level.WARNING, "CSV export failed!", exception );
      }
    }
  }

  /**
   * stores the data to a HTML file
   * 
   * @param aFile
   *          file object
   */
  @Override
  protected void storeToHtmlFile( final File aFile, final SPIDataSet aDataSet )
  {
    try
    {
      toHtmlPage( aFile, aDataSet );
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      if ( LOG.isLoggable( Level.WARNING ) )
      {
        LOG.log( Level.WARNING, "HTML export failed!", exception );
      }
    }
  }

  /**
   * @return
   */
  private JComponent createButtonPane()
  {
    final JButton runAnalysisButton = createRunAnalysisButton();
    this.runAnalysisAction = ( RestorableAction )runAnalysisButton.getAction();

    final JButton exportButton = createExportButton();
    this.exportAction = exportButton.getAction();
    this.exportAction.setEnabled( false );

    final JButton closeButton = createCloseButton();
    this.closeAction = closeButton.getAction();

    return SwingComponentUtils.createButtonPane( closeButton, runAnalysisButton, exportButton );
  }

  /**
   * Creates the HTML template for exports to HTML.
   * 
   * @param aExporter
   *          the HTML exporter instance to use, cannot be <code>null</code>.
   * @return a HTML exporter filled with the template, never <code>null</code>.
   */
  private HtmlExporter createHtmlTemplate( final HtmlExporter aExporter )
  {
    aExporter.addCssStyle( "body { font-family: sans-serif; } " );
    aExporter.addCssStyle( "table { border-width: 1px; border-spacing: 0px; border-color: gray;"
        + " border-collapse: collapse; border-style: solid; margin-bottom: 15px; } " );
    aExporter.addCssStyle( "table th { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " background-color: #C0C0FF; text-align: left; font-weight: bold; font-family: sans-serif; } " );
    aExporter.addCssStyle( "table td { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " font-family: monospace; } " );
    aExporter.addCssStyle( ".date { text-align: right; font-size: x-small; margin-bottom: 15px; } " );
    aExporter.addCssStyle( ".w100 { width: 100%; } " );
    aExporter.addCssStyle( ".w35 { width: 35%; } " );
    aExporter.addCssStyle( ".w30 { width: 30%; } " );
    aExporter.addCssStyle( ".w15 { width: 15%; } " );
    aExporter.addCssStyle( ".w10 { width: 10%; } " );
    aExporter.addCssStyle( ".w8 { width: 8%; } " );
    aExporter.addCssStyle( ".w7 { width: 7%; } " );

    final Element body = aExporter.getBody();
    body.addChild( H1 ).addContent( "SPI Analysis results" );
    body.addChild( HR );
    body.addChild( DIV ).addAttribute( "class", "date" ).addContent( "Generated: ", "{date-now}" );

    Element table, tr, thead, tbody;

    table = body.addChild( TABLE ).addAttribute( "class", "w100" );
    thead = table.addChild( THEAD );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w30" ).addAttribute( "colspan", "2" );
    tr.addChild( TH ).addAttribute( "class", "w35" ).addAttribute( "colspan", "4" ).addContent( "MOSI" );
    tr.addChild( TH ).addAttribute( "class", "w35" ).addAttribute( "colspan", "4" ).addContent( "MISO" );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Index" );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Time" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Hex" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Bin" );
    tr.addChild( TH ).addAttribute( "class", "w8" ).addContent( "Dec" );
    tr.addChild( TH ).addAttribute( "class", "w7" ).addContent( "ASCII" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Hex" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Bin" );
    tr.addChild( TH ).addAttribute( "class", "w8" ).addContent( "Dec" );
    tr.addChild( TH ).addAttribute( "class", "w7" ).addContent( "ASCII" );
    tbody = table.addChild( TBODY );
    tbody.addContent( "{decoded-data}" );

    return aExporter;
  }

  /**
   * @return
   */
  private JPanel createPreviewPane()
  {
    final JPanel panTable = new JPanel( new GridLayout( 1, 1, 0, 0 ) );

    this.outText = new JEditorPane( "text/html", getEmptyHtmlPage() );
    this.outText.setEditable( false );

    panTable.add( new JScrollPane( this.outText ) );

    return panTable;
  }

  /**
   * @return
   */
  private JPanel createSettingsPane()
  {
    final String channels[] = new String[32];
    for ( int i = 0; i < 32; i++ )
    {
      channels[i] = new String( "Channel " + i );
    }

    final JPanel settings = new JPanel( new SpringLayout() );

    SpringLayoutUtils.addSeparator( settings, "Settings" );

    settings.add( createRightAlignedLabel( "SCK" ) );
    this.sck = new JComboBox( channels );
    settings.add( this.sck );

    settings.add( createRightAlignedLabel( "MISO" ) );
    this.miso = new JComboBox( channels );
    settings.add( this.miso );

    settings.add( createRightAlignedLabel( "MOSI" ) );
    this.mosi = new JComboBox( channels );
    settings.add( this.mosi );

    settings.add( createRightAlignedLabel( "/CS" ) );
    this.cs = new JComboBox( channels );
    settings.add( this.cs );

    settings.add( createRightAlignedLabel( "Mode" ) );
    this.modearray = new String[4];
    for ( int i = 0; i < this.modearray.length; i++ )
    {
      this.modearray[i] = new String( "" + i );
    }
    this.mode = new JComboBox( this.modearray );
    settings.add( this.mode );

    settings.add( createRightAlignedLabel( "Bits" ) );
    this.bitarray = new String[13];
    for ( int i = 0; i < this.bitarray.length; i++ )
    {
      this.bitarray[i] = new String( "" + ( i + 4 ) );
    }
    this.bits = new JComboBox( this.bitarray );
    this.bits.setSelectedItem( "8" );
    settings.add( this.bits );

    settings.add( createRightAlignedLabel( "Order" ) );
    this.orderarray = new String[2];
    this.orderarray[0] = new String( "MSB first" );
    this.orderarray[1] = new String( "LSB first" );
    this.order = new JComboBox( this.orderarray );
    settings.add( this.order );

    settings.add( createRightAlignedLabel( "Show /CS?" ) );
    this.reportCS = new JCheckBox();
    this.reportCS.setToolTipText( "Whether or not to show /CS transitions in analysis results?" );
    this.reportCS.setSelected( true );
    settings.add( this.reportCS );

    settings.add( createRightAlignedLabel( "Honour /CS?" ) );
    this.honourCS = new JCheckBox();
    this.honourCS.setToolTipText( "Whether or not to use /CS in analysis results?" );
    this.honourCS.setSelected( false );
    settings.add( this.honourCS );

    SpringLayoutUtils.makeEditorGrid( settings, 10, 4 );

    return settings;
  }

  /**
   * Generates an empty HTML page.
   * 
   * @return String with HTML data.
   */
  private String getEmptyHtmlPage()
  {
    final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
    return exporter.toString( new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.LONG );
          return df.format( new Date() );
        }
        return null;
      }
    } );
  }

  /**
   * Initializes this dialog.
   */
  private void initDialog()
  {
    setMinimumSize( new Dimension( 640, 480 ) );

    final JComponent settingsPane = createSettingsPane();
    final JComponent previewPane = createPreviewPane();

    final JPanel contentPane = new JPanel( new GridBagLayout() );
    contentPane.add( settingsPane, new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 2, 0, 2, 0 ), 0, 0 ) );
    contentPane.add( previewPane, new GridBagConstraints( 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 2, 0, 2, 0 ), 0, 0 ) );

    final JComponent buttons = createButtonPane();

    SwingComponentUtils.setupDialogContentPane( this, contentPane, buttons );
  }

  /**
   * generate a HTML page
   * 
   * @param aDataSet
   *          the data set to create the HTML page for, cannot be
   *          <code>null</code>.
   * @return String with HTML data
   */
  private String toHtmlPage( final File aFile, final SPIDataSet aDataSet ) throws IOException
  {
    final int bitCount = Integer.parseInt( ( String )this.bits.getSelectedItem() );
    final int bitAdder = ( bitCount % 4 != 0 ) ? 1 : 0;

    final MacroResolver macroResolver = new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.LONG );
          return df.format( new Date() );
        }
        else if ( "decoded-data".equals( aMacro ) )
        {
          final List<SPIData> decodedData = aDataSet.getData();
          Element tr;

          for ( int i = 0; i < decodedData.size(); i++ )
          {
            final SPIData ds = decodedData.get( i );

            if ( ds.isEvent() )
            {
              String event;
              String bgColor;

              // this is an event
              if ( SPIDataSet.SPI_CS_LOW.equals( ds.getEventName() ) )
              {
                // start condition
                event = ds.getEventName();
                bgColor = "#c0ffc0";
              }
              else if ( SPIDataSet.SPI_CS_HIGH.equals( ds.getEventName() ) )
              {
                // stop condition
                event = ds.getEventName();
                bgColor = "#e0e0e0";
              }
              else
              {
                // unknown event
                event = "UNKNOWN";
                bgColor = "#ff8000";
              }

              tr = aParent.addChild( TR ).addAttribute( "style", "background-color: " + bgColor + ";" );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( aDataSet.getDisplayTime( ds.getStartSampleIndex() ) );
              tr.addChild( TD ).addContent( event );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD ).addContent( event );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD );
            }
            else if ( ds.isData() )
            {
              final int sampleIdx = ds.getStartSampleIndex();

              tr = aParent.addChild( TR );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( aDataSet.getDisplayTime( sampleIdx ) );

              int mosiValue = ds.isMosiData() ? ds.getDataValue() : 0;
              int misoValue = ds.isMisoData() ? ds.getDataValue() : 0;

              // Try to coalesce equal timestamps...
              if ( ( i + 1 ) < decodedData.size() )
              {
                final SPIData nextDS = decodedData.get( i + 1 );
                if ( nextDS.getStartSampleIndex() == sampleIdx )
                {
                  mosiValue = nextDS.isMosiData() ? nextDS.getDataValue() : mosiValue;
                  misoValue = nextDS.isMisoData() ? nextDS.getDataValue() : misoValue;
                  // Make sure to skip this DS in the next iteration...
                  i++;
                }
              }

              // MOSI value first, MISO value next...
              addDataValues( tr, i, sampleIdx, mosiValue );
              addDataValues( tr, i, sampleIdx, misoValue );
            }
          }
        }

        return null;
      }

      /**
       * @param aTableRow
       * @param aIdx
       * @param aSampleIdx
       * @param aValue
       */
      private void addDataValues( final Element aTableRow, final int aIdx, final int aSampleIdx, final int aValue )
      {
        final String mosiDataHex = DisplayUtils.integerToHexString( aValue, bitCount / 4 + bitAdder );
        final String mosiDataBin = DisplayUtils.integerToBinString( aValue, bitCount );
        final String mosiDataDec = String.valueOf( aValue );
        final String mosiDataASCII;
        if ( ( bitCount == 8 ) && Character.isLetterOrDigit( ( char )aValue ) )
        {
          mosiDataASCII = String.valueOf( ( char )aValue );
        }
        else
        {
          mosiDataASCII = "";
        }

        aTableRow.addChild( TD ).addContent( "0x", mosiDataHex );
        aTableRow.addChild( TD ).addContent( "0b", mosiDataBin );
        aTableRow.addChild( TD ).addContent( mosiDataDec );
        aTableRow.addChild( TD ).addContent( mosiDataASCII );
      }
    };

    if ( aFile == null )
    {
      final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
      return exporter.toString( macroResolver );
    }
    else
    {
      final HtmlFileExporter exporter = ( HtmlFileExporter )createHtmlTemplate( ExportUtils.createHtmlExporter( aFile ) );
      exporter.write( macroResolver );
      exporter.close();
    }

    return null;
  }
}
