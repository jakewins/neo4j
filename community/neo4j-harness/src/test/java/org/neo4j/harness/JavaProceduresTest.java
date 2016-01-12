package org.neo4j.harness;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import java.util.stream.Stream;

import org.neo4j.proc.ReadOnlyProcedure;
import org.neo4j.test.SuppressOutput;
import org.neo4j.test.TargetDirectory;
import org.neo4j.test.server.HTTP;

import static junit.framework.TestCase.assertEquals;
import static org.neo4j.test.server.HTTP.RawPayload.quotedJson;

public class JavaProceduresTest
{
    @Rule
    public TargetDirectory.TestDirectory testDir = TargetDirectory.testDirForTest( InProcessBuilderTest.class );

    @Rule public SuppressOutput suppressOutput = SuppressOutput.suppressAll();

    public static class MyProcedures
    {
        public static class OutputRecord
        {
            public int someNumber = 1337;
        }

        @ReadOnlyProcedure
        public Stream<OutputRecord> myProc()
        {
            return Stream.of( new OutputRecord() );
        }
    }

    @Test
    public void shouldLaunchWithDeclaredProcedures() throws Exception
    {
        // When
        try(ServerControls server = TestServerBuilders.newInProcessBuilder().withProcedure( MyProcedures.class ).newServer())
        {
            // Then
            HTTP.Response response = HTTP.POST( server.httpURI().resolve( "db/data/transaction/commit" ).toString(),
                    quotedJson( "{ 'statements': [ { 'statement': 'CALL org.neo4j.harness.myProc' } ] }" ) );

            JsonNode result = response.get( "results" ).get( 0 );
            assertEquals( "someNumber", result.get( "columns" ).get( 0 ).asText() );
            assertEquals( 1337, result.get( "data" ).get( 0 ).get( "row" ).get( 0 ).asInt() );
        }
    }
}
