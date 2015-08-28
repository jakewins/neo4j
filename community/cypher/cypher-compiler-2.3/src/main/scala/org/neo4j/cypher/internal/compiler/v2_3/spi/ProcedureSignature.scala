package org.neo4j.cypher.internal.compiler.v2_3.spi

import org.neo4j.cypher.internal.compiler.v2_3.symbols._

case class ProcedureName(namespace:Seq[String], name:String)
case class Argument( name:String, typ: CypherType )
case class ProcedureSignature( name:ProcedureName, inputs:Seq[Argument], outputs:Seq[Argument] )
