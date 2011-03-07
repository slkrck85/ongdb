###
Copyright (c) 2002-2011 "Neo Technology,"
Network Engine for Objects in Lund AB [http://neotechnology.com]

This file is part of Neo4j.

Neo4j is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
###

define( 
  ['./views/DashboardView'
   './models/ServerPrimitives'
   './models/DiskUsage'
   './models/CacheUsage'
   'lib/backbone'],
  (DashboardView, ServerPrimitives, DiskUsage, CacheUsage) ->
  
    class DashboardController extends Backbone.Controller
      routes : 
        "" : "dashboard"

      initialize : (appState) =>
        @appState = appState

      dashboard : =>
        @appState.set( mainView : @getDashboardView() )

      getDashboardView : =>
        @dashboardView ?= new DashboardView  
          state      : @appState
          primitives : @getServerPrimitives()
          diskUsage  : @getDiskUsage()
          cacheUsage : @getCacheUsage()

      getServerPrimitives : =>
        @serverPrimitives ?= new ServerPrimitives( server : @appState.getServer(), pollingInterval : 5000 )

      getDiskUsage : =>
        @diskUsage ?= new DiskUsage( server : @appState.getServer(), pollingInterval : 5000 )

      getCacheUsage : =>
        @cacheUsage ?= new CacheUsage( server : @appState.getServer(), pollingInterval : 5000 )

)
