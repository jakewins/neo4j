package org.neo4j.cypher.internal.compiler.v2_3.executionplan

import java.util
import java.util.Collections

import org.neo4j.cypher.internal.compiler.v2_3._
import org.neo4j.cypher.internal.compiler.v2_3.ast.rewriters.reattachAliasedExpressions
import org.neo4j.cypher.internal.compiler.v2_3.commands.{CallProcedure, CreateProcedure}
import org.neo4j.cypher.internal.compiler.v2_3.pipes.{NoExternalResource, NullPipeDecorator, QueryState}
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.{Id, InternalPlanDescription, NoChildren, PlanDescriptionImpl}
import org.neo4j.cypher.internal.compiler.v2_3.spi.{GraphStatistics, PlanContext, QueryContext, UpdateCountingQueryContext}
import org.neo4j.cypher.internal.compiler.v2_3.tracing.rewriters.RewriterStepSequencer
import org.neo4j.graphdb.Result.ResultVisitor
import org.neo4j.kernel.api.Statement

import scala.collection.JavaConverters._


class ProcedurePlanBuilder(rewriterSequencer: (String) => RewriterStepSequencer) extends ExecutablePlanBuilder {

  override def producePlan(in: PreparedQuery, ctx: PlanContext, tracer: CompilationPhaseTracer, createFingerprintReference: (Option[PlanFingerprint]) => PlanFingerprintReference) = {
    val rewriter = rewriterSequencer("SchemaOpCompiler")(reattachAliasedExpressions).rewriter
    val rewrite = in.rewrite(rewriter)

    val res = rewrite.abstractQuery match {
      case q: CallProcedure =>

        new ProcedureExecutionPlan {
          override def run(ctx: QueryContext, statement: Statement, planType: ExecutionMode, params: Map[String, Any]): InternalExecutionResult = {

            val qs = new QueryState(ctx, NoExternalResource, params, NullPipeDecorator)
            val ec = ExecutionContext.empty

            val description = PlanDescriptionImpl(new Id, "CallProcedure", NoChildren, Seq(), Set.empty)
            new AcceptableExecutionResult(new TaskCloser, statement) {
              override def queryStatistics(): InternalQueryStatistics = InternalQueryStatistics()

              override def executionMode: ExecutionMode = NormalMode

              override def javaColumns: util.List[String] =
                ctx.procedureSignature( q.name ).outputs.map( _.name ).asJava

              override def accept[EX <: Exception](visitor: ResultVisitor[EX]): Unit = {
                ctx.callProcedure( q.name, q.args.map( _.apply(ec)(qs) ), visitor )
              }

              override def executionPlanDescription(): InternalPlanDescription = description
            }
          }
        }

      case q: CreateProcedure =>
        // TODO: I don't think the below plan description is right, sort out how to test and fix. Should contain info about the created procedure
        val description = PlanDescriptionImpl(new Id, "CreateProcedure", NoChildren, Seq.empty, Set.empty)
        new ProcedureExecutionPlan {
          override def run(queryContext: QueryContext, statement: Statement, planType: ExecutionMode, params: Map[String, Any]): InternalExecutionResult = {
            val ctx = new UpdateCountingQueryContext(queryContext)
            val qs = new QueryState(ctx, NoExternalResource, params, NullPipeDecorator)

            ctx.createProcedure(q.readOnly, q.signature, q.language, q.body.apply(ExecutionContext.empty)(qs).toString)

            new AcceptableExecutionResult(new TaskCloser, statement) {
              override def queryStatistics(): InternalQueryStatistics = ctx.getStatistics

              override def executionMode: ExecutionMode = NormalMode

              override def javaColumns: util.List[String] = Collections.emptyList()

              override def accept[EX <: Exception](visitor: ResultVisitor[EX]): Unit = {}

              override def executionPlanDescription(): InternalPlanDescription = description
            }
          }
        }
    }
    res
  }
}

abstract class ProcedureExecutionPlan extends ExecutionPlan {
  override def runtimeUsed: RuntimeName = ProcedureRuntimeName

  override def isStale(lastTxId: () => Long, statistics: GraphStatistics): Boolean = false

  override def plannerUsed: PlannerName = ProcedurePlannerName

  override def isPeriodicCommit: Boolean = false
}