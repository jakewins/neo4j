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
package org.neo4j.kernel.api.procedure;

/** Describes a procedure stored in the database */
public class ProcedureDescriptor
{
    /**
     * For future-compatibility with the cypher planner, and to keep the door open to perform write-forwarding
     * of cypher statements, we track read/update mode of each procedure and store this in the definition on disk.
     * Please don't remove this, I know it's now being used, it's there to not have to break backwards compat when
     * we need it for the planner, auth and clustering.
     */
    public enum Mode
    {
        READ_ONLY,
        UPDATE
    }

    private final ProcedureSignature signature;
    private final String language;
    private final Mode mode;
    private final String procedureBody;


    public ProcedureDescriptor( ProcedureSignature signature, String language, Mode mode, String procedureBody )
    {
        this.signature = signature;
        this.language = language;
        this.mode = mode;
        this.procedureBody = procedureBody;
    }

    public ProcedureSignature signature()
    {
        return signature;
    }

    public String language()
    {
        return language;
    }

    public String procedureBody()
    {
        return procedureBody;
    }

    public Mode mode()
    {
        return mode;
    }

    @Override
    public boolean equals( Object o )
    {
        // Note that equality is *only* checked on signature, this is probably wrong, but is depended on in
        // TxState. Please don't change this without modifying how txstate tracks changes to procedures.
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ProcedureDescriptor that = (ProcedureDescriptor) o;

        if ( !signature.equals( that.signature ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return signature.hashCode();
    }
}
