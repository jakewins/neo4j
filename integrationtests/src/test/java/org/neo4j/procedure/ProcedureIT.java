package org.neo4j.procedure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
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

    @Test
    public void shouldLoadBeAbleToCallProcedureWithNodeReturn() throws Throwable
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
                    "CALL org.neo4j.procedure.node(42)" );
            Node node = (Node) res.next().get( "node" );
            assertThat(node.getId(), equalTo( 42L ));
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

    public static class NodeOutput
    {
        public Node node;

        public NodeOutput()
        {

        }

        void setNode( Node node )
        {
            this.node = node;
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

        @ReadOnlyProcedure
        public Stream<NodeOutput> node( @FieldName( "id" ) long id )
        {
            NodeOutput nodeOutput = new NodeOutput();
            nodeOutput.setNode( nodeFromId( id ) );
            return Stream.of( nodeOutput );
        }
    }

    private static Node nodeFromId( long id )
    {
        return new Node()
        {
            @Override
            public long getId()
            {
                return id;
            }

            @Override
            public void delete()
            {

            }

            @Override
            public Iterable<Relationship> getRelationships()
            {
                return null;
            }

            @Override
            public boolean hasRelationship()
            {
                return false;
            }

            @Override
            public Iterable<Relationship> getRelationships( RelationshipType... types )
            {
                return null;
            }

            @Override
            public Iterable<Relationship> getRelationships( Direction direction, RelationshipType... types )
            {
                return null;
            }

            @Override
            public boolean hasRelationship( RelationshipType... types )
            {
                return false;
            }

            @Override
            public boolean hasRelationship( Direction direction, RelationshipType... types )
            {
                return false;
            }

            @Override
            public Iterable<Relationship> getRelationships( Direction dir )
            {
                return null;
            }

            @Override
            public boolean hasRelationship( Direction dir )
            {
                return false;
            }

            @Override
            public Iterable<Relationship> getRelationships( RelationshipType type, Direction dir )
            {
                return null;
            }

            @Override
            public boolean hasRelationship( RelationshipType type, Direction dir )
            {
                return false;
            }

            @Override
            public Relationship getSingleRelationship( RelationshipType type, Direction dir )
            {
                return null;
            }

            @Override
            public Relationship createRelationshipTo( Node otherNode, RelationshipType type )
            {
                return null;
            }

            @Override
            public Iterable<RelationshipType> getRelationshipTypes()
            {
                return null;
            }

            @Override
            public int getDegree()
            {
                return 0;
            }

            @Override
            public int getDegree( RelationshipType type )
            {
                return 0;
            }

            @Override
            public int getDegree( Direction direction )
            {
                return 0;
            }

            @Override
            public int getDegree( RelationshipType type, Direction direction )
            {
                return 0;
            }

            @Override
            public void addLabel( Label label )
            {

            }

            @Override
            public void removeLabel( Label label )
            {

            }

            @Override
            public boolean hasLabel( Label label )
            {
                return false;
            }

            @Override
            public Iterable<Label> getLabels()
            {
                return null;
            }

            @Override
            public GraphDatabaseService getGraphDatabase()
            {
                return null;
            }

            @Override
            public boolean hasProperty( String key )
            {
                return false;
            }

            @Override
            public Object getProperty( String key )
            {
                return null;
            }

            @Override
            public Object getProperty( String key, Object defaultValue )
            {
                return null;
            }

            @Override
            public void setProperty( String key, Object value )
            {

            }

            @Override
            public Object removeProperty( String key )
            {
                return null;
            }

            @Override
            public Iterable<String> getPropertyKeys()
            {
                return null;
            }

            @Override
            public Map<String,Object> getProperties( String... keys )
            {
                return null;
            }

            @Override
            public Map<String,Object> getAllProperties()
            {
                return null;
            }
        };
    }
}
