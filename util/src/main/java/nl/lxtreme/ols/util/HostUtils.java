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


import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;


/**
 * Provides some host/OS specific utilities.
 */
public final class HostUtils
{
  // INNER TYPES

  /**
   * Denotes a callback for some "main events" in an application.
   * <p>
   * This interface is merely used for retrieving the proper system events on
   * Mac OSX. On other operating systems, this interface has no real value.
   * </p>
   */
  public interface ApplicationCallback
  {
    /**
     * Called upon receiving a "about" event from the host operating system.
     * 
     * @return <code>true</code> if the event is being handled,
     *         <code>false</code> (the default) if this event is ignored.
     */
    public boolean handleAbout();

    /**
     * Called upon receiving a "set preferenes" event from the host operating
     * system.
     * 
     * @return <code>true</code> if the event is being handled,
     *         <code>false</code> (the default) if this event is ignored.
     */
    public boolean handlePreferences();

    /**
     * Called upon receiving a quit event from the host operating system.
     * 
     * @return <code>true</code> if the event is being handled,
     *         <code>false</code> (the default) if this event is ignored.
     */
    public boolean handleQuit();
  }

  /**
   * Provides a hack to ensure the system class loader is used at all times when
   * loading UI classes/resources/...
   */
  static final class CLValue implements UIDefaults.ActiveValue
  {
    /**
     * @see javax.swing.UIDefaults.ActiveValue#createValue(javax.swing.UIDefaults)
     */
    public @Override
    ClassLoader createValue( final UIDefaults aDefaults )
    {
      return HostUtils.class.getClassLoader();
    }
  }

  // CONSTRUCTORS

  /**
   * Creates a new HostUtils instance.
   */
  private HostUtils()
  {
    // NO-op
  }

  // METHODS

  /**
   * Returns the "presumed" filename extension (like '.jpg', '.zip') from a
   * given file.
   * 
   * @param aFile
   *          the file to return the extension for, cannot be <code>null</code>.
   * @return the file extension (always in lower case), never <code>null</code>
   *         but can be empty if the given file has <em>no</em> file extension.
   */
  public static final String getFileExtension( final File aFile )
  {
    String ext = "";

    String filename = aFile.getName();
    int idx = filename.lastIndexOf( '.' );

    if ( ( idx > 0 ) && ( idx < filename.length() - 1 ) )
    {
      ext = filename.substring( idx + 1 ).toLowerCase();
    }
    return ext;
  }

  /**
   * Allows the logging properties of the JVM to be set at any moment in time
   * providing the logging configuration in an input-stream.
   * 
   * @param aInputStream
   *          the input stream providing the logging properties, cannot be
   *          <code>null</code>.
   */
  public static final void initLogging( final InputStream aInputStream )
  {
    final LogManager logManager = LogManager.getLogManager();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try
    {
      Thread.currentThread().setContextClassLoader( HostUtils.class.getClassLoader() );
      logManager.readConfiguration( aInputStream );
    }
    catch ( IOException exception )
    {
      Logger.getAnonymousLogger().log( Level.FINE, "Problems to load the logging configuration file!", exception );
    }
    finally
    {
      Thread.currentThread().setContextClassLoader( cl );
    }
  }

