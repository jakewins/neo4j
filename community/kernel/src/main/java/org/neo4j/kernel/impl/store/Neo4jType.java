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
package org.neo4j.kernel.impl.store;

public class Neo4jType
{
    /**
     * A unique simple number assigned to each type, used when representing types in
     * various forms. This is expected to be durably stored and potentially public,
     * don't change ordinals.
     */
    private final int ordinal;

    public Neo4jType( int ordinal )
    {
        this.ordinal = ordinal;
    }

    public static class Text extends Neo4jType
    {
        public Text()
        {
            super( 0 );
        }
    }

    public static class Number extends Neo4jType
    {
        public Number()
        {
            super( 1 );
        }

        protected Number(int ordinal)
        {
            super( ordinal );
        }
    }

    public static class Integer extends Number
    {
        public Integer()
        {
            super( 2 );
        }
    }

    public static class Float extends Number
    {
        public Float()
        {
            super( 3 );
        }
    }
}
