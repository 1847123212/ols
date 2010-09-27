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
package nl.lxtreme.ols.api.data;


import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;


/**
 * Provides a container for captured data in which the data can be annotated
 * with "any" kind of information, such as cursors, protocol decoding
 * information, and so on.
 * <p>
 * Data files will start with a header containing meta data marked by lines
 * starting with ";". The actual readout values will follow after the header. A
 * value is a logic level transition of one channel. The associated timestamp
 * since sample start (start has timestamp 0) is stored, too after a @
 * character. This is called compressed format. The handling of the data within
 * the class is the same. A value is 32bits long. The value is encoded in hex
 * and each value is followed by a new line.
 */
public final class DataContainer implements CapturedData
{
  // CONSTANTS

  /** The maximum number of cursors that can be set. */
  public static final int MAX_CURSORS = 10;
  /** The maximum number of channels. */
  public static final int MAX_CHANNELS = 32;
  /** The number of channels per block. */
  public static final int CHANNELS_PER_BLOCK = 8;
  /** The maximum number of blocks. */
  public static final int MAX_BLOCKS = MAX_CHANNELS / CHANNELS_PER_BLOCK;

  private static final Logger LOG = Logger.getLogger( DataContainer.class.getName() );

  /** The regular expression used to parse an (OLS-datafile) instruction. */
  private static final Pattern OLS_INSTRUCTION_PATTERN = Pattern.compile( "^;([^:]+):\\s+([^\r\n]+)$" );
  /** The regular expression used to parse an (OLS-datafile) data value. */
  private static final Pattern OLS_DATA_PATTERN = Pattern.compile( "^([0-9a-fA-F]+)@(\\d+)$" );

  // VARIABLES

  /** the actual captured data */
  private volatile CapturedData capturedData;

  /** position of cursors */
  private final long[] cursorPositions;
  /** The labels of each channel. */
  private final String[] channelLabels;
  /** The individual annotations. */
  private final Map<Integer, ChannelAnnotations> annotations;

  /** cursors enabled status */
  private volatile boolean cursorEnabled;

  // CONSTRUCTORS

  /**
   * Creates a new DataContainer instance.
   */
  public DataContainer()
  {
    this.cursorPositions = new long[MAX_CURSORS];
    Arrays.fill( this.cursorPositions, Long.MIN_VALUE );

    this.channelLabels = new String[MAX_CHANNELS];
    Arrays.fill( this.channelLabels, "" );

    this.annotations = new HashMap<Integer, ChannelAnnotations>();
  }

  // METHODS

