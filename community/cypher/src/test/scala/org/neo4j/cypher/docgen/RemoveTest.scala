package org.neo4j.cypher.docgen

import org.junit.Test
import org.junit.Assert._

class RemoveTest extends DocumentingTestBase {
  def graphDescription = List(
    "Andres:Swedish KNOWS Tobias:Swedish",
    "Andres KNOWS Peter:German:Swedish"
  )

  override val properties = Map(
    "Andres" -> Map("age" -> 36l),
    "Tobias" -> Map("age" -> 25l),
    "Peter" -> Map("age" -> 34l)
  )

  def section = "Remove"

  @Test def remove_property() {
    testQuery(
      title = "Remove a property",
      text = "Neo4j doesn't allow storing +null+ in properties. Instead, if no value exists, the property is " +
        "just not there. So, to remove a property value on a node or a relationship, is also done with +REMOVE+.",
      queryText = "start andres = node(%Andres%) remove andres.age return andres",
      returns = "The node is returned, and no property `age` exists on it.",
      assertions = (p) => assertFalse("Property was not removed as expected.", node("Andres").hasProperty("age")) )
  }

  @Test def remove_a_label_from_a_node() {
    testQuery(
      title = "Remove a label from a node",
      text = "To remove labels, you use +REMOVE+.",
      queryText = "start n = node(%Peter%) remove n:German return n",
      returns = "",
      assertions = (p) => assert(getLabelsFromNode(p) === List("Swedish"))
    )
  }

  @Test def remove_multiple_labels_from_a_node() {
    testQuery(
      title = "Removing multiple labels",
      text = "To remove multiple labels, you use +REMOVE+.",
      queryText = "start n = node(%Peter%) remove n:German:Swedish return n",
      returns = "",
      assertions = (p) => assert(getLabelsFromNode(p).isEmpty)
    )
  }
}