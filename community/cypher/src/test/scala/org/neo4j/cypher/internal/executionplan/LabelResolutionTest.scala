package org.neo4j.cypher.internal.executionplan

import org.junit.Test
import org.scalatest.Assertions
import org.neo4j.cypher.internal.commands.values.{LabelValue, LabelName, ResolvedLabel}
import org.neo4j.cypher.internal.commands.{LabelSetOp, LabelAction}
import org.neo4j.cypher.internal.commands.expressions.Identifier
import org.neo4j.cypher.UnknownLabelException
import org.neo4j.cypher.internal.mutation.CreateNode

class LabelResolutionTest extends Assertions {

  val blue  = ResolvedLabel("blue", 42)
  val green = ResolvedLabel("green", 28)

  @Test
  def testResolvedLabelsAreNotTouched() {
    // GIVEN
    val resolver = LabelResolution(mapper())

    // WHEN
    assert(blue === resolver(blue))
  }


  @Test
  def testLabelNamesAreResolved() {
    // GIVEN
    val resolver = LabelResolution(mapper("green" -> green))
    val expr     = LabelName("green")

    // WHEN
    assert(green === resolver(expr))
  }

  @Test
  def testResolveViaTypedRewrite() {
    // GIVEN
    val resolver = LabelResolution(mapper("green" -> green))
    val expr     = LabelName("green")

    // WHEN
    assert(green === expr.typedRewrite[LabelValue](resolver))
  }

  @Test
  def testUnknownLabelNamesThrow() {
    // GIVEN
    val resolver = LabelResolution(mapper("green" -> green))
    val expr     = LabelName("red")

    // WHEN
    intercept[UnknownLabelException](resolver(expr))
  }

  def mapper(pairs: (String, ResolvedLabel)*): (String => Option[ResolvedLabel]) = {
    val mapping: Map[String, ResolvedLabel] = pairs.toMap
    val fn: (String => Option[ResolvedLabel]) = { (name: String) => mapping.get(name) }
    fn
  }
}