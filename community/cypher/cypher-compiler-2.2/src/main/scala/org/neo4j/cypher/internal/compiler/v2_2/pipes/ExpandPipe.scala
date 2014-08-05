/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.cypher.internal.compiler.v2_2.pipes

import org.neo4j.cypher.InternalException
import org.neo4j.cypher.internal.compiler.v2_2.ExecutionContext
import org.neo4j.cypher.internal.compiler.v2_2.executionplan.Effects
import org.neo4j.cypher.internal.compiler.v2_2.planDescription.PlanDescription.Arguments.IntroducedIdentifier
import org.neo4j.cypher.internal.compiler.v2_2.symbols._
import org.neo4j.graphdb.{Direction, Node, Relationship}
import org.neo4j.cursor.Cursor
import org.neo4j.register.{ObjectRegister, LongRegister, Register}
import org.neo4j.cypher.internal.compiler.v2_2.spi.QueryContext

/**
 * Input cursor passed to the kernel.
 */
private class ExpandInputCursor(input:Iterator[ExecutionContext], from: String, nodeReg: Register.Int64.Write,
                                currentRow: Register.Obj.Write[ExecutionContext]) extends Cursor
{
  override def next() = if(input.hasNext) {
    val row: ExecutionContext = input.next()
    currentRow.write(row)

    getFromNode( row ) match {
      case n: Node =>
        nodeReg.write(n.getId)
        true
      case null => next()
      case value => throw new InternalException(s"Expected to find a node at $from but found $value instead")
    }
  } else false

  override def close() = ???
  override def reset() = ???

  def getFromNode(row: ExecutionContext): Any =
    row.getOrElse(from, throw new InternalException(s"Expected to find a node at $from but found nothing"))
}

private class ExpandOutputIterator( cursor:Cursor, relName: String, to: String, state: QueryState,
                                   currentRow: Register.Obj.Read[ExecutionContext], relReg: Register.Int64.Read,
                                   neighbor: Register.Int64.Read) extends Iterator[ExecutionContext]
{
  var nextRow:ExecutionContext = null

  override def hasNext = nextRow != null || prefetch

  override def next() = if(hasNext) { val current = nextRow; nextRow = null; current } else null

  private def prefetch = if(cursor.next)
  {
    nextRow = currentRow.read().newWith(Seq(
                              relName -> state.query.relationshipOps.getById( relReg.read() ),
                              to -> state.query.nodeOps.getById( neighbor.read() )))
    true
  } else false

}

case class ExpandPipe(source: Pipe, from: String, relName: String, to: String, dir: Direction, types: Seq[String])
                     (implicit pipeMonitor: PipeMonitor) extends PipeWithSource(source, pipeMonitor) {
  protected def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState): Iterator[ExecutionContext] = {
    val nodeReg     = new LongRegister()
    val typeReg     = new ObjectRegister[Array[Int]](typeIds(state.query))
    val dirReg      = new ObjectRegister[Direction](dir)
    val relReg      = new LongRegister()
    val neighborReg = new LongRegister()

    val currentRow = new ObjectRegister[ExecutionContext]()

    new ExpandOutputIterator(state.query.traverse( new ExpandInputCursor(input, from, nodeReg, currentRow),
      nodeReg, typeReg, dirReg, relReg, neighborReg), relName, to, state, currentRow, relReg, neighborReg )
  }

  def typeIds(state:QueryContext) : Array[Int] = types.flatMap ( t => state.getOptRelTypeId(t).toList ).toArray // Putting on my functional pants!

  def planDescription = {
    val arguments = Seq(IntroducedIdentifier(relName), IntroducedIdentifier(to))
    source.planDescription.andThen(this, "Expand", arguments:_*)
  }

  val symbols = source.symbols.add(to, CTNode).add(relName, CTRelationship)

  def dup(sources: List[Pipe]): Pipe = {
    val (source :: Nil) = sources
    copy(source = source)
  }

  override def localEffects = Effects.READS_ENTITIES
}