  /**
   * Initializes the OS-specific stuff.
   * 
   * @param aApplicationName
   *          the name of the application (when this needs to be passed to the
   *          guest OS);
   * @param aApplicationCallback
   *          the application callback used to report application events on some
   *          platforms (Mac OS), may be <code>null</code>.
   */
  public static final void initOSSpecifics( final String aApplicationName,
      final ApplicationCallback aApplicationCallback )
  {
    initLogging();

    if ( isMacOSX() )
    {
      // Moves the main menu bar to the screen menu bar location...
      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      System.setProperty( "apple.awt.graphics.EnableQ2DX", "true" );
      System.setProperty( "com.apple.mrj.application.apple.menu.about.name", aApplicationName );
      System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );
      System.setProperty( "com.apple.mrj.application.live-resize", "false" );
      System.setProperty( "com.apple.macos.smallTabs", "true" );

      if ( aApplicationCallback != null )
      {
        installApplicationCallback( aApplicationCallback );
      }
    }
    else if ( isUnix() )
    {
      try
      {
        setLookAndFeel( "com.jgoodies.looks.plastic.Plastic3DLookAndFeel" );
      }
      catch ( Exception exception )
      {
        Logger.getAnonymousLogger().log( Level.WARNING, "Failed to set look and feel!", exception );
      }
    }
    else if ( isWindows() )
    {
      try
      {
        setLookAndFeel( "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" );
      }
      catch ( Exception exception )
      {
        Logger.getAnonymousLogger().log( Level.WARNING, "Failed to set look and feel!", exception );
      }
    }
  }

  /**
   * Returns whether the current host's operating system is Mac OS X.
   * 
   * @return <code>true</code> if running on Mac OS X, <code>false</code>
   *         otherwise.
   */
  public static final boolean isMacOSX()
  {
    final String osName = System.getProperty( "os.name" );
    return ( "Mac OS X".equalsIgnoreCase( osName ) );
  }

  /**
   * Returns whether the current host's operating system is Linux or any other
   * UNIX-like operating system.
   * 
   * @return <code>true</code> if running on Linux or any other UNIX system,
   *         <code>false</code> otherwise.
   */
  public static boolean isUnix()
  {
    String osName = System.getProperty( "os.name" ).toLowerCase();
    // linux or unix
    return ( ( osName.indexOf( "nix" ) >= 0 ) || ( osName.indexOf( "nux" ) >= 0 ) );
  }

  /**
   * Returns whether the current host's operating system is Windows.
   * 
   * @return <code>true</code> if running on Windows, <code>false</code>
   *         otherwise.
   */
  public static boolean isWindows()
  {
    final String osName = System.getProperty( "os.name" ).toLowerCase();
    return osName.indexOf( "win" ) >= 0;
  }

  /**
   * Returns whether the host OS needs an explicit exit menu item or not.
   * <p>
   * For example, on Mac OS, you don't need an explicit exit menu, since it is
   * by default provided. On Linux or Windows machines, you do need an explicit
   * exit function.
   * </p>
   * 
   * @return <code>true</code> if this host needs an explicit exit menu item,
   *         <code>false</code> otherwise.
   */
  public static final boolean needsExitMenuItem()
  {
    return !isMacOSX();
  }

  /**
   * Tries to read the properties from a file with a given name.
   * 
   * @param aName
   *          the name of the properties file, excluding <tt>.properties</tt>,
   *          cannot be <code>null</code> or empty. By convention, the name of a
   *          properties file should be in the "reverse package name", e.g.,
   *          "com.foo.bar".
   * @return the read properties (object), or <code>null</code> if no such
   *         property file exists.
   * @throws IllegalArgumentException
   *           in case the given name was <code>null</code> or empty.
   */
  public static final Properties readProperties( final String aName )
  {
    if ( ( aName == null ) || aName.trim().isEmpty() )
    {
      throw new IllegalArgumentException( "Properties name cannot be null or empty!" );
    }

    final File propFile = createPropertiesFile( aName );

    try
    {
      final FileReader propFileReader = new FileReader( propFile );
      final Properties props = new Properties();
      props.load( propFileReader );
      return props;
    }
    catch ( IOException exception )
    {
      Logger.getAnonymousLogger().log( Level.FINE, "Reading properties from '" + propFile + "' failed!", exception );
    }
    // Unable to load any (valid) properties file, return null to indicate
    // this...
    return null;
  }

  /**
   * Sets the filename to end with the given file extension, if this is not
   * already the case.
   * 
   * @param aFile
   *          the file that should get the given file extension, cannot be
   *          <code>null</code>;
   * @param aFileExtension
   *          the new file extension to add to the given file, cannot be
   *          <code>null</code>.
   * @return a file with the given file extension, never <code>null</code>.
   */
  public static final File setFileExtension( final File aFile, final String aFileExtension )
  {
    String filename = aFile.getName();
    // If the filename already has the given file extension, than simply do
    // nothing...
    if ( aFileExtension.trim().isEmpty() || filename.toLowerCase().endsWith( aFileExtension.toLowerCase() ) )
    {
      return aFile;
    }
    return new File( aFile.getPath(), filename + "." + aFileExtension );
  }

  /**
   * Tries to write the properties from a file with a given name.
   * 
   * @param aName
   *          the name of the properties file, excluding <tt>.properties</tt>,
   *          cannot be <code>null</code> or empty. By convention, the name of a
   *          properties file should be in the "reverse package name", e.g.,
   *          "com.foo.bar".
   * @return the read properties (object), or <code>null</code> if no such
   *         property file exists.
   * @throws IllegalArgumentException
   *           in case the given name was <code>null</code> or empty, or the
   *           given properties was <code>null</code>.
   */
  public static final void writeProperties( final String aName, final Properties aProperties )
  {
    if ( ( aName == null ) || aName.trim().isEmpty() )
    {
      throw new IllegalArgumentException( "Properties name cannot be null or empty!" );
    }
    if ( aProperties == null )
    {
      throw new IllegalArgumentException( "Properties object cannot be null!" );
    }

    final File propFile = createPropertiesFile( aName );

    try
    {
      final FileWriter propFileWriter = new FileWriter( propFile );
      aProperties.store( propFileWriter, "Written on " + new Date() );
    }
    catch ( IOException exception )
    {
      Logger.getAnonymousLogger().log( Level.FINE, "Reading properties from '" + propFile + "' failed!", exception );
    }
  }

  /**
   * Creates an OS-specific properties file location.
   * 
   * @param aName
   *          the name of the properties file, excluding <tt>.properties</tt>,
   *          cannot be <code>null</code> or empty. By convention, the name of a
   *          properties file should be in the "reverse package name", e.g.,
   *          "com.foo.bar".
   * @return the file pointing to the OS-specific properties file location,
   *         never <code>null</code>.
   */
  private static final File createPropertiesFile( final String aName )
  {
    final String dirName;
    final String fileName;
    if ( isMacOSX() )
    {
      dirName = System.getProperty( "user.home" ) + "/Library/Preferences/";
      fileName = aName + ".Application";
    }
    else if ( isUnix() )
    {
      dirName = System.getProperty( "user.home" );
      fileName = "." + aName + ".properties";
    }
    else
    {
      dirName = System.getProperty( "user.home" );
      fileName = aName + ".properties";
    }

    final File propFile = new File( dirName, fileName );
    return propFile;
  }

  /**
   * 
   */
  private static void initLogging()
  {
    System.setProperty( "java.util.logging.config.file", "logging.properties" );

    try
    {
      LogManager.getLogManager().readConfiguration();
    }
    catch ( IOException exception )
    {
      Logger.getAnonymousLogger().log( Level.FINE, "Problems to load the logging configuration file!", exception );
    }
  }

  /**
   * @param aApplicationCallback
   */
  private static void installApplicationCallback( final ApplicationCallback aApplicationCallback )
  {
    final String applicationClassName = "com.apple.eawt.Application";
    final String applicationListenerClassName = "com.apple.eawt.ApplicationListener";

    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try
    {
      final Class<?> appClass = classLoader.loadClass( applicationClassName );
      final Class<?> appAdapterClass = classLoader.loadClass( applicationListenerClassName );

      if ( ( appClass != null ) && ( appAdapterClass != null ) )
      {
        final Object proxy = Proxy.newProxyInstance( classLoader, new Class<?>[] { appAdapterClass },
            new InvocationHandler()
            {
              @Override
              public Object invoke( final Object aProxy, final Method aMethod, final Object[] aArgs ) throws Throwable
              {
                final String name = aMethod.getName();
                if ( "handleQuit".equals( name ) )
                {
                  if ( aApplicationCallback.handleQuit() )
                  {
                    handleEventParameter( aArgs );
                  }
                }
                else if ( "handleAbout".equals( name ) )
                {
                  if ( aApplicationCallback.handleAbout() )
                  {
                    handleEventParameter( aArgs );
                  }
                }
                else if ( "handlePreferences".equals( name ) )
                {
                  if ( aApplicationCallback.handlePreferences() )
                  {
                    handleEventParameter( aArgs );
                  }
                }
                return null;
              }

              /**
               * @param aArgs
               */
              private void handleEventParameter( final Object[] aArgs )
              {
                if ( ( aArgs == null ) || ( aArgs.length == 0 ) )
                {
                  return;
                }

                final Object event = aArgs[0];

                final Class<?> eventClass = event.getClass();
                if ( !"com.apple.eawt.ApplicationEvent".equals( eventClass.getName() ) )
                {
                  return;
                }

                try
                {
                  final Method setHandledMethod = eventClass.getMethod( "setHandled", Boolean.TYPE );
                  setHandledMethod.invoke( event, Boolean.TRUE );
                }
                catch ( Exception exception )
                {
                  Logger.getAnonymousLogger().log( Level.ALL, "Event handling in callback failed!", exception );
                }
              }
            } );

        // Call Application#getApplication() ...
        final Method getAppMethod = appClass.getMethod( "getApplication" );
        final Object app = getAppMethod.invoke( null );

        // Call Application#addAboutMenuItem() ...
        final Method addAboutMenuItemMethod = appClass.getMethod( "addAboutMenuItem" );
        addAboutMenuItemMethod.invoke( app );

        // Call Application#addPreferencesMenuItem() ...
        final Method addPrefsMenuItemMethod = appClass.getMethod( "addPreferencesMenuItem" );
        addPrefsMenuItemMethod.invoke( app );

        // Call Application#setEnabledPreferencesMenu(true) ...
        final Method setEnabledPrefsMenuMethod = appClass.getMethod( "setEnabledPreferencesMenu", Boolean.TYPE );
        setEnabledPrefsMenuMethod.invoke( app, Boolean.FALSE ); // XXX set to
        // true to
        // enable
        // preferences!!!

        // Call Application#addApplicationListener(...) ...
        final Method addAppListenerMethod = appClass.getMethod( "addApplicationListener", appAdapterClass );
        addAppListenerMethod.invoke( app, proxy );
      }
    }
    catch ( Exception exception )
    {
      Logger.getAnonymousLogger().log( Level.ALL, "Install application callback failed!", exception );
    }
  }

  /**
   * @param aLookAndFeelClass
   */
  private static final void setLookAndFeel( final String aLookAndFeelClassName )
  {
    final UIDefaults defaults = UIManager.getDefaults();
    // to make sure we always use system class loader
    defaults.put( "ClassLoader", new CLValue() );

    final ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    try
    {
      Thread.currentThread().setContextClassLoader( HostUtils.class.getClassLoader() );
      UIManager.setLookAndFeel( aLookAndFeelClassName );
    }
    catch ( Exception exception )
    {
      Logger.getAnonymousLogger().log( Level.WARNING, "Failed to set look and feel!", exception );
    }
    finally
    {
      Thread.currentThread().setContextClassLoader( oldCL );
    }
  }
}

/* EOF */
