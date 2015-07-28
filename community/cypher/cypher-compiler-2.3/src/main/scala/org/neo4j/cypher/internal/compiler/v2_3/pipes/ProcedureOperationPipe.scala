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
package org.neo4j.cypher.internal.compiler.v2_3.pipes

import org.neo4j.cypher.internal.compiler.v2_3.ExecutionContext
import org.neo4j.cypher.internal.compiler.v2_3.commands._
import org.neo4j.cypher.internal.compiler.v2_3.executionplan.Effects
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.{NoChildren, PlanDescriptionImpl}
import org.neo4j.cypher.internal.compiler.v2_3.symbols._
import org.neo4j.kernel.api.procedure.ProcedureSignature
import org.neo4j.kernel.impl.store.Neo4jTypes
import org.neo4j.kernel.impl.store.Neo4jTypes.{NodeType, AnyType}
import scala.collection.JavaConversions._

class ProcedureOperationPipe(op: ProcedureOperation)
                        (implicit val monitor: PipeMonitor) extends Pipe {
  protected def internalCreateResults(state: QueryState): Iterator[ExecutionContext] = {
    val baseContext = state.initialContext.getOrElse(ExecutionContext.empty)

    op match {
      case c: CreateProcedure =>
        state.query.createProcedure( c.readOnly, c.signature, c.language, c.body.apply(baseContext)(state).toString ) // TODO not toString
    }
    Iterator.empty
  }

  def symbols = new SymbolTable()

  def planDescription = new PlanDescriptionImpl(this.id, "ProcedureOperation", NoChildren, Seq.empty, identifiers)

  def exists(pred: Pipe => Boolean) = pred(this)

  def dup(sources: List[Pipe]): Pipe = {
    require(sources.isEmpty)
    this
  }

  def sources: Seq[Pipe] = Seq.empty

  override val localEffects = Effects()
}

class ProcedureCallPipe(op: CallProcedure, signature: ProcedureSignature)
                            (implicit val monitor: PipeMonitor) extends Pipe {
  protected def internalCreateResults(state: QueryState): Iterator[ExecutionContext] = {
    val baseContext = state.initialContext.getOrElse(ExecutionContext.empty)
    val evaledArgs = op.args.map( _.apply(baseContext)(state) )

    state.query.callProcedure( signature, evaledArgs ).map( (v) => {
      val newMap = MutableMaps.create(columns.size)
      (v zip columns).foreach( (item) => newMap(item._2 ) = item._1 )
      baseContext.newFromMutableMap( newMap )
    })
  }

  def symbols = new SymbolTable(signature.outputSignature().map( (p) => (p.first(), toCypherType(p.other())) ).toMap)

  def planDescription = new PlanDescriptionImpl(this.id, "ProcedureCall", NoChildren, Seq.empty, identifiers)

  def exists(pred: Pipe => Boolean) = pred(this)

  def dup(sources: List[Pipe]): Pipe = {
    require(sources.isEmpty)
    this
  }

  def sources: Seq[Pipe] = Seq.empty

  override val localEffects = Effects()
  private val columns = symbols.keys

  private def toCypherType( t:AnyType ): CypherType = t match {
    case t:Neo4jTypes.NodeType => CTNode
    case t:Neo4jTypes.RelationshipType => CTRelationship
    case t:Neo4jTypes.PathType => CTPath
    case t:Neo4jTypes.MapType => CTMap
    case t:Neo4jTypes.BooleanType => CTBoolean
    case t:Neo4jTypes.IntegerType => CTInteger
    case t:Neo4jTypes.FloatType => CTFloat
    case t:Neo4jTypes.NumberType => CTNumber
    case t:Neo4jTypes.TextType => CTString
    case t:Neo4jTypes.ListType => CTCollection(toCypherType(t.innerType()))
    case t:Neo4jTypes.AnyType => CTAny
  }
}
