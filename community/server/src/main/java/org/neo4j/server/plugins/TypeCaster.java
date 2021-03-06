/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB.
 *
 * ONgDB is free software: you can redistribute it and/or modify
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
package org.neo4j.server.plugins;

import java.util.Arrays;

import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.server.rest.repr.BadInputException;

abstract class TypeCaster
{
    abstract Object get( GraphDatabaseAPI graphDb, ParameterList parameters, String name )
            throws BadInputException;

    Object convert( Object[] result ) throws BadInputException
    {
        throw new BadInputException( "Cannot convert to primitive array: " + Arrays.toString( result ) );
    }

    abstract Object[] getList( GraphDatabaseAPI graphDb, ParameterList parameters, String name )
            throws BadInputException;
}
