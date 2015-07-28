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
package org.neo4j.cypher.internal.compiler.v2_3.commands

import org.neo4j.cypher.internal.compiler.v2_3.ast.FunctionInvocation
import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.Expression
import org.neo4j.kernel.api.procedure.ProcedureSignature
import org.neo4j.kernel.impl.store.Neo4jTypes.AnyType

sealed abstract class IndexOperation extends AbstractQuery {
  val label: String
}

// TODO use label: LabelValue?
final case class CreateIndex(label: String, propertyKeys: Seq[String], queryString: QueryString = QueryString.empty) extends IndexOperation {
  def setQueryText(t: String): CreateIndex = copy(queryString = QueryString(t))
}

final case class DropIndex(label: String, propertyKeys: Seq[String], queryString: QueryString = QueryString.empty) extends IndexOperation {
  def setQueryText(t: String): DropIndex = copy(queryString = QueryString(t))
}

sealed abstract class PropertyConstraintOperation extends AbstractQuery {
  def id: String
  def idForProperty: String
  def propertyKey: String
}

sealed abstract class NodePropertyConstraintOperation extends PropertyConstraintOperation {
  def label: String
}

sealed abstract class RelationshipPropertyConstraintOperation extends PropertyConstraintOperation {
  def relType: String
}

final case class CreateUniqueConstraint(id: String, label: String, idForProperty: String, propertyKey: String,
                                        queryString: QueryString = QueryString.empty) extends NodePropertyConstraintOperation {
  def setQueryText(t: String): CreateUniqueConstraint = copy(queryString = QueryString(t))
}

final case class DropUniqueConstraint(id: String, label: String, idForProperty: String, propertyKey: String,
                                      queryString: QueryString = QueryString.empty) extends NodePropertyConstraintOperation {
  def setQueryText(t: String): DropUniqueConstraint = copy(queryString = QueryString(t))
}

final case class CreateNodeMandatoryPropertyConstraint(id: String, label: String, idForProperty: String,
                                                       propertyKey: String, queryString: QueryString = QueryString.empty) extends NodePropertyConstraintOperation {
  def setQueryText(t: String): CreateNodeMandatoryPropertyConstraint = copy(queryString = QueryString(t))
}

final case class DropNodeMandatoryPropertyConstraint(id: String, label: String, idForProperty: String, propertyKey: String,
                                                     queryString: QueryString = QueryString.empty) extends NodePropertyConstraintOperation {
  def setQueryText(t: String): DropNodeMandatoryPropertyConstraint = copy(queryString = QueryString(t))
}

final case class CreateRelationshipMandatoryPropertyConstraint(id: String, relType: String, idForProperty: String,
                                                               propertyKey: String, queryString: QueryString = QueryString.empty) extends RelationshipPropertyConstraintOperation {
  def setQueryText(t: String): CreateRelationshipMandatoryPropertyConstraint = copy(queryString = QueryString(t))
}

final case class DropRelationshipMandatoryPropertyConstraint(id: String, relType: String, idForProperty: String, propertyKey: String,
                                                             queryString: QueryString = QueryString.empty) extends RelationshipPropertyConstraintOperation {
  def setQueryText(t: String): DropRelationshipMandatoryPropertyConstraint = copy(queryString = QueryString(t))
}


sealed abstract class ProcedureOperation extends AbstractQuery

// TODO: This shouldn't be in 'IndexOperation'
final case class CreateProcedure(readOnly: Boolean, signature: ProcedureSignature, language: String, body:Expression, queryString: QueryString = QueryString.empty) extends ProcedureOperation {
  def setQueryText(t: String): CreateProcedure = copy(queryString = QueryString(t))
}

final case class CallProcedure(namespace: Seq[String], name:String, args: IndexedSeq[Expression], queryString: QueryString = QueryString.empty) extends ProcedureOperation {

  def setQueryText(t: String): CallProcedure = copy(queryString = QueryString(t))
}