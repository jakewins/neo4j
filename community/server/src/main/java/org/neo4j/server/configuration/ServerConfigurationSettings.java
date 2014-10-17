/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.Description;
import org.neo4j.server.webadmin.console.ShellSessionCreator;

import static org.neo4j.helpers.Settings.ANY;
import static org.neo4j.helpers.Settings.BOOLEAN;
import static org.neo4j.helpers.Settings.DURATION;
import static org.neo4j.helpers.Settings.FALSE;
import static org.neo4j.helpers.Settings.INTEGER;
import static org.neo4j.helpers.Settings.NO_DEFAULT;
import static org.neo4j.helpers.Settings.PATH;
import static org.neo4j.helpers.Settings.STRING;
import static org.neo4j.helpers.Settings.TRUE;
import static org.neo4j.helpers.Settings.illegalValueMessage;
import static org.neo4j.helpers.Settings.matches;
import static org.neo4j.helpers.Settings.min;
import static org.neo4j.helpers.Settings.port;
import static org.neo4j.helpers.Settings.setting;

// TODO refine the descriptions of each setting
// Warning: Without getting the values from a Config instance, the default values do not really take effect.
@Description("Settings used by the server configuration")
public abstract class ServerConfigurationSettings
{   
    // database configuration
    @Description( "List of custom security rules for Neo4j to use." )
    public static final Setting<String> security_rules = setting( "org.neo4j.server.rest.security_rules", STRING,
            NO_DEFAULT );
    
    @Description( "Path to database tuning configuration file." )
    public static final Setting<File> db_tuning_property_file = setting( "org.neo4j.server.db.tuning.properties",
            PATH, NO_DEFAULT ); 
    // the interesting thing is why we do not have a default value for this path?
    // The default value is set in PropertyFileConfigurator.NEO4J_PROPERTIES_FILENAME?

    @Description( "Path to the database directory." )
    public static final Setting<File> db_location = setting( "org.neo4j.server.database.location", PATH,
            "data/graph.db" );
    
    @Description( "Database operation mode, SINGLE or HA." )
    public static final Setting<String> db_mode = setting( "org.neo4j.server.database.mode",
            STRING, "SINGLE" );
  
    // webserver configuration
    @Description( "Http port for the Neo4j REST API." )
    public static final Setting<Integer> webserver_port = setting( "org.neo4j.server.webserver.port", INTEGER, "7474",
            port );
    
    @Description( "Hostname for the Neo4j REST API" )
    public static final Setting<String> webserver_address = setting( "org.neo4j.server.webserver.address", STRING,
            "localhost", illegalValueMessage( "Must be a valid hostname", matches( ANY ) ) );
    
    @Description( "Number of Neo4j worker threads." )
    public static final Setting<Integer> webserver_max_threads = setting( "org.neo4j.server.webserver.maxthreads",
            INTEGER, NO_DEFAULT, min(1) );
    
    @Description( "If execution time limiting is enabled in the database, this configures the maximum request execution time." )
    public static final Setting<Long> webserver_limit_execution_time = setting(
            "org.neo4j.server.webserver.limit.executiontime", DURATION, NO_DEFAULT );

    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<Boolean> webserver_enable_statistics_collection = setting(
            "org.neo4j.server.webserver.statistics", BOOLEAN, FALSE );

    // paths 
    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<File> rest_api_path = setting( "org.neo4j.server.webadmin.data.uri", PATH,
            "/db/data" /*, basePath(The base path is host:port)*/ );

    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<File> management_api_path = setting( "org.neo4j.server.webadmin.management.uri",
            PATH, "/db/manage" );

    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<File> browser_path = setting( "org.neo4j.server.webadmin.browser.uri",
            PATH, "/browser" );
    // TODO is it okay to assign key "org.neo4j.server.webadmin.browser.uri" to it? I created this key.
    
    @Description("Path to the statistics database file.")
    public static final Setting<File> rrdb_location = setting( "org.neo4j.server.webadmin.rrdb.location",
            PATH, NO_DEFAULT );
    
    // packages
    
    // other settings
    @Description( "Console engines for the legacy webadmin administr" )
    public static final Setting<String> management_console_engines = setting( "org.neo4j.server.manage.console_engines",
            STRING, NO_DEFAULT );
    
    public static List<String> getDefaultManagementConsoleEngines() 
    {
        // TODO for Zhen. Think of a better way to assign the default values, either putting them in one string or
        // adding support for List<String> in setting class
        return new ArrayList<String> () {
            private static final long serialVersionUID = 6621747998288594121L;
          {
              add( new ShellSessionCreator().name() );
          }
        };
    }
    
    @Description( "Comma-separated list of <classname>=<mount point> for unmanaged extensions." )
    public static final Setting<String> third_party_packages = setting( "org.neo4j.server.thirdparty_jaxrs_classes",
            STRING, NO_DEFAULT );

    // security configuration
    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<Boolean> script_sandboxing_enabled = setting(
            "org.neo4j.server.script.sandboxing.enabled", BOOLEAN, TRUE );

    @Description( "Enable HTTPS for the REST API." )
    public static final Setting<Boolean> webserver_https_enabled = setting( "org.neo4j.server.webserver.https.enabled",
            BOOLEAN, FALSE );

    @Description( "HTTPS port for the REST API." )
    public static final Setting<Integer> webserver_https_port = setting( "org.neo4j.server.webserver.https.port",
            INTEGER, "7473", port );

    @Description( "Path to the keystore used to store SSL certificates and keys while the server is running." )
    public static final Setting<File> webserver_keystore_path = setting(
            "org.neo4j.server.webserver.https.keystore.location", PATH, "neo4j-home/ssl/keystore" );
    
    @Description( "Path to the SSL certificate used for HTTPS connections." )
    public static final Setting<File> webserver_https_cert_path = setting(
            "org.neo4j.server.webserver.https.cert.location", PATH, "neo4j-home/ssl/snakeoil.cert" );
    
    @Description( "Path to the SSL key used for HTTPS connections." )
    public static final Setting<File> webserver_https_key_path = setting(
            "org.neo4j.server.webserver.https.key.location", PATH, "neo4j-home/ssl/snakeoil.key" );
    
    @Description( "Enable HTTP request logging." )
    public static final Setting<Boolean> http_logging = setting( "org.neo4j.server.http.log.enabled", BOOLEAN, FALSE );
    
    @Description( "Path to a logback configuration file for HTTP request logging." )
    public static final Setting<File> http_log_config_location = setting( "org.neo4j.server.http.log.config", PATH,
            NO_DEFAULT );

    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<Boolean> wadl_enabled = setting( "unsupported_wadl_generation_enabled", BOOLEAN,
            NO_DEFAULT );

    // TODO: This should not be public, meaning it should go in a separate Settings class that does not get documented, for instance "ServerInternalSettings"
    public static final Setting<Long> startup_timeout = setting( "org.neo4j.server.startup_timeout", DURATION, "120s" );
    
    @Description( "Timeout for idle transactions." )
    public static final Setting<Long> transaction_timeout = setting( "org.neo4j.server.transaction.timeout", DURATION,
            "60s" );
    
    // TODO for Zhen. Add a Config class to access the configurations instead of using Configuration
}
