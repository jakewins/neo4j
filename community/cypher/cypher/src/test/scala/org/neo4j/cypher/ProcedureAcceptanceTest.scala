/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.cypher

import org.neo4j.kernel.api.exceptions.schema.{NoSuchIndexException, DropIndexFailureException}
import java.util.concurrent.TimeUnit
import java.io.{FileOutputStream, File}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.test.TestGraphDatabaseFactory

class ProcedureAcceptanceTest extends ExecutionEngineFunSuite with QueryStatisticsTestSupport {

  test("create procedure") {
    // WHEN
    val res = execute(
      """CREATE READ ONLY PROCEDURE example.myProc(input:Text): (output:Text)
      USING javascript FROM SOURCE "emit(input);"
      """)

    // THEN
    assertStats( res, procedureAdded = 1 )
  }
  
  test("call procedure") {
    // GIVEN
    execute(
      """CREATE READ ONLY PROCEDURE example.myProc(input:Text): (output:Text)
      USING javascript FROM SOURCE "emit(input);"
      """)

    // WHEN
    execute("CALL example.myProc( 'hello' )").toList should equal (List(Map("output" -> "hello")))
  }

}