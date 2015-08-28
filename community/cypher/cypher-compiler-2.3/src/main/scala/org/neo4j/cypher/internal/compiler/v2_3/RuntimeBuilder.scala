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
package org.neo4j.cypher.internal.compiler.v2_3

import org.neo4j.cypher.internal.compiler.v2_3.CompilationPhaseTracer.CompilationPhase._
import org.neo4j.cypher.internal.compiler.v2_3.CompiledPlanBuilder.createTracer
import org.neo4j.cypher.internal.compiler.v2_3.codegen.profiling.ProfilingTracer
import org.neo4j.cypher.internal.compiler.v2_3.codegen.{QueryExecutionTracer, CodeStructure, CodeGenerator}
import org.neo4j.cypher.internal.compiler.v2_3.executionplan.InterpretedExecutionPlanBuilder.interpretedToExecutionPlan
import org.neo4j.cypher.internal.compiler.v2_3.executionplan._
import org.neo4j.cypher.internal.compiler.v2_3.helpers._
import org.neo4j.cypher.internal.compiler.v2_3.notification.RuntimeUnsupportedNotification
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription.Arguments
import org.neo4j.cypher.internal.compiler.v2_3.planner.execution.{PipeExecutionBuilderContext, PipeExecutionPlanBuilder}
import org.neo4j.cypher.internal.compiler.v2_3.planner.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.compiler.v2_3.planner.{CantCompileQueryException, SemanticTable}
import org.neo4j.cypher.internal.compiler.v2_3.spi.{QueryContext, GraphStatistics, PlanContext}
import org.neo4j.function.Supplier
import org.neo4j.function.Suppliers._
import org.neo4j.graphdb.QueryExecutionType.QueryType
import org.neo4j.helpers.Clock
import org.neo4j.kernel.api.Statement
import org.neo4j.kernel.impl.core.NodeManager
import org.neo4j.kernel.api

object RuntimeBuilder {
  def create(runtimeName: Option[RuntimeName], interpretedProducer: InterpretedPlanBuilder,
            compiledProducer: CompiledPlanBuilder, useErrorsOverWarnings: Boolean) = runtimeName match {
    case None => SilentFallbackRuntimeBuilder(interpretedProducer, compiledProducer)
    case Some(CompiledRuntimeName) if useErrorsOverWarnings => ErrorReportingRuntimeBuilder(compiledProducer)
    case Some(CompiledRuntimeName) => WarningFallbackRuntimeBuilder(interpretedProducer, compiledProducer)
    case Some(InterpretedRuntimeName) => InterpretedRuntimeBuilder(interpretedProducer)
  }
}
trait RuntimeBuilder {

  def apply(logicalPlan: LogicalPlan, pipeBuildContext: PipeExecutionBuilderContext, planContext: PlanContext,
            tracer: CompilationPhaseTracer, semanticTable: SemanticTable,
            monitor: NewRuntimeSuccessRateMonitor, plannerName: PlannerName,
            preparedQuery: PreparedQuery, nodeManager:NodeManager,
            createFingerprintReference:Option[PlanFingerprint]=>PlanFingerprintReference): ExecutionPlan = {
    try {
      compiledProducer(logicalPlan, semanticTable, planContext, monitor, tracer, plannerName, preparedQuery,
                       nodeManager, createFingerprintReference)
    } catch {
      case e: CantCompileQueryException =>
        monitor.unableToHandlePlan(logicalPlan, e)
        fallback(preparedQuery)
        interpretedProducer(logicalPlan, pipeBuildContext, planContext, tracer, preparedQuery, createFingerprintReference )
    }
  }

  def compiledProducer: CompiledPlanBuilder

  def interpretedProducer: InterpretedPlanBuilder

  def fallback(preparedQuery: PreparedQuery): Unit
}

case class SilentFallbackRuntimeBuilder(interpretedProducer: InterpretedPlanBuilder, compiledProducer: CompiledPlanBuilder)
  extends RuntimeBuilder {

  override def fallback(preparedQuery: PreparedQuery): Unit = {}
}

case class WarningFallbackRuntimeBuilder(interpretedProducer: InterpretedPlanBuilder, compiledProducer: CompiledPlanBuilder)
  extends RuntimeBuilder {

  override def fallback(preparedQuery: PreparedQuery): Unit = preparedQuery.notificationLogger
    .log(RuntimeUnsupportedNotification)
}

