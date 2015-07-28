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

  test("createAndCallProcedure") {
    // WHEN
    execute("CREATE READ ONLY PROCEDURE example.myProc(input:Text): (output:Text) USING javascript FROM SOURCE " +
      "\"yield record(input);\n\"")

    // THEN
    assert(execute("CALL example.myProc( 'hello' )").toList === List(Map("line" -> Seq("1","'Aadvark'","0")), Map("line" -> Seq("2","'Babs'")), Map("line" -> Seq("3","'Cash'","1"))))
  }

  test("recursiveCalls") {
    // WHEN
    execute("CREATE (n:Product {uuid:'shoes'})-[:PARENT]->()-[:APPLY]->({pid:'myPromotion', parent:'mom'})")

    execute("CREATE READ ONLY PROCEDURE retail.getChain( id:Integer ) : ( link:Node ) USING cypher FROM SOURCE " +
      "\"MATCH (n)-[:PARENT*0..]->(link) WHERE id(n) = {id} RETURN link\"")

    execute("CREATE READ ONLY PROCEDURE retail.getPromos(id:Text): (id:Text, parent:Text, name:Text) USING javascript FROM SOURCE " +
      "\"for (var product in neo4j.findNodes('Product', 'uuid', id))\n" +
      "{\n" +
      "    for (var row in retail_getChain(product.getId()))\n" +
      "    {\n" +
      "        for(var promotedRel in Iterator(row.link.getRelationships(neo4j.OUTGOING, [type('APPLY'), type('EXCLUDE')])))\n" +
      "        {\n" +
      "            var promotion = promotedRel.getEndNode();\n" +
      "            yield record( promotion.getProperty('pid'),\n" +
      "                    promotion.getProperty('parent'),\n" +
      "                    promotedRel.getType().name());\n" +
      "        }\n" +
      "    }\n" +
      "}\n\"")

    // THEN
    assert(execute("CALL retail.getPromos( 'shoes' )").toList ===
      List(Map("id" -> "myPromotion", "name" -> "APPLY", "parent" -> "mom")))
  }

  implicit class FileHelper(file: File) {
    def deleteAll(): Unit = {
      def deleteFile(dfile: File): Unit = {
        if (dfile.isDirectory)
          dfile.listFiles.foreach {
            f => deleteFile(f)
          }
        dfile.delete
      }
      deleteFile(file)
    }
  }

  private def createDbWithFailedIndex: GraphDatabaseService = {
    new File("target/test-data/impermanent-db").deleteAll()
    var graph = new TestGraphDatabaseFactory().newEmbeddedDatabase("target/test-data/impermanent-db")
    eengine = new ExecutionEngine(graph)
    execute("CREATE INDEX ON :Person(name)")
    execute("create (:Person {name:42})")
    val tx = graph.beginTx()
    try {
      graph.schema().awaitIndexesOnline(3, TimeUnit.SECONDS)
      tx.success()
    } finally {
      tx.close()
    }
    graph.shutdown()

    val stream = new FileOutputStream("target/test-data/impermanent-db/schema/index/lucene/1/failure-message")
    stream.write(65)
    stream.close()

    graph = new TestGraphDatabaseFactory().newEmbeddedDatabase("target/test-data/impermanent-db")
    eengine = new ExecutionEngine(graph)
    graph
  }
}