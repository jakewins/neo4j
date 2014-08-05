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
package org.neo4j.register;

/**
 * Object register that uses a simple non-volatile reference field to store a single object.
 */
public class ObjectRegister<T> implements Register.Obj.Read<T>, Register.Obj.Write<T>
{
    private T value;

    public ObjectRegister( )
    {
        this(null);
    }

    public ObjectRegister( T initialValue )
    {
        this.value = initialValue;
    }

    @Override
    public T read()
    {
        return value;
    }

    @Override
    public void write( T value )
    {
        this.value = value;
    }
}