  /**
   * Adds a channel annotation for the channel with the given index.
   * 
   * @param aChannelIdx
   *          the index of channel to remove all annotations for, >=0 && < 32.
   * @param aStartIdx
   *          the start index;
   * @param aEndIdx
   *          the end index;
   * @param aData
   *          the data.
   */
  public void addChannelAnnotation( final int aChannelIdx, final int aStartIdx, final int aEndIdx, final Object aData )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    ChannelAnnotations annotations = this.annotations.get( Integer.valueOf( aChannelIdx ) );
    if ( annotations == null )
    {
      annotations = new ChannelAnnotations( aChannelIdx );
      this.annotations.put( Integer.valueOf( aChannelIdx ), annotations );
    }
    annotations.addAnnotation( aStartIdx, aEndIdx, aData );
  }

  /**
   * Calculates the time offset
   * 
   * @param time
   *          absolute sample number
   * @return time relative to data
   */
  public long calculateTime( final long aTime )
  {
    if ( this.capturedData.hasTriggerData() )
    {
      return aTime - this.capturedData.getTriggerPosition();
    }

    return aTime;
  }

  /**
   * Clears <em>all</em> channel annotations for the channel with the given
   * index.
   * 
   * @param aChannelIdx
   *          the index of channel to remove all annotations for, >=0 && < 32.
   */
  public void clearChannelAnnotations( final int aChannelIdx )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    this.annotations.remove( Integer.valueOf( aChannelIdx ) );
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getAbsoluteLength()
   */
  @Override
  public long getAbsoluteLength()
  {
    return hasCapturedData() ? this.capturedData.getAbsoluteLength() : NOT_AVAILABLE;
  }

  /**
   * Returns the channel annotations.
   * 
   * @param aChannelIdx
   *          the index of the channel to retrieve the annotations for, >= 0 &&
   *          < 32.
   * @return the channel annotations, can be <code>null</code>.
   */
  public ChannelAnnotation getChannelAnnotation( final int aChannelIdx, final int aTimeIndex )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }

    final ChannelAnnotations channelAnnotations = this.annotations.get( Integer.valueOf( aChannelIdx ) );
    if ( channelAnnotations == null )
    {
      return null;
    }
    return channelAnnotations.getAnnotation( aTimeIndex );
  }

  /**
   * Returns the channel annotations.
   * 
   * @param aChannelIdx
   *          the index of the channel to retrieve the annotations for, >= 0 &&
   *          < 32.
   * @return the channel annotations, can be <code>null</code>.
   */
  public Iterator<ChannelAnnotation> getChannelAnnotations( final int aChannelIdx, final int aStartIdx,
      final int aEndIdx )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }

    final ChannelAnnotations channelAnnotations = this.annotations.get( Integer.valueOf( aChannelIdx ) );
    if ( channelAnnotations == null )
    {
      return Collections.<ChannelAnnotation> emptyList().iterator();
    }
    return channelAnnotations.getAnnotations( aStartIdx, aEndIdx );
  }

  /**
   * Returns the channel label.
   * 
   * @param aChannelIdx
   *          the index of the channel to retrieve the label for, >= 0 && < 32.
   * @return the channel's label, can be <code>null</code>.
   */
  public String getChannelLabel( final int aChannelIdx )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    return this.channelLabels[aChannelIdx];
  }

  /**
   * Returns all channel labels.
   * 
   * @return an array of all channel's label, never <code>null</code>.
   */
  public final String[] getChannelLabels()
  {
    final String[] result = new String[this.channelLabels.length];
    System.arraycopy( this.channelLabels, 0, result, 0, result.length );
    return result;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getChannels()
   */
  @Override
  public int getChannels()
  {
    return hasCapturedData() ? this.capturedData.getChannels() : NOT_AVAILABLE;
  }

  /**
   * Get position of a cursor.
   * 
   * @param aCursorIdx
   *          the index of the cursor to set, should be >= 0 and < 10.
   * @return a cursor position, or Long.MIN_VALUE if not set.
   * @throws IllegalArgumentException
   *           in case an invalid cursor index was given.
   */
  public long getCursorPosition( final int aCursorIdx ) throws IllegalArgumentException
  {
    if ( ( aCursorIdx < 0 ) || ( aCursorIdx > this.cursorPositions.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid cursor index: " + aCursorIdx + "! Should be between 0 and "
          + this.cursorPositions.length );
    }
    return this.cursorPositions[aCursorIdx];
  }

  /**
   * @return the cursorPositions
   */
  public long[] getCursorPositions()
  {
    return this.cursorPositions;
  }

  /**
   * Returns the (absolute) time value for the cursor indicated by the given
   * index.
   * 
   * @param aCursorIdx
   *          the index of the cursor to return as time, should be >= 0 and <
   *          10.
   * @return the time value (in seconds), or -1.0 if the cursor is not
   *         available.
   */
  public Double getCursorTimeValue( final int aCursorIdx )
  {
    long cursorPos = getCursorPosition( aCursorIdx );
    if ( cursorPos > Long.MIN_VALUE )
    {
      return calculateTime( cursorPos ) / ( double )this.capturedData.getSampleRate();
    }
    return null;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getEnabledChannels()
   */
  @Override
  public int getEnabledChannels()
  {
    return hasCapturedData() ? this.capturedData.getEnabledChannels() : NOT_AVAILABLE;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getSampleIndex(long)
   */
  @Override
  public int getSampleIndex( final long aAbs )
  {
    return hasCapturedData() ? this.capturedData.getSampleIndex( aAbs ) : NOT_AVAILABLE;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getSampleRate()
   */
  @Override
  public int getSampleRate()
  {
    return hasCapturedData() ? this.capturedData.getSampleRate() : NOT_AVAILABLE;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getTimestamps()
   */
  @Override
  public long[] getTimestamps()
  {
    return hasCapturedData() ? this.capturedData.getTimestamps() : new long[0];
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getTriggerPosition()
   */
  @Override
  public long getTriggerPosition()
  {
    return hasCapturedData() && hasTriggerData() ? this.capturedData.getTriggerPosition() : CapturedData.NOT_AVAILABLE;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#getValues()
   */
  @Override
  public int[] getValues()
  {
    return hasCapturedData() ? this.capturedData.getValues() : new int[0];
  }

  /**
   * Returns whether any captured data is available.
   * 
   * @return <code>true</code> if there is captured data, <code>false</code>
   *         otherwise.
   */
  public boolean hasCapturedData()
  {
    return this.capturedData != null;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#hasTimingData()
   */
  @Override
  public boolean hasTimingData()
  {
    return hasCapturedData() ? this.capturedData.hasTimingData() : false;
  }

  /**
   * @see nl.lxtreme.ols.api.data.CapturedData#hasTriggerData()
   */
  @Override
  public boolean hasTriggerData()
  {
    return hasCapturedData() ? this.capturedData.hasTriggerData() : false;
  }

  /**
   * Returns whether a channel label is set or not.
   * 
   * @param aChannelIdx
   *          the channel index to check whether its label is set, >= 0 && < 32.
   * @return <code>true</code> if there a non-empty label set for the given
   *         channel index, <code>false</code> otherwise.
   */
  public boolean isChannelLabelSet( final int aChannelIdx )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    final String label = this.channelLabels[aChannelIdx];
    return ( label != null ) && !label.trim().isEmpty();
  }

  /**
   * Returns whether or not the cursor with the given index is set.
   * 
   * @param aCursorIdx
   *          the index of the cursor to check, should be >= 0 and < 10.
   * @return a cursor position, or Long.MIN_VALUE if not set.
   * @return <code>true</code> if the cursor with the given index is set,
   *         <code>false</code> otherwise.
   */
  public boolean isCursorPositionSet( final int aCursorIdx )
  {
    if ( ( aCursorIdx < 0 ) || ( aCursorIdx > this.cursorPositions.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid cursor index: " + aCursorIdx + "! Should be between 0 and "
          + this.cursorPositions.length );
    }
    return this.cursorPositions[aCursorIdx] > Long.MIN_VALUE;
  }

  /**
   * Returns whether or not the cursor data is enabled.
   * 
   * @return <code>true</code> if the cursors are enabled, <code>false</code>
   *         otherwise.
   */
  public boolean isCursorsEnabled()
  {
    return this.cursorEnabled;
  }

  /**
   * Reads the data from a given reader.
   * 
   * @param aReader
   *          the reader to read the data from, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems.
   */
  public void read( final Reader aReader ) throws IOException
  {
    int size = -1, rate = -1, channels = 32, enabledChannels = -1;
    long triggerPos = -1;

    long[] cursorPositions = new long[10];
    Arrays.fill( cursorPositions, Long.MIN_VALUE );

    boolean cursors = false;
    boolean compressed = false;
    long absLen = 0;

    final BufferedReader br = new BufferedReader( aReader );
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.info( "Parsing OLS captured data from stream..." );
    }

    try
    {
      final List<String[]> dataValues = new ArrayList<String[]>();

      String line;
      while ( ( line = br.readLine() ) != null )
      {
        // Determine whether the line is an instruction, or data...
        final Matcher instructionMatcher = OLS_INSTRUCTION_PATTERN.matcher( line );
        final Matcher dataMatcher = OLS_DATA_PATTERN.matcher( line );

        if ( dataMatcher.matches() )
        {
          final String[] dataPair = new String[] { dataMatcher.group( 1 ), dataMatcher.group( 2 ) };
          dataValues.add( dataPair );
        }
        else if ( instructionMatcher.matches() )
        {
          // Ok; found an instruction...
          final String instrKey = instructionMatcher.group( 1 );
          final String instrValue = instructionMatcher.group( 2 );

          if ( "Size".equals( instrKey ) )
          {
            size = Integer.parseInt( instrValue );
          }
          else if ( "Rate".equals( instrKey ) )
          {
            rate = Integer.parseInt( instrValue );
          }
          else if ( "Channels".equals( instrKey ) )
          {
            channels = Integer.parseInt( instrValue );
          }
          else if ( "TriggerPosition".equals( instrKey ) )
          {
            triggerPos = Long.parseLong( instrValue );
          }
          else if ( "EnabledChannels".equals( instrKey ) )
          {
            enabledChannels = Integer.parseInt( instrValue );
          }
          else if ( "CursorEnabled".equals( instrKey ) )
          {
            cursors = Boolean.parseBoolean( instrValue );
          }
          else if ( "Compressed".equals( instrKey ) )
          {
            compressed = Boolean.parseBoolean( instrValue );
          }
          else if ( "AbsoluteLength".equals( instrKey ) )
          {
            absLen = Long.parseLong( instrValue );
          }
          else if ( "CursorA".equals( instrKey ) )
          {
            cursorPositions[0] = Long.parseLong( instrValue );
          }
          else if ( "CursorB".equals( instrKey ) )
          {
            cursorPositions[1] = Long.parseLong( instrValue );
          }
          else if ( instrKey.startsWith( "Cursor" ) )
          {
            final int idx = Integer.parseInt( instrKey.substring( 6 ) );
            final long pos = Long.parseLong( instrValue );
            cursorPositions[idx] = pos;
          }
        }
      }

      long absoluteLength;
      int[] values;
      long[] timestamps;

      if ( dataValues.isEmpty() || ( size < 0 ) )
      {
        throw new IOException( "File does not appear to be a valid datafile!" );
      }

      if ( !compressed )
      {
        throw new IOException(
            "Uncompressed data file found! Please sent this file to the OLS developers for further inspection!" );
      }
      else if ( size != dataValues.size() )
      {
        throw new IOException( "Data size mismatch! Corrupt file encountered!" );
      }
      else
      {
        // new compressed file format
        absoluteLength = absLen;
        values = new int[size];
        timestamps = new long[size];

        try
        {
          for ( int i = 0; i < dataValues.size(); i++ )
          {
            final String[] dataPair = dataValues.get( i );

            values[i] = Integer.parseInt( dataPair[0].substring( 0, 4 ), 16 ) << 16
                | Integer.parseInt( dataPair[0].substring( 4, 8 ), 16 );

            timestamps[i] = Long.parseLong( dataPair[1] );
          }
        }
        catch ( final NumberFormatException exception )
        {
          throw new IOException( "Invalid data encountered." );
        }
      }

      // Create a copy of all read cursor positions...
      System.arraycopy( cursorPositions, 0, this.cursorPositions, 0, cursorPositions.length );
      this.cursorEnabled = cursors;

      // Finally set the captured data, and notify all event listeners...
      setCapturedData( new CapturedDataImpl( values, timestamps, triggerPos, rate, channels, enabledChannels,
          absoluteLength ) );
    }
    finally
    {
      br.close();
    }
  }

  /**
   * Sets the captured data.
   * 
   * @param aCapturedData
   *          the captured data to set, may be <code>null</code>.
   */
  public void setCapturedData( final CapturedData aCapturedData )
  {
    this.capturedData = aCapturedData;
    this.annotations.clear();
  }

  /**
   * @param aChannelIdx
   *          the index of the channel to set the label for, >= 0 && < 32;
   * @param aAnnotations
   */
  public void setChannelAnnotations( final int aChannelIdx, final ChannelAnnotations aAnnotations )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    this.annotations.put( Integer.valueOf( aChannelIdx ), aAnnotations );
  }

  /**
   * Sets the channel label.
   * 
   * @param aChannelIdx
   *          the index of the channel to set the label for, >= 0 && < 32;
   * @param aLabel
   *          the label to set, may be <code>null</code>.
   */
  public void setChannelLabel( final int aChannelIdx, final String aLabel )
  {
    if ( ( aChannelIdx < 0 ) || ( aChannelIdx > this.channelLabels.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid channel index: " + aChannelIdx + "! Should be between 0 and "
          + this.channelLabels.length );
    }
    this.channelLabels[aChannelIdx] = aLabel;
  }

  /**
   * Sets all channel labels directly.
   * 
   * @param aLabels
   *          the array of labels to set, cannot be <code>null</code>.
   */
  public void setChannelLabels( final String[] aLabels )
  {
    if ( aLabels.length != this.channelLabels.length )
    {
      throw new IllegalArgumentException( "Invalid channel labels! Should have exact " + this.channelLabels.length
          + " items!" );
    }
    System.arraycopy( aLabels, 0, this.channelLabels, 0, this.channelLabels.length );
  }

  /**
   * Sets whether or not the cursor data is enabled.
   * 
   * @param aCursorEnabled
   *          <code>true</code> to the enable the cursor data,
   *          <code>false</code> otherwise.
   */
  public void setCursorEnabled( final boolean aCursorEnabled )
  {
    this.cursorEnabled = aCursorEnabled;
  }

  /**
   * Sets a cursor position.
   * 
   * @param aCursorIdx
   *          the index of the cursor to set, should be >= 0 and < 10;
   * @param aCursorPosition
   *          the actual cursor position to set.
   * @throws IllegalArgumentException
   *           in case an invalid cursor index was given.
   */
  public void setCursorPosition( final int aCursorIdx, final long aCursorPosition ) throws IllegalArgumentException
  {
    if ( ( aCursorIdx < 0 ) || ( aCursorIdx > this.cursorPositions.length - 1 ) )
    {
      throw new IllegalArgumentException( "Invalid cursor index! Should be between 0 and "
          + this.cursorPositions.length );
    }
    this.cursorPositions[aCursorIdx] = aCursorPosition;
  }

  /**
   * Writes the data to the given writer.
   * 
   * @param aWriter
   *          the writer to write the data to, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems.
   */
  public void write( final Writer aWriter ) throws IOException
  {
    final BufferedWriter bw = new BufferedWriter( aWriter );

    try
    {
      final int[] values = this.capturedData.getValues();
      final long[] timestamps = this.capturedData.getTimestamps();

      bw.write( ";Size: " );
      bw.write( Integer.toString( values.length ) );
      bw.newLine();

      bw.write( ";Rate: " );
      bw.write( Integer.toString( this.capturedData.getSampleRate() ) );
      bw.newLine();

      bw.write( ";Channels: " );
      bw.write( Integer.toString( this.capturedData.getChannels() ) );
      bw.newLine();

      bw.write( ";EnabledChannels: " );
      bw.write( Integer.toString( this.capturedData.getEnabledChannels() ) );
      bw.newLine();

      if ( this.capturedData.hasTriggerData() )
      {
        bw.write( ";TriggerPosition: " );
        bw.write( Long.toString( this.capturedData.getTriggerPosition() ) );
        bw.newLine();
      }

      bw.write( ";Compressed: " );
      bw.write( Boolean.toString( true ) );
      bw.newLine();

      bw.write( ";AbsoluteLength: " );
      bw.write( Long.toString( this.capturedData.getAbsoluteLength() ) );
      bw.newLine();

      bw.write( ";CursorEnabled: " );
      bw.write( Boolean.toString( this.cursorEnabled ) );
      bw.newLine();

      for ( int i = 0; i < this.cursorPositions.length; i++ )
      {
        bw.write( String.format( ";Cursor%d: ", i ) );
        bw.write( Long.toString( this.cursorPositions[i] ) );
        bw.newLine();
      }
      for ( int i = 0; i < values.length; i++ )
      {
        final String hexVal = Integer.toHexString( values[i] );
        bw.write( "00000000".substring( hexVal.length() ) );
        bw.write( hexVal );
        bw.write( "@" );
        bw.write( Long.toString( timestamps[i] ) );
        bw.newLine();
      }
    }
    finally
    {
      bw.flush();
    }
  }
}
