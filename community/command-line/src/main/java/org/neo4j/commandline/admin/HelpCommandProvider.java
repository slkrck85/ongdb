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
package org.neo4j.commandline.admin;

import java.nio.file.Path;
import javax.annotation.Nonnull;

import org.neo4j.commandline.arguments.Arguments;

public class HelpCommandProvider extends AdminCommand.Provider
{
    private final Usage usage;

    public HelpCommandProvider( Usage usage )
    {
        super( "help" );
        this.usage = usage;
    }

    @Override
    @Nonnull
    public Arguments allArguments()
    {
        return new Arguments().withOptionalPositionalArgument( 0, "command" );
    }

    @Override
    @Nonnull
    public String description()
    {
        return "This help text, or help for the command specified in <command>.";
    }

    @Override
    @Nonnull
    public String summary()
    {
        return description();
    }

    @Override
    @Nonnull
    public AdminCommandSection commandSection()
    {
        return AdminCommandSection.general();
    }

    @Override
    @Nonnull
    public AdminCommand create( Path homeDir, Path configDir, OutsideWorld outsideWorld )
    {
        return new HelpCommand( usage, outsideWorld::stdOutLine, CommandLocator.fromServiceLocator() );
    }
}
