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

import org.neo4j.cypher.internal.compiler.v2_3.{InvalidArgumentException, CompilationPhaseTracer, PreparedQuery}
import org.neo4j.cypher.internal.compiler.v2_3.ast._
import org.neo4j.cypher.internal.compiler.v2_3.notification.PlannerUnsupportedNotification
import org.neo4j.cypher.internal.compiler.v2_3.planner.CantHandleQueryException
import org.neo4j.cypher.internal.compiler.v2_3.spi.PlanContext

/**
 * Given a prioritized list of builders, returns the output of the first one that is capable of handling the query.
 */
trait FallbackBuilder extends ExecutablePlanBuilder {

  def producePlan(inputQuery: PreparedQuery, planContext: PlanContext,
                  tracer: CompilationPhaseTracer): Either[CompiledPlan, PipeInfo] = {
    val queryText = inputQuery.queryText
    val statement = inputQuery.statement

    for( builder <- builders ) try {
      monitor.newQuerySeen(queryText, statement)
      return builder.producePlan(inputQuery, planContext, tracer)
    } catch {
      case e: CantHandleQueryException =>
        monitor.unableToHandleQuery(queryText, statement, e)
        warn(inputQuery)
    }

    throw new CantHandleQueryException( "No planner available to handle input query." )
  }

  def builders: Seq[ExecutablePlanBuilder]

  def monitor: NewLogicalPlanSuccessRateMonitor

  def warn(preparedQuery: PreparedQuery): Unit
}

case class SilentFallbackPlanBuilder(builders: Seq[ExecutablePlanBuilder],
                                     monitor: NewLogicalPlanSuccessRateMonitor) extends FallbackBuilder {

  override def warn(preparedQuery: PreparedQuery): Unit = {}
}

case class WarningFallbackPlanBuilder(builders: Seq[ExecutablePlanBuilder],
                                      monitor: NewLogicalPlanSuccessRateMonitor) extends FallbackBuilder {

  override def warn(preparedQuery: PreparedQuery): Unit = preparedQuery.notificationLogger
    .log(PlannerUnsupportedNotification)
}