package org.neo4j.procedure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.proc.FieldName;
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
        try ( Transaction ignore = db.beginTx() )
        {
            Result res1 = db.execute( "CALL org.neo4j.procedure.integrationTestMe" );
            assertThat( res1.next(), equalTo( map( "someVal", 1337L ) ));
            assertFalse( res1.hasNext() );

            Result res2 = db.execute( "CALL org.neo4j.procedure.integrationTestMeToo(42)" );
            assertThat( res2.next(), equalTo( map( "someVal", 42L ) ));
            assertFalse( res2.hasNext() );
        }
    }

    @Test
    public void shouldLoadBeAbleToCallMethodWithParameterMap() throws Throwable
    {
        // Given
        new JarBuilder().createJarFor( plugins.newFile( "myProcedures.jar" ), ClassWithProcedures.class );

        // When
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig( GraphDatabaseSettings.plugin_dir, plugins.getRoot().getAbsolutePath() )
                .newGraphDatabase();

        // Then
        try ( Transaction ignore = db.beginTx() )
        {
            Result res = db.execute( "CALL org.neo4j.procedure.simpleArgument", map( "name", 42L ) );
            assertThat( res.next(), equalTo( map( "someVal", 42L ) ) );
            assertFalse( res.hasNext() );
        }
    }

    @Test
    public void shouldLoadBeAbleToCallProcedureWithGenericArgument() throws Throwable
    {
        // Given
        new JarBuilder().createJarFor( plugins.newFile( "myProcedures.jar" ), ClassWithProcedures.class );

        // When
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig( GraphDatabaseSettings.plugin_dir, plugins.getRoot().getAbsolutePath() )
                .newGraphDatabase();

        // Then
        try ( Transaction ignore = db.beginTx() )
        {
            Result res = db.execute(
                    "CALL org.neo4j.procedure.genericArguments([ ['graphs'], ['are'], ['everywhere']], " +
                    "[ [[1, 2, 3]], [[4, 5]]] )" );
            assertThat( res.next(), equalTo( map( "someVal", 5L ) ) );
            assertFalse( res.hasNext() );
        }
    }

    @Test
    public void shouldLoadBeAbleToCallProcedureWithMapArgument() throws Throwable
    {
        // Given
        new JarBuilder().createJarFor( plugins.newFile( "myProcedures.jar" ), ClassWithProcedures.class );

        // When
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig( GraphDatabaseSettings.plugin_dir, plugins.getRoot().getAbsolutePath() )
                .newGraphDatabase();

        // Then
        try ( Transaction ignore = db.beginTx() )
        {
            Result res = db.execute(
                    "CALL org.neo4j.procedure.mapArgument({foo: 42, bar: 'hello'})" );
            assertThat( res.next(), equalTo( map( "someVal", 2L ) ) );
            assertFalse( res.hasNext() );
        }
    }


    public static class Output
    {
        public int someVal = 1337;

        public Output()
        {
        }

        public Output( int someVal )
        {
            this.someVal = someVal;
        }
    }

    public static class ClassWithProcedures
    {
        @ReadOnlyProcedure
        public Stream<Output> integrationTestMe()
        {
            return Stream.of( new Output() );
        }

        @ReadOnlyProcedure
        public Stream<Output> simpleArgument( @FieldName( "name" ) int someValue )
        {
            return Stream.of( new Output( someValue ) );
        }

        @ReadOnlyProcedure
        public Stream<Output> genericArguments( @FieldName( "stringList" ) List<List<String>> stringList,
                @FieldName( "intList" ) List<List<List<Integer>>> intList )
        {

            return Stream.of( new Output( stringList.size() + intList.size() ) );
        }

        @ReadOnlyProcedure
        public Stream<Output> mapArgument( @FieldName( "map" )Map<String,Object> map )
        {

            return Stream.of( new Output( map.size()) );
        }
    }

}
