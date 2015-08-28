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
package org.neo4j.cypher.internal.compiler.v2_3.executionplan

import java.io.PrintWriter
import java.util

import org.neo4j.cypher.internal.compiler.v2_3._
import org.neo4j.cypher.internal.compiler.v2_3.helpers.Eagerly
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription.Arguments.{Planner, Runtime}
import org.neo4j.function.Supplier
import org.neo4j.graphdb.Result.ResultVisitor
import org.neo4j.graphdb._
import org.neo4j.kernel.api.Statement

import scala.collection.Map

trait SuccessfulCloseable {
  def success(): Unit
  def close(): Unit
}

class CompiledExecutionResult(taskCloser: TaskCloser, statement: Statement, compiledCode: GeneratedQueryExecution)
  extends AcceptableExecutionResult(taskCloser, statement) with SuccessfulCloseable  {

  self =>

  import scala.collection.JavaConverters._

  compiledCode.setSuccessfulCloseable(self)

  override def accept[EX <: Exception](visitor: ResultVisitor[EX]): Unit = compiledCode.accept(visitor)

  /*
   * NOTE: This should ony be used for testing, it creates an InternalExecutionResult
   * where you can call both toList and dumpToString
   */
  def toEagerIterableResult(planner: PlannerName, runtime: RuntimeName): InternalExecutionResult = {
    val dumpToStringBuilder = Seq.newBuilder[Map[String, String]]
    val result = new util.ArrayList[util.Map[String, Any]]()
    doInAccept{ (row) =>
      populateResults(result)(row)
      populateDumpToStringResults(dumpToStringBuilder)(row)
    }
    val iterator = result.iterator()
    new CompiledExecutionResult(taskCloser, statement, compiledCode) {

      override def javaColumns: util.List[String] = self.javaColumns

      override def accept[EX <: Exception](visitor: ResultVisitor[EX]): Unit = throw new UnsupportedOperationException

      override def executionPlanDescription(): InternalPlanDescription = self.executionPlanDescription().addArgument(Planner(planner.name)).addArgument(Runtime(runtime.name))

      override def toList = result.asScala.map(m => Eagerly.immutableMapValues(m.asScala, materializeAsScala)).toList

      override def dumpToString(writer: PrintWriter) = formatOutput(writer, columns, dumpToStringBuilder.result(), queryStatistics())

      override def next() = Eagerly.immutableMapValues(iterator.next().asScala, materializeAsScala)

      override def hasNext = iterator.hasNext

      override def javaIterator: ResourceIterator[util.Map[String, Any]] = new WrappingResourceIterator[util.Map[String, Any]] {
        def hasNext = iterator.hasNext
        def next() = iterator.next()
      }
    }

  }

  override def executionPlanDescription(): InternalPlanDescription = {
    if (!taskCloser.isClosed) throw new ProfilerStatisticsNotReadyException
    compiledCode.executionPlanDescription()
  }

  override def executionMode = compiledCode.executionMode()

  override def javaColumns = compiledCode.javaColumns()
}



