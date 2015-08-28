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

import java.util

import org.neo4j.cypher.internal.compiler.v2_3.CompiledPlanBuilder.DescriptionProvider
import org.neo4j.cypher.internal.compiler.v2_3.ast.Statement
import org.neo4j.cypher.internal.compiler.v2_3.commands._
import org.neo4j.cypher.internal.compiler.v2_3.executionplan.builders._
import org.neo4j.cypher.internal.compiler.v2_3.pipes._
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription
import org.neo4j.cypher.internal.compiler.v2_3.planner.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.compiler.v2_3.planner.{CantCompileQueryException, CantHandleQueryException}
import org.neo4j.cypher.internal.compiler.v2_3.profiler.Profiler
import org.neo4j.cypher.internal.compiler.v2_3.spi._
import org.neo4j.cypher.internal.compiler.v2_3.symbols.SymbolTable
import org.neo4j.cypher.internal.compiler.v2_3.{ExecutionMode, ProfileMode, _}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.helpers.{Clock}
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.kernel.api.{Statement => KernelStatement}
import org.neo4j.kernel.impl.core.NodeManager


trait RunnablePlan {
  def apply(statement: KernelStatement,
            nodeManager: NodeManager,
            execMode: ExecutionMode,
            descriptionProvider: DescriptionProvider,
            params: Map[String, Any],
            closer: TaskCloser): InternalExecutionResult
}

case class ProcedurePlan(updating: Boolean,
                         planDescription: InternalPlanDescription,
                         columns: Seq[String],
                         executionResultBuilder: RunnablePlan )

case class CompiledPlan(updating: Boolean,
                        periodicCommit: Option[PeriodicCommitInfo] = None,
                        fingerprint: Option[PlanFingerprint] = None,
                        plannerUsed: PlannerName,
                        planDescription: InternalPlanDescription,
                        columns: Seq[String],
                        executionResultBuilder: RunnablePlan )

case class PipeInfo(pipe: Pipe,
                    updating: Boolean,
                    periodicCommit: Option[PeriodicCommitInfo] = None,
                    fingerprint: Option[PlanFingerprint] = None,
                    plannerUsed: PlannerName)

case class PeriodicCommitInfo(size: Option[Long]) {
  def batchRowCount = size.getOrElse(/* defaultSize */ 1000L)
}

trait NewLogicalPlanSuccessRateMonitor {
  def newQuerySeen(queryText: String, ast:Statement)
  def unableToHandleQuery(queryText: String, ast:Statement, origin: CantHandleQueryException)
}

trait NewRuntimeSuccessRateMonitor {
  def newPlanSeen(plan: LogicalPlan)
  def unableToHandlePlan(plan: LogicalPlan, origin: CantCompileQueryException)
}

object ExecutablePlanBuilder {

  def create(plannerName: Option[PlannerName], schemaPlanProducer:ExecutablePlanBuilder,
             rulePlanProducer: ExecutablePlanBuilder, costPlanProducer: ExecutablePlanBuilder,
             planBuilderMonitor: NewLogicalPlanSuccessRateMonitor, useErrorsOverWarnings: Boolean) = plannerName match {
    case None => new SilentFallbackPlanBuilder(Seq(schemaPlanProducer, costPlanProducer, rulePlanProducer), planBuilderMonitor)
    case Some(_) if useErrorsOverWarnings => new ErrorReportingExecutablePlanBuilder(new SilentFallbackPlanBuilder(Seq(schemaPlanProducer, costPlanProducer), planBuilderMonitor))
    case Some(_) => new WarningFallbackPlanBuilder(Seq(schemaPlanProducer, costPlanProducer, rulePlanProducer), planBuilderMonitor)
  }
}

trait ExecutablePlanBuilder {
  def producePlan(inputQuery: PreparedQuery, planContext: PlanContext, tracer: CompilationPhaseTracer = CompilationPhaseTracer.NO_TRACING, createFingerprintReference: (Option[PlanFingerprint]) => PlanFingerprintReference): ExecutionPlan
}

class ExecutionPlanBuilder(fingerprinter:Option[PlanFingerprint]=>PlanFingerprintReference, pipeBuilder: ExecutablePlanBuilder) extends PatternGraphBuilder {
  def build(planContext: PlanContext, inputQuery: PreparedQuery, tracer: CompilationPhaseTracer=CompilationPhaseTracer.NO_TRACING): ExecutionPlan = {
    pipeBuilder.producePlan(inputQuery, planContext, tracer, fingerprinter)
  }
}

/** Exposes a legacy pipe plan as an executable plan */
object InterpretedExecutionPlanBuilder {

  def interpretedToExecutionPlan(pipeInfo: PipeInfo, planContext: PlanContext, inputQuery: PreparedQuery,
                               createFingerprintReference:Option[PlanFingerprint]=>PlanFingerprintReference) = {
    val abstractQuery = inputQuery.abstractQuery
    val PipeInfo(pipe, updating, periodicCommitInfo, fp, planner) = pipeInfo

    val columns = getQueryResultColumns(abstractQuery, pipe.symbols)
    val resultBuilderFactory = new DefaultExecutionResultBuilderFactory(pipeInfo, columns)
    val func = getExecutionPlanFunction(periodicCommitInfo, abstractQuery.getQueryText, updating, resultBuilderFactory, inputQuery.notificationLogger)

    new ExecutionPlan {
      private val fingerprint = createFingerprintReference(fp)

      def run(queryContext: QueryContext, ignored: KernelStatement, planType: ExecutionMode, params: Map[String, Any]) =
        func(queryContext, planType, params)

      def isPeriodicCommit = periodicCommitInfo.isDefined
      def plannerUsed = planner
      def isStale(lastTxId: () => Long, statistics: GraphStatistics) = fingerprint.isStale(lastTxId, statistics)

      def runtimeUsed = InterpretedRuntimeName
    }

  }

  private def getQueryResultColumns(q: AbstractQuery, currentSymbols: SymbolTable): List[String] = q match {
    case in: PeriodicCommitQuery =>
      getQueryResultColumns(in.query, currentSymbols)

    case in: Query =>
      // Find the last query part
      var query = in
      while (query.tail.isDefined) {
        query = query.tail.get
      }

      query.returns.columns.flatMap {
        case "*" => currentSymbols.identifiers.keys
        case x => Seq(x)
      }

    case union: Union =>
      getQueryResultColumns(union.queries.head, currentSymbols)

    case _ =>
      List.empty
  }

  private def getExecutionPlanFunction(periodicCommit: Option[PeriodicCommitInfo],
                                       queryId: AnyRef,
                                       updating: Boolean,
                                       resultBuilderFactory: ExecutionResultBuilderFactory,
                                       notificationLogger: InternalNotificationLogger):
  (QueryContext, ExecutionMode, Map[String, Any]) => InternalExecutionResult =
    (queryContext: QueryContext, planType: ExecutionMode, params: Map[String, Any]) => {
      val builder = resultBuilderFactory.create()

      val profiling = planType == ProfileMode
      val builderContext = if (updating || profiling) new UpdateCountingQueryContext(queryContext) else queryContext
      builder.setQueryContext(builderContext)

      if (periodicCommit.isDefined) {
        if (!builderContext.isTopLevelTx)
          throw new PeriodicCommitInOpenTransactionException()
        builder.setLoadCsvPeriodicCommitObserver(periodicCommit.get.batchRowCount)
      }

      if (profiling)
        builder.setPipeDecorator(new Profiler())

      builder.build(queryId, planType, params, notificationLogger)
    }
}

