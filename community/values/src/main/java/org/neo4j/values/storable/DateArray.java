/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.values.storable;

import java.time.LocalDate;
import java.util.Arrays;

import org.neo4j.values.ValueMapper;

public final class DateArray extends TemporalArray<LocalDate,DateValue>
{
    private final LocalDate[] value;

    DateArray( LocalDate[] value )
    {
        assert value != null;
        this.value = value;
    }

    @Override
    protected LocalDate[] value()
    {
        return value;
    }

    @Override
    public <T> T map( ValueMapper<T> mapper )
    {
        return mapper.mapDateArray( this );
    }

    @Override
    public boolean equals( Value other )
    {
        return other.equals( value );
    }

    @Override
    public boolean equals( LocalDate[] x )
    {
        return Arrays.equals( value, x );
    }

    @Override
    public <E extends Exception> void writeTo( ValueWriter<E> writer ) throws E
    {
        writeTo( writer, ValueWriter.ArrayType.DATE, value );
    }

    @Override
    public ValueGroup valueGroup()
    {
        return ValueGroup.DATE_ARRAY;
    }

    @Override
    int unsafeCompareTo( Value otherValue )
    {
        return compareToNonPrimitiveArray( (DateArray) otherValue );
    }

    @Override
    public String getTypeName()
    {
        return "DateArray";
    }

    @Override
    long sizePerItem()
    {
        //4 bytes for the pointer + the size of the payload
        return 4 + 24;
    }
}
