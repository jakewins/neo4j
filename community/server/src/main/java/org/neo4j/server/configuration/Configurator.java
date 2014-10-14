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

import static org.neo4j.helpers.Settings.ANY;
import static org.neo4j.helpers.Settings.INTEGER;
import static org.neo4j.helpers.Settings.DURATION;
import static org.neo4j.helpers.Settings.STRING;
import static org.neo4j.helpers.Settings.BOOLEAN;
import static org.neo4j.helpers.Settings.NO_DEFAULT;
import static org.neo4j.helpers.Settings.FALSE;
import static org.neo4j.helpers.Settings.TRUE;
import static org.neo4j.helpers.Settings.PATH;
import static org.neo4j.helpers.Settings.port;
import static org.neo4j.helpers.Settings.min;
import static org.neo4j.helpers.Settings.illegalValueMessage;
import static org.neo4j.helpers.Settings.matches;
import static org.neo4j.helpers.Settings.setting;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.Description;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.info.DiagnosticsExtractor;
import org.neo4j.kernel.info.DiagnosticsPhase;
import org.neo4j.server.webadmin.console.ShellSessionCreator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

//TODO refine the descriptions of each setting
// Warning: Without getting the values from a Config instance, the default values do not really take effect.
@Description("Settings used by the server configuration")
public abstract class Configurator
{   
    // database configuration
    @Description( "Keeps record of what security rules are applied." )
    public static final Setting<String> security_rules = setting( "org.neo4j.server.rest.security_rules", STRING,
            NO_DEFAULT );
    
    @Description( "???" )//TODO well, what is this?
    public static final Setting<File> db_tuning_property_file = setting( "org.neo4j.server.db.tuning.properties",
            PATH, NO_DEFAULT );
    
    @Description( "The location where the configuration file is stored." )
    public static final Setting<File> neo_server_config_file = setting( "org.neo4j.server.propertie", PATH,
            File.separator + "etc" + File.separator + "neo" );
    // TODO rename this? Every other name starts with "db_"? is this one also related to db? shall we change the name of this variable?
    
    @Description( "The location where the 'db' database file is stored." )
    public static final Setting<File> db_location = setting( "org.neo4j.server.database.location", PATH,
            "data/graph.db" );
    
    @Description( "Defines the operation mode of the 'db' database - single or HA." )
    public static final Setting<String> db_mode = setting( "org.neo4j.server.database.mode",
            STRING, "SINGLE" );
  
    // webserver configuration
    @Description( "The port that the webserver is bound." )
    public static final Setting<Integer> webserver_port = setting( "org.neo4j.server.webserver.port", INTEGER, "7474",
            port );
    
    @Description( "The host that the webserver is bound." )
    public static final Setting<String> webserver_address = setting( "org.neo4j.server.webserver.address", STRING,
            "localhost", illegalValueMessage( "Must be a valid hostname", matches( ANY ) ) );
    
    @Description( "Maximum count of threads that the webserver could create. By default the value is set based on the "
            + "processor core count of the local machine." )    
    public static final Setting<Integer> webserver_max_threads = setting( "org.neo4j.server.webserver.maxthreads",
            INTEGER, NO_DEFAULT, min(1) );
    
    @Description( "Time limitation for an execution to time out if no response is received within the specified time period." )
    public static final Setting<Long> webserver_limit_execution_time = setting(
            "org.neo4j.server.webserver.limit.executiontime", DURATION, NO_DEFAULT );
    
    @Description( "Whether to enable statistics mode or not." )
    public static final Setting<Boolean> webserver_enable_statistics_collection = setting(
            "org.neo4j.server.webserver.statistics", BOOLEAN, FALSE );

    // paths 
    // TODO might assign the default basepath check into the following paths. this might be done by Zhen latter
    @Description( "The path to where the data are stored on the webserver." )
    public static final Setting<File> rest_api_path = setting( "org.neo4j.server.webadmin.data.uri", PATH,
            "/db/data" /*, basePath(The base path is host:port)*/ );
    
    @Description("The path to where the management is stored on the webserver.")//TODO well, it is the database itself or??
    public static final Setting<File> management_api_path = setting( "org.neo4j.server.webadmin.management.uri",
            PATH, "/db/manage" );
    
    @Description("The path where the browser is stored on the webserver.")
    public static final Setting<File> browser_path = setting( "org.neo4j.server.webadmin.browser.uri",
            PATH, "/browser" );//TODO is it okay to assign key "org.neo4j.server.webadmin.browser.uri" to it? I made up this key.
    
    @Description("The path where the rrdb is stored on the webserver.")//TODO what is a rrdb? The same with db_location?
    public static final Setting<File> rrdb_location = setting( "org.neo4j.server.webadmin.rrdb.location",
            PATH, NO_DEFAULT );
    
    // packages
    //TODO The following string are never used by anyone, are they only constant values?
//  String REST_API_PACKAGE = "org.neo4j.server.rest.web";
//  String DISCOVERY_API_PACKAGE = "org.neo4j.server.rest.discovery";
//  String MANAGEMENT_API_PACKAGE = "org.neo4j.server.webadmin.rest";
    public static final Setting<String> rest_api_package = setting( "org.neo4j.server.rest.web",
            STRING, NO_DEFAULT );  
    public static final Setting<String> discovery_api_package = setting( "org.neo4j.server.rest.discovery",
            STRING, NO_DEFAULT );
    public static final Setting<String> management_api_package = setting( "org.neo4j.server.webadmin.rest",
            STRING, NO_DEFAULT );
    
