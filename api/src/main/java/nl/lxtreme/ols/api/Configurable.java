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
package nl.lxtreme.ols.api;


import org.osgi.service.prefs.*;


/**
 * This interface defines the methods required to make (UI) object states
 * controllable by the project mechanism.
 * <p>
 * Its methods are called by {@link Project} when storing or loading the project
 * state. A project state is the collection of states of all user configurable
 * items.
 * <p>
 * Note: When defining property values it should be kept in mind that the
 * project configuration file should be understandable and editable by users.
 * Use common sense to determine wheter a particular setting should be part of
 * the project configuration or not. For key naming conventions please look at
 * an actual configuration file.
 * 
 * @version 0.7
 * @author Michael "Mr. Sump" Poppitz
 */
public interface Configurable
{
  /**
   * Reads configuration from given preferences. UI element settings must be
   * modified according to the properties found.
   * 
   * @param aPreferences
   *          the preferences to read configuration from, cannot be
   *          <code>null</code>.
   */
  public void readPreferences( final Preferences aPreferences );

  /**
   * Writes configuration to given preferences. Properties must be set according
   * to the UI element settings.
   * 
   * @param aPreferences
   *          the preferences to write configuration to, cannot be
   *          <code>null</code>.
   */
  public void writePreferences( final Preferences aPreferences );
}
