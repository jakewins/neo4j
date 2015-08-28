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
package org.neo4j.cypher.internal.compiler.v2_3.parser

import org.neo4j.cypher.internal.compiler.v2_3.ast.{Expression, FunctionInvocation}
import org.neo4j.cypher.internal.compiler.v2_3.ast.convert.commands.StatementConverters
import StatementConverters._
import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.Literal
import org.neo4j.cypher.internal.compiler.v2_3.spi.{ProcedureName, ProcedureSignature, Argument}
import org.neo4j.cypher.internal.compiler.v2_3.symbols.{IntegerType}
import org.neo4j.cypher.internal.compiler.v2_3.{commands => legacyCommands, _}
import org.parboiled.scala._

class ProcedureTest extends ParserTest[ast.Command, legacyCommands.AbstractQuery] with Command {
  implicit val parserToTest = Command ~ EOI

  test("create_no_arg_procedure") {
    parsing("CREATE READ ONLY PROCEDURE myProc() USING javascript FROM SOURCE \"log.info('hello, world');\"") or
      parsing("create read only procedure myProc() USING javascript FROM SOURCE \"log.info('hello, world');\"") shouldGive
        legacyCommands.CreateProcedure( true, ProcedureSignature( ProcedureName(Seq(), "myProc"), List(), List()), "javascript", Literal("log.info('hello, world');") )
  }

  test("create_no_output_procedure") {
    parsing("CREATE READ ONLY PROCEDURE myProc( in1: Integer ) USING javascript FROM SOURCE " +
      "\"log.info('hello, world');\"") or
      parsing("create read only procedure myProc( in1: Integer ) using javascript from source " +
        "\"log.info('hello, world');\"") shouldGive
      legacyCommands.CreateProcedure( true, ProcedureSignature( ProcedureName(Seq(), "myProc"), List(Argument("in1", IntegerType.instance)), List()), "javascript", Literal("log.info('hello, world');") )
  }

  test("create_procedure") {
    parsing("CREATE READ ONLY PROCEDURE myProc( in1: Integer ) : (out1: Integer) USING javascript FROM SOURCE " +
      "\"yield record(in1);\"") or
      parsing("create read only procedure myProc( in1: Integer ) : (out1: Integer) using javascript from source " +
        "\"yield record(in1);\"") shouldGive
      legacyCommands.CreateProcedure( true, ProcedureSignature( ProcedureName(Seq(), "myProc"), List(Argument("in1", IntegerType.instance)), List(Argument("out1", IntegerType.instance))), "javascript", Literal("yield record(in1);") )
  }

  test("call_procedure") {
    parsing("CALL myProc( 12 )") or
      parsing("call myProc( 12 )") shouldGive
      legacyCommands.CallProcedure( ProcedureName(Seq(), "myProc"), IndexedSeq( Literal(12) ) )
  }

  def convert(astNode: ast.Command): legacyCommands.AbstractQuery = astNode.asQuery
}