    // other settings
    @Description( "The console engines that are used." )
    public static final Setting<String> management_console_engines = setting( "org.neo4j.server.manage.console_engines",
            STRING, NO_DEFAULT );
    
    public static List<String> getDefaultManagementConsoleEngines() 
    {
        // TODO think of a better way to assign the default values, either putting them in one string or
        // adding support for List<String> in setting class, will be done by Zhen
        return new ArrayList<String> () {
            private static final long serialVersionUID = 6621747998288594121L;
          {
              add( new ShellSessionCreator().name() );
          }
        };
    }
    
    @Description( "The place to specify third party packages." )
    public static final Setting<String> third_party_packages = setting( "org.neo4j.server.thirdparty_jaxrs_classes",
            STRING, NO_DEFAULT );

    // security configuration
    @Description( "Whether script sandboxing is enabled or not." )
    public static final Setting<Boolean> script_sandboxing_enabled = setting(
            "org.neo4j.server.script.sandboxing.enabled", BOOLEAN, TRUE );
    
    @Description( "Whether https is enabled or not." )
    public static final Setting<Boolean> webserver_https_enabled = setting( "org.neo4j.server.webserver.https.enabled",
            BOOLEAN, FALSE );
    
    @Description( "The port that the https webserver is bound." )
    public static final Setting<Integer> webserver_https_port = setting( "org.neo4j.server.webserver.https.port",
            INTEGER, "7473", port );
    
    @Description( "Specify where the keystore is saved on the webserver." )
    public static final Setting<File> webserver_keystore_path = setting(
            "org.neo4j.server.webserver.https.keystore.location", PATH, "neo4j-home/ssl/keystore" );
    
    @Description( "Specify where the certificates used for https connections are saved on the webserver." )
    public static final Setting<File> webserver_https_cert_path = setting(
            "org.neo4j.server.webserver.https.cert.location", PATH, "neo4j-home/ssl/snakeoil.cert" );
    
    @Description( "Specify where the keys used for https connections are saved on the webserver." )
    public static final Setting<File> webserver_https_key_path = setting(
            "org.neo4j.server.webserver.https.key.location", PATH, "neo4j-home/ssl/snakeoil.key" );
    
    @Description( "Whether logging is enabled or not." )
    public static final Setting<Boolean> http_logging = setting( "org.neo4j.server.http.log.enabled", BOOLEAN, FALSE );
    
    @Description( "Specify where log configuration is saved on the webserver." )
    public static final Setting<File> http_log_config_location = setting( "org.neo4j.server.http.log.config", PATH,
            NO_DEFAULT );
    
    @Description( "Whether WADL is enabled or not." )
    public static final Setting<Boolean> wadl_enabled = setting( "unsupported_wadl_generation_enabled", BOOLEAN,
            NO_DEFAULT );

    @Description( "Time limitation for server start up to time out if no response is received within the specified time period." )
    public static final Setting<Long> startup_timeout = setting( "org.neo4j.server.startup_timeout", DURATION, "120s" );
    
    @Description( "Time limitation for a transaction to time out if no response is received within the specified time period." )
    public static final Setting<Long> transaction_timeout = setting( "org.neo4j.server.transaction.timeout", DURATION,
            "60s" );
    
    //TODO add a Config class to access the configurations instead of using Configuration, will be done by Zhen
    
    public abstract Configuration configuration();

    public abstract Map<String, String> getDatabaseTuningProperties();

    @Deprecated
    public abstract List<ThirdPartyJaxRsPackage> getThirdpartyJaxRsClasses();

    public abstract List<ThirdPartyJaxRsPackage> getThirdpartyJaxRsPackages();

    public static DiagnosticsExtractor<Configurator> DIAGNOSTICS = new DiagnosticsExtractor<Configurator>()
    {
        @Override
        public void dumpDiagnostics( final Configurator source, DiagnosticsPhase phase, StringLogger log )
        {
            if ( phase.isInitialization() || phase.isExplicitlyRequested() )
            {
                final Configuration config = source.configuration();
                log.logLongMessage( "Server configuration:", new PrefetchingIterator<String>()
                {
                    final Iterator<?> keys = config.getKeys();

                    @Override
                    protected String fetchNextOrNull()
                    {
                        while ( keys.hasNext() )
                        {
                            Object key = keys.next();
                            if ( key instanceof String )
                            {
                                return key + " = " + config.getProperty( (String) key );
                            }
                        }
                        return null;
                    }
                }, true );
            }
        }

        @Override
        public String toString()
        {
            return Configurator.class.getName();
        }
    };

    public static abstract class Adapter extends Configurator
    {
        @Override
        public List<ThirdPartyJaxRsPackage> getThirdpartyJaxRsClasses()
        {
            return getThirdpartyJaxRsPackages();
        }

        @Override
        public List<ThirdPartyJaxRsPackage> getThirdpartyJaxRsPackages()
        {
            return emptyList();
        }

        @Override
        public Map<String, String> getDatabaseTuningProperties()
        {
            return emptyMap();
        }

        @Override
        public Configuration configuration()
        {
            return new MapConfiguration( Collections.<String, String>emptyMap() );
        }
    }

    public static final Configurator EMPTY = new Configurator.Adapter()
    {
    };

}
