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
package org.neo4j.cypher.internal.compiler.v2_3.ast

import org.neo4j.cypher.internal.compiler.v2_3.planner.SemanticTable
import org.neo4j.cypher.internal.compiler.v2_3._
import org.neo4j.kernel.impl.store.Neo4jTypes
import org.neo4j.kernel.impl.store.Neo4jTypes.AnyType

trait SymbolicName extends ASTNode with ASTParticle {
  def name: String
  def position: InputPosition
}

trait SymbolicNameWithId[+ID <: NameId] extends SymbolicName {
  def id(implicit semanticTable: SemanticTable): Option[ID]
}

case class LabelName(name: String)(val position: InputPosition) extends SymbolicNameWithId[LabelId] {
  def id(implicit semanticTable: SemanticTable): Option[LabelId] = semanticTable.resolvedLabelIds.get(name)
}

case class PropertyKeyName(name: String)(val position: InputPosition) extends SymbolicNameWithId[PropertyKeyId] {
  def id(implicit semanticTable: SemanticTable): Option[PropertyKeyId] = semanticTable.resolvedPropertyKeyNames.get(name)
}

case class RelTypeName(name: String)(val position: InputPosition) extends SymbolicNameWithId[RelTypeId] {
  def id(implicit semanticTable: SemanticTable): Option[RelTypeId] = semanticTable.resolvedRelTypeNames.get(name)
}

// An actual type literal, eg. 'Text', 'Node', 'Integer', 'Number'..
// TODO: Should this be expressed as a SymbolicName, is that right?
case class Type(name: String, genericType: Type = null)(val position: InputPosition) extends SymbolicName {
  def asType : AnyType = name match {
    case "Any" => Neo4jTypes.NTAny
    case "Text" => Neo4jTypes.NTText
    case "Number" => Neo4jTypes.NTNumber
    case "Integer" => Neo4jTypes.NTInteger
    case "Float" => Neo4jTypes.NTFloat
    case "Boolean" => Neo4jTypes.NTBoolean
    case "Map" => Neo4jTypes.NTMap
    case "Node" => Neo4jTypes.NTNode
    case "Relationship" => Neo4jTypes.NTRelationship
    case "Path" => Neo4jTypes.NTPath
    case "List" => Neo4jTypes.NTList( genericType.asType )
    case _ => throw new RuntimeException( name + " is not a valid type name." ) // TODO not sure where this check should go, parser?
  }
}

