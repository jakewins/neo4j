package org.neo4j.kernel.impl.procedures.es6;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import org.neo4j.kernel.impl.store.Neo4jTypes;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.ES6Test.exec;
import static org.neo4j.kernel.impl.procedures.es6.ES6Test.record;
import static org.neo4j.kernel.impl.procedures.es6.ES6Test.yields;

@RunWith( Parameterized.class )
public class ES6TypesTest
{
    @Parameterized.Parameters(name="{1}")
    public static List<Object[]> parameters()
    {
        return asList(
            new Object[]{ Neo4jTypes.NTInteger, "1", 1l },
            new Object[]{ Neo4jTypes.NTText, "'hello, world!'", "hello, world!" }
        );
    }

    @Parameterized.Parameter( 0 )
    public Neo4jTypes.AnyType type;

    @Parameterized.Parameter( 1 )
    public String javascriptExpression;

    @Parameterized.Parameter( 2 )
    public Object javaExpression;

    @Test
    public void shouldAcceptTypeAsInput() throws Throwable
    {
        assertThat( exec( procedureSignature( "f" ).in( "arg", type ).out( "out", type ).build(), "yield [arg]", javaExpression ),
                  yields( record( javaExpression ) ));
    }
}
