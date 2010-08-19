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
package nl.lxtreme.ols.util;


import java.util.logging.*;


/**
 * 
 */
public final class DisplayUtils
{
  // CONSTANTS

  /**
   * Constant used to determine whether we should show "0.000 s".
   */
  private static final double ZERO_TIME_THRESHOLD = 1.0e-16;

  private static final Logger LOG = Logger.getLogger( DisplayUtils.class.getName() );

  // CONSTRUCTORS

  /**
   * Creates a new DisplayUtils instance (never used).
   */
  private DisplayUtils()
  {
    // NO-op
  }

  // METHODS

  /**
   * Converts a given frequency (in Hertz, Hz) to something more readable for
   * the user, like "10.0 kHz".
   * 
   * @param aFrequency
   *          the frequency (in Hz) to convert to a display value.
   * @return the display representation of the given frequency, never
   *         <code>null</code>.
   */
  public static String displayFrequency( final double aFrequency )
  {
    final String[] unitStrs = { "Hz", "kHz", "MHz", "GHz", "THz" };
    final double[] unitVals = { 1.0, 1.0e3, 1.0e6, 1.0e9, 1.0e12 };

    int i = unitVals.length - 1;
    for ( ; i >= 0; i-- )
    {
      if ( aFrequency >= unitVals[i] )
      {
        break;
      }
    }
    i = Math.max( i, 0 );

    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "aFreq = " + aFrequency + " -> " + unitVals[i] + " " + unitStrs[i] );
    }

    return String.format( "%.3f %s", aFrequency / unitVals[i], unitStrs[i] );
  }

  /**
   * Converts a given time first by scaling it down with a given scale, and
   * returns it as something readable for the user.
   * 
   * @param aTime
   *          the time to convert to a given display value.
   * @param aScale
   *          the scale factor to apply before converting the given time to a
   *          display representation.
   * @return the display representation of the given (scaled) time, never
   *         <code>null</code>.
   */
  public static String displayScaledTime( final double aTime, final double aScale )
  {
    return displayTime( aTime / aScale );
  }

  /**
   * Converts a given time (in seconds) to something more readable for the user,
   * like "1.000 ms".
   * 
   * @param aTime
   *          the time (in seconds) to convert to a given display value.
   * @return the display representation of the given time, never
   *         <code>null</code>.
   */
  public static String displayTime( final double aTime )
  {
    final String[] unitStrs = { "s", "ms", "\u03BCs", "ns", "ps" }; // \u03BC ==
    // Greek mu
    // character
    final double[] unitVals = { 1.0, 1.0e-3, 1.0e-6, 1.0e-9, 1.0e-12 };

    double absTime = Math.abs( aTime );

    int i = 0;
    if ( absTime > ZERO_TIME_THRESHOLD )
    {
      for ( ; i < unitVals.length; i++ )
      {
        if ( absTime >= unitVals[i] )
        {
          break;
        }
      }
      i = Math.min( i, unitVals.length - 1 );
    }

    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "aTime = " + aTime + " -> " + unitVals[i] + " " + unitStrs[i] );
    }

    return String.format( "%.3f %s", aTime / unitVals[i], unitStrs[i] );
  }

  /**
   * Returns the ordinal representation (in English) of the given value.
   * 
   * @param aValue
   *          the value to get the ordinal value for, >= 0 && < 40.
   * @return a ordinal number representation of the given value, like "1st".
   */
  public static String getOrdinalNumber( final int aValue )
  {
    String suffix = "";
    if ( ( aValue == 1 ) || ( aValue == 21 ) || ( aValue == 31 ) )
    {
      suffix = "st";
    }
    else if ( ( aValue == 2 ) || ( aValue == 22 ) )
    {
      suffix = "nd";
    }
    else if ( ( aValue == 3 ) || ( aValue == 23 ) )
    {
      suffix = "rd";
    }
    else if ( ( aValue >= 0 ) && ( aValue < 40 ) )
    {
      suffix = "th";
    }
    return String.format( "%d%s", aValue, suffix );
  }

  /**
   * converts an integer to a bin string with leading zeros
   * 
   * @param aValue
   *          integer value for conversion
   * @param aFieldWidth
   *          number of charakters in field
   * @return a nice string
   */
  public static String integerToBinString( final int aValue, final int aFieldWidth )
  {
    // first build a mask to cut off the signed extension
    final int mask = ( int )Math.pow( 2.0, aFieldWidth ) - 1;

    StringBuilder sb = new StringBuilder( Integer.toBinaryString( aValue & mask ) );

    int numberOfLeadingZeros = aFieldWidth - sb.length();
    if ( numberOfLeadingZeros < 0 )
    {
      numberOfLeadingZeros = 0;
    }
    if ( numberOfLeadingZeros > aFieldWidth )
    {
      numberOfLeadingZeros = aFieldWidth;
    }

    if ( numberOfLeadingZeros > 0 )
    {
      for ( ; numberOfLeadingZeros > 0; numberOfLeadingZeros-- )
      {
        sb.insert( 0, '0' );
      }
    }

    return sb.toString();
  }

  /**
   * converts an integer to a hex string with leading zeros
   * 
   * @param aValue
   *          integer value for conversion
   * @param aFieldWidth
   *          number of charakters in field
   * @return a nice string
   */
  public static String integerToHexString( final int aValue, final int aFieldWidth )
  {
    // first build a mask to cut off the signed extension
    final int mask = ( int )Math.pow( 16.0, aFieldWidth ) - 1;

    String str = Integer.toHexString( aValue & mask );
    int numberOfLeadingZeros = aFieldWidth - str.length();
    if ( numberOfLeadingZeros < 0 )
    {
      numberOfLeadingZeros = 0;
    }
    if ( numberOfLeadingZeros > aFieldWidth )
    {
      numberOfLeadingZeros = aFieldWidth;
    }
    char zeros[] = new char[numberOfLeadingZeros];
    for ( int i = 0; i < zeros.length; i++ )
    {
      zeros[i] = '0';
    }
    String ldz = new String( zeros );
    return ( new String( ldz + str ) );
  }

  /**
   * Returns whether the given string is actually empty, meaning
   * <code>null</code> or an empty string.
   * 
   * @param aValue
   *          the string value to check for "emptyness", can be
   *          <code>null</code>.
   * @return <code>true</code> if the given string is empty, <code>false</code>
   *         otherwise.
   */
  public static boolean isEmpty( final String aValue )
  {
    return ( aValue == null ) || aValue.trim().isEmpty();
  }

}

/* EOF */
