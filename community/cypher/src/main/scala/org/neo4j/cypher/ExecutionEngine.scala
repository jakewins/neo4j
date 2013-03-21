/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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

import internal.commands._
import internal.executionplan.ExecutionPlanBuilder
import internal.executionplan.verifiers.{IndexHintVerifier, Verifier}
import internal.LRUCache
import internal.spi.gdsimpl.TransactionBoundQueryContext
import internal.spi.QueryContext
import scala.collection.JavaConverters._
import java.util.{Map => JavaMap}
import org.neo4j.kernel.{ThreadToStatementContextBridge, GraphDatabaseAPI, InternalAbstractGraphDatabase}
import org.neo4j.graphdb.{Transaction, GraphDatabaseService}
import org.neo4j.graphdb.factory.GraphDatabaseSettings
import org.neo4j.kernel.impl.util.StringLogger
import org.neo4j.kernel.api.StatementContext


class ExecutionEngine(graph: GraphDatabaseService, logger: StringLogger = StringLogger.DEV_NULL) {

  checkScalaVersion()

  require(graph != null, "Can't work with a null graph database")

  val parser = createCorrectParser()
  val planBuilder = new ExecutionPlanBuilder(graph)

  private def createCorrectParser() = if (graph.isInstanceOf[InternalAbstractGraphDatabase]) {
    val database = graph.asInstanceOf[InternalAbstractGraphDatabase]
    database.getConfig.get(GraphDatabaseSettings.cypher_parser_version) match {
      case v:String => new CypherParser(v)
      case _ => new CypherParser()
    }
  } else {
    new CypherParser()
  }

  @throws(classOf[SyntaxException])
  def profile(query: String, params: Map[String, Any]): ExecutionResult = {
    logger.debug(query)
    prepare(query).profile(queryContext, params)
  }

  @throws(classOf[SyntaxException])
  def profile(query: String, params: JavaMap[String, Any]): ExecutionResult = profile(query, params.asScala.toMap)

  @throws(classOf[SyntaxException])
  def profile(query: String): ExecutionResult = profile(query, Map[String, Any]())


  @throws(classOf[SyntaxException])
  def execute(query: String): ExecutionResult = execute(query, Map[String, Any]())

  @throws(classOf[SyntaxException])
  def execute(query: String, params: Map[String, Any]): ExecutionResult = {
    logger.debug(query)
    prepare(query).execute(queryContext, params)
  }

  private def queryContext = {
    val tx = graph.beginTx()

    val ctx = graph.asInstanceOf[GraphDatabaseAPI]
      .getDependencyResolver
      .resolveDependency(classOf[ThreadToStatementContextBridge])
      .getCtxForWriting

    new TransactionBoundQueryContext(graph.asInstanceOf[GraphDatabaseAPI], tx, ctx)
  }

  @throws(classOf[SyntaxException])
  def execute(query: String, params: JavaMap[String, Any]): ExecutionResult = execute(query, params.asScala.toMap)

  @throws(classOf[SyntaxException])
  def prepare(query: String): ExecutionPlan =  {
    val parsedQuery: AbstractQuery = parser.parse(query)
    verify(parsedQuery)
    withQueryContext { (ctx:QueryContext) =>
      ctx.getOrCreateFromSchemaState(query, (_) => planBuilder.build(parsedQuery))
    }
  }

  def verify(query: AbstractQuery) {
    for (verifier <- verifiers)
      verifier.verify(query)
  }

  def isPrepared(query : String) : Boolean = withQueryContext {
    (ctx:QueryContext) => ctx.schemaStateContains(query)
  }

  def execute(query: AbstractQuery, params: Map[String, Any]): ExecutionResult =
    planBuilder.build(query).execute(queryContext, params)

  private def withQueryContext[T]( f: QueryContext => T ) : T = {
    val ctx = queryContext
    var success = true
    try {
      f(ctx)
    } catch {
      case t : Throwable =>
        success = false
        throw t
    } finally
    {
      ctx.close(success)
    }
  }

  private def checkScalaVersion() {
    if (util.Properties.versionString.matches("^version 2.9.0")) {
      throw new Error("Cypher can only run with Scala 2.9.0. It looks like the Scala version is: " +
        util.Properties.versionString)
    }
  }

  val verifiers:Seq[Verifier] = Seq(IndexHintVerifier)
}


