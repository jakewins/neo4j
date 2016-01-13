package org.neo4j.procedure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.proc.JarBuilder;
import org.neo4j.proc.ReadOnlyProcedure;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.neo4j.helpers.collection.MapUtil.map;

public class ProcedureIT
{
    @Rule
    public TemporaryFolder plugins = new TemporaryFolder();

    @Test
    public void shouldLoadProcedureFromPluginDirectory() throws Throwable
    {
        // Given
        new JarBuilder().createJarFor( plugins.newFile( "myProcedures.jar" ), ClassWithProcedures.class );

        // When
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig( GraphDatabaseSettings.plugin_dir, plugins.getRoot().getAbsolutePath() )
                .newGraphDatabase();

        // Then
        try( Transaction tx = db.beginTx() )
        {
            Result res = db.execute( "CALL org.neo4j.procedure.integrationTestMe" );
            assertThat( res.next(), equalTo( map( "someVal", 1337L ) ));
            assertFalse( res.hasNext() );
        }
    }

    public static class Output
    {
        public int someVal = 1337;
    }

    public static class ClassWithProcedures
    {
        @ReadOnlyProcedure
        public Stream<Output> integrationTestMe()
        {
            return Stream.of( new Output() );
        }
    }
}
