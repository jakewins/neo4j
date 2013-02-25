package org.neo4j.cypher.internal.executionplan

import org.neo4j.cypher.internal.commands.expressions.Expression
import org.neo4j.cypher.internal.commands.values.{LabelName, ResolvedLabel}
import org.neo4j.cypher.UnknownLabelException

case class LabelResolution(labelMapper: String => Option[ResolvedLabel]) extends (Expression => Expression) {

  def apply(expr: Expression) = expr match {
    case (label: LabelName) => labelMapper(label.name).getOrElse(throw new UnknownLabelException(label.name))
    case _ => expr
  }
}