case class InterpretedRuntimeBuilder(interpretedProducer: InterpretedPlanBuilder) extends RuntimeBuilder {

  override def apply(logicalPlan: LogicalPlan, pipeBuildContext: PipeExecutionBuilderContext, planContext: PlanContext,
                     tracer: CompilationPhaseTracer, semanticTable: SemanticTable,
                     monitor: NewRuntimeSuccessRateMonitor, plannerName: PlannerName,
                     preparedQuery: PreparedQuery, nodeManager:NodeManager,
                     createFingerprintReference:Option[PlanFingerprint]=>PlanFingerprintReference): ExecutionPlan =
    interpretedProducer.apply(logicalPlan, pipeBuildContext, planContext, tracer, preparedQuery, createFingerprintReference)

  override def compiledProducer = throw new InternalException("This should never be called")

  override def fallback(preparedQuery: PreparedQuery) = throw new InternalException("This should never be called")
}

case class ErrorReportingRuntimeBuilder(compiledProducer: CompiledPlanBuilder) extends RuntimeBuilder {

  override def interpretedProducer = throw new InternalException("This should never be called")

  override def fallback(preparedQuery: PreparedQuery) = throw new
      InvalidArgumentException("The given query is not currently supported in the selected runtime")
}

case class InterpretedPlanBuilder(clock: Clock, monitors: Monitors) {

  def apply(logicalPlan: LogicalPlan, pipeBuildContext: PipeExecutionBuilderContext,
            planContext: PlanContext, tracer: CompilationPhaseTracer, inputQuery:PreparedQuery,
            createFingerprintReference:Option[PlanFingerprint]=>PlanFingerprintReference) =
    closing(tracer.beginPhase(PIPE_BUILDING)) {
      interpretedToExecutionPlan( new PipeExecutionPlanBuilder(clock, monitors).build(logicalPlan)(pipeBuildContext, planContext), planContext, inputQuery, createFingerprintReference)
    }
}

case class CompiledPlanBuilder(clock: Clock, structure:CodeStructure[GeneratedQuery]) {

  private val codeGen = new CodeGenerator(structure)

  def apply(logicalPlan: LogicalPlan, semanticTable: SemanticTable, planContext: PlanContext,
            monitor: NewRuntimeSuccessRateMonitor, tracer: CompilationPhaseTracer,
            plannerName: PlannerName, inputQuery:PreparedQuery, nodeManager:NodeManager,
            createFingerprintReference:Option[PlanFingerprint]=>PlanFingerprintReference): ExecutionPlan = {
    monitor.newPlanSeen(logicalPlan)
    closing(tracer.beginPhase(CODE_GENERATION)) {
      val compiled = codeGen.generate(logicalPlan, planContext, clock, semanticTable, plannerName)

      new ExecutionPlan {
        val fingerprint = createFingerprintReference(compiled.fingerprint)

        def isStale(lastTxId: () => Long, statistics: GraphStatistics) = fingerprint.isStale(lastTxId, statistics)

        def run(queryContext: QueryContext, kernelStatement: api.Statement,
                executionMode: ExecutionMode, params: Map[String, Any]): InternalExecutionResult = {
          val taskCloser = new TaskCloser
          taskCloser.addTask(queryContext.close)
          try {
            if (executionMode == ExplainMode) {
              //close all statements
              taskCloser.close(success = true)
              new ExplainExecutionResult(compiled.columns.toList,
                compiled.planDescription, QueryType.READ_ONLY, inputQuery.notificationLogger.notifications)
            } else
              compiled.executionResultBuilder(kernelStatement, nodeManager, executionMode, createTracer(executionMode), params, taskCloser)
          } catch {
            case (t: Throwable) =>
              taskCloser.close(success = false)
              throw t
          }
        }

        def plannerUsed: PlannerName = compiled.plannerUsed

        def isPeriodicCommit: Boolean = compiled.periodicCommit.isDefined

        def runtimeUsed = CompiledRuntimeName
      }
    }
  }
}

object CompiledPlanBuilder {
  type DescriptionProvider = (InternalPlanDescription => (Supplier[InternalPlanDescription], Option[QueryExecutionTracer]))

  def createTracer( mode: ExecutionMode ) : DescriptionProvider = mode match {
    case ProfileMode =>
      val tracer = new ProfilingTracer()
      (description: InternalPlanDescription) => (new Supplier[InternalPlanDescription] {

        override def get(): InternalPlanDescription = description.map {
          plan: InternalPlanDescription =>
            val data = tracer.get(plan.id)
            plan.
              addArgument(Arguments.DbHits(data.dbHits())).
              addArgument(Arguments.Rows(data.rows())).
              addArgument(Arguments.Time(data.time()))
        }
      }, Some(tracer))
    case _ => (description: InternalPlanDescription) => (singleton(description), None)
  }
}
