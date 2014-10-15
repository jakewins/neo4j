package org.neo4j.server.configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;

import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.logging.ConsoleLogger;
import org.neo4j.server.configuration.validation.Validator;

public class ServerAndDatabasePropertyFileConfigurator

{
    private static final String NEO4J_PROPERTIES_FILENAME = "neo4j.properties";
    // TODO should move NEO4J_PROPERTIES_FILENAME to the setting class?

    private File propertyFileDirectory;
    
    private Map<String, String> properties = null; 
    // Temporarily record the properties both for server and db
    // finally will be put into the config
    
    public ServerAndDatabasePropertyFileConfigurator ()
    {
        this.properties = new HashMap<>();
    }
    
    public Config createConfig( File serverPropertiesFile, ConsoleLogger log )
    {
        Config config = null;       
        
        if ( serverPropertiesFile == null )
        {
            serverPropertiesFile = new File( System.getProperty( ServerConfigurationSettings.neo_server_config_file.name() ) );
            // what is stored in System property
        }
        try
        {
            propertyFileDirectory = serverPropertiesFile.getParentFile();
            
            // TODO so if we have one config file for both server and db, we might merge them together. 
            // but now we assume we have two separate files and no overlap between entries in two files
            loadServerProperties( serverPropertiesFile, log );
            loadDatabaseTuningProperties( serverPropertiesFile, log );
            
            // register all the properties in the config class
            config = new Config( properties );

            //normalizeUris();
            //ensureRelativeUris();

//            if ( v != null )
//            {
//                // TODO 
//                // v.validate( this.configuration(), log );
//                
//            }
        }
        catch ( ConfigurationException ce )
        {
            log.warn( "Invalid configuration", ce );
        }
        return config;
    }
    
    
    private boolean propertyFileDirectoryContainsDBTuningFile()
    {
        File[] neo4jPropertyFiles = propertyFileDirectory.listFiles( new FilenameFilter()
        {

            @Override
            public boolean accept( File dir, String name )
            {
                return name.toLowerCase().equals( NEO4J_PROPERTIES_FILENAME );
            }
        } );
        return neo4jPropertyFiles != null && neo4jPropertyFiles.length == 1;
    }

    private void loadDatabaseTuningProperties( File configFile, ConsoleLogger log ) throws ConfigurationException
    {
        Map<String, String> databaseTuningProperties = null;
        String databaseTuningPropertyFileLocation = properties.get( ServerConfigurationSettings.db_tuning_property_file.name() );

        // Try and find the file automatically
        if ( databaseTuningPropertyFileLocation == null )
        {
            if ( propertyFileDirectoryContainsDBTuningFile() )
            {
                // why the logic here is so hard to understand?
                databaseTuningPropertyFileLocation = new File( propertyFileDirectory, NEO4J_PROPERTIES_FILENAME ).getAbsolutePath();
                log.log( "No database tuning file explicitly set, defaulting to [%s]",
                        databaseTuningPropertyFileLocation );
            }
            else
            {
                log.log(
                        "No database tuning properties (org.neo4j.server.db.tuning.properties) found in [%s], using defaults.",
                        configFile.getPath() );
            }
        }

        // Load the file if we found it
        else //( databaseTuningPropertyFileLocation != null )
        {
            File databaseTuningPropertyFile = new File( databaseTuningPropertyFileLocation );

            if ( !databaseTuningPropertyFile.exists() )
            {
                log.warn( "The specified file for database performance tuning properties [%s] does not exist.",
                        databaseTuningPropertyFileLocation );
            }
            else
            {
                try
                {
                    databaseTuningProperties = MapUtil.load( databaseTuningPropertyFile );
                }
                catch ( IOException e )
                {
                    log.warn( "Unable to load database tuning file: " + e.getMessage() );
                }
            }
        }

        // Default to no user-defined config if no config was found
        if(databaseTuningProperties == null )
        {
            databaseTuningProperties = new HashMap<>();
        }

        // Always override the store dir property
        databaseTuningProperties.put( GraphDatabaseSettings.store_dir.name(),
                properties.get( ServerConfigurationSettings.db_location.name() ) );
        properties.putAll( databaseTuningProperties );
    }

    private void loadServerProperties( File propertiesFile, ConsoleLogger log )
    {
        // TODO Auto-generated method stub
        
    }
    
}
