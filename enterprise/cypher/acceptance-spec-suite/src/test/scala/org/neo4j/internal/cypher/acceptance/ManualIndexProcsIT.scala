/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.cypher.acceptance

import org.neo4j.cypher.{CypherExecutionException, ExecutionEngineFunSuite}
import org.neo4j.graphdb.config.Setting
import org.neo4j.graphdb.factory.GraphDatabaseSettings

class ManualIndexProcsIT extends ExecutionEngineFunSuite {

  override def databaseConfig(): Map[Setting[_], String] = Map(
    GraphDatabaseSettings.node_auto_indexing -> "true",
    GraphDatabaseSettings.node_keys_indexable -> "name,email",
    GraphDatabaseSettings.relationship_auto_indexing -> "true",
    GraphDatabaseSettings.relationship_keys_indexable -> "weight")

  test("Node from exact key value match") {
    val node = createNode()
    graph.inTx {
      graph.index().forNodes("index").add(node, "key", "value")
    }

    val result = execute(
      """CALL db.index.manual.seek.nodes('index', 'key', 'value')
        |YIELD node AS n RETURN n""".stripMargin).toList

    result should equal(List(Map("n" -> node)))
  }

  test("should fail if index doesn't exist for node seek") {
    a[CypherExecutionException] should be thrownBy
      execute(
        """CALL db.index.manual.seek.nodes('index', 'key', 'value')
          |YIELD node AS n RETURN n""".stripMargin)
  }

  test("should return node from manual index search") {
    val node = createNode()
    graph.inTx {
      graph.index().forNodes("index").add(node, "key", "value")
    }

    val result = execute( """CALL db.index.manual.nodes("index", "key:value") YIELD node as n RETURN n""").toList

    result should equal(List(Map("n" -> node)))
  }

  test("should fail if index doesn't exist for node search") {
    a[CypherExecutionException] should be thrownBy
      execute("""CALL db.index.manual.nodes('index', 'key:value') YIELD node AS n RETURN n""")
  }

  test("legacy index + where") {
    val node = createNode(Map("prop" -> 42))
    val otherNode = createNode(Map("prop" -> 21))
    val thirdNode = createNode(Map("prop" -> 37))
    val fourthNode = createNode(Map("prop" -> 21))

    graph.inTx {
      graph.index().forNodes("index").add(node, "key", "value")
      graph.index().forNodes("index").add(otherNode, "key", "value")
      graph.index().forNodes("index").add(thirdNode, "key", "value")
      graph.index().forNodes("index").add(fourthNode, "key", "value")
    }

    val result = execute(
      """CALL db.index.manual.nodes("index", "key:value") YIELD node AS n WHERE n.prop = 21 RETURN n""").toList

    result should equal(List(Map("n" -> otherNode), Map("n" -> fourthNode)))
  }

  test("should seek relationship from manual index") {
    val node = createNode(Map("prop" -> 42))
    val otherNode = createNode(Map("prop" -> 21))
    val relationship = relate(node, otherNode)
    val unwantedRelationship = relate(node, otherNode)
    val ignoredRelationship = relate(node, otherNode)

    graph.inTx {
      val relationshipIndex = graph.index().forRelationships("relIndex")
      relationshipIndex.add(relationship, "key", "value")
      relationshipIndex.add(unwantedRelationship, "key", "wrongValue")
      relationshipIndex.add(ignoredRelationship, "wrongKey", "value")
    }

    val query = "CALL db.index.manual.seek.relationships('relIndex', 'key', 'value') YIELD relationship AS r RETURN r"
    val result = execute(query)

    result.toList should equal(List(Map("r" -> relationship)))
  }

  test("should fail if index doesn't exist for relationship") {
    a[CypherExecutionException] should be thrownBy
      execute("""CALL db.index.manual.seek.relationships('index', 'key', 'value') YIELD relationship AS r RETURN r""")
  }

  test("should do manual relationship index search") {
    val node = createNode(Map("prop" -> 42))
    val otherNode = createNode(Map("prop" -> 21))
    val r1 = relate(node, otherNode)
    val r2 = relate(node, otherNode)
    val r3 = relate(node, otherNode)
    val r4 = relate(node, otherNode)

    graph.inTx {
      val relationshipIndex = graph.index().forRelationships("relIndex")
      relationshipIndex.add(r1, "wrongKey", "value")
      relationshipIndex.add(r2, "key", "value")
      relationshipIndex.add(r3, "key", "wrongValue")
      relationshipIndex.add(r4, "key", "value")
    }

    val query = "CALL db.index.manual.relationships('relIndex','key:value') YIELD relationship AS r RETURN r"
    val result = execute(query)

    result.toList should equal(List(
      Map("r" -> r2),
      Map("r" -> r4)
    ))
  }

  test("should MATCH undirected using result from manual index search") {
    val node = createNode(Map("prop" -> 42))
    val otherNode = createNode(Map("prop" -> 21))
    val relationship = relate(node, otherNode)

    graph.inTx {
      val relationshipIndex = graph.index().forRelationships("relIndex")
      relationshipIndex.add(relationship, "key", "value")
    }

    val query = "CALL db.index.manual.relationships('relIndex','key:*') YIELD relationship AS r MATCH (a)-[r]-(b) RETURN r"
    val result = execute(query)

    result.toList should equal(List(
      Map("r" -> relationship),
      Map("r" -> relationship)
    ))
  }

  test("should MATCH directed using result from manual index search") {
    val node = createNode(Map("prop" -> 42))
    val otherNode = createNode(Map("prop" -> 21))
    val relationship = relate(node, otherNode)

    graph.inTx {
      val relationshipIndex = graph.index().forRelationships("relIndex")
      relationshipIndex.add(relationship, "key", "value")
    }

    val query = "CALL db.index.manual.relationships('relIndex','key:*') YIELD relationship AS r MATCH (a)-[r]->(b) RETURN r"
    val result = execute(query)

    result.toList should equal(List(
      Map("r" -> relationship)
    ))
  }

  test("should return correct results on combined node and relationship index seek") {
    val node = createNode()
    val resultNode = createNode()
    val rel = relate(node, resultNode)
    relate(node, createNode())

    graph.inTx {
      graph.index().forNodes("nodes").add(node, "key", "A")
      graph.index().forRelationships("rels").add(rel, "key", "B")
    }

    val result = execute("CALL db.index.manual.seek.nodes('nodes', 'key', 'A') YIELD node AS n " +
      "CALL db.index.manual.seek.relationships('rels', 'key', 'B') YIELD relationship AS r " +
      "MATCH (n)-[r]->(b) RETURN b")
    result.toList should equal(List(Map("b" -> resultNode)))
  }

  test("Auto-index node from exact key value match using manual feature") {
    val node = createNode(Map("name" -> "Neo"))

    val result = execute(
      """CALL db.index.manual.seek.nodes('node_auto_index', 'name', 'Neo')
        |YIELD node AS n RETURN n""".stripMargin).toList

    result should equal(List(Map("n" -> node)))
  }

  test("Auto-index relationship from exact key value match using manual seek") {
    val a = createNode()
    val b = createNode()
    val rel = relate(a, b, "weight" -> 12)

    val result = execute(
      """CALL db.index.manual.seek.relationships('relationship_auto_index', 'weight', 12)
        |YIELD relationship AS r RETURN r""".stripMargin).toList

    result should equal(List(Map("r" -> rel)))
  }

  test("Auto-index and return node using auto seek") {
    createNode(Map("wrongName" -> "Neo"))
    val node = createNode(Map("name" -> "Neo"))
    createNode(Map("name" -> "wrongKey"))

    val result = execute(
      """CALL db.index.auto.seek.nodes('name', 'Neo')
        |YIELD node AS n RETURN n""".stripMargin).toList

    result should equal(List(Map("n" -> node)))
  }

  test("Auto-index and return node using auto search") {
    val node = createNode(Map("name" -> "Neo"))
    val node2 = createNode(Map("name" -> "Johan"))
    val node3 = createNode(Map("name" -> "Nina"))

    val result = execute("CALL db.index.auto.nodes('name:N*') YIELD node as n RETURN n").toList

    result should equal(List(Map("n" -> node), Map("n" -> node3)))
  }

  test("Auto-index and return relationship using auto search") {
    val a = createNode()
    val b = createNode()
    var rel = relate(a, b, "weight" -> 42)
    var rel2 = relate(a, b, "weight" -> 37)
    var rel3 = relate(a, b, "weight" -> 3)
    var rel4 = relate(a, b, "width" -> 3)

    val result = execute(
      """CALL db.index.auto.relationships('weight:3*')
    |YIELD relationship AS r RETURN r""".stripMargin).toList

    result should equal(List(Map("r" -> rel2), Map("r" -> rel3)))
  }

  test("Auto-index relationship from exact key value match using auto feature") {
    val a = createNode()
    val b = createNode()
    val rel = relate(a, b, "weight" -> 12)
    relate(a, b, "height" -> 12)
    relate(a, b, "weight" -> 10)

    val result = execute(
      """CALL db.index.auto.seek.relationships('weight', 12)
        |YIELD relationship AS r RETURN r""".stripMargin).toList

    result should equal(List(Map("r" -> rel)))
  }

  test("Should be able to create a node manual index by using a procedure") {
    // Given a database with nodes with properties
    val node = createNode(Map("name" -> "Neo"))

    // When adding a node to the index, the index should exist
    val addResult = execute(
      """MATCH (n) WITH n CALL db.index.manual.add.node('usernames', n, 'name', 'Neo') YIELD success as s RETURN s"""
        .stripMargin).toList

    addResult should be(List(Map("s" -> true)))

    // Then the index should exist
    val result = execute("CALL db.index.manual.exists.forNodes('usernames')").toList

    result should be(List(Map("success" -> true)))

    // And queries should return nodes
    graph.inTx {
      val results = graph.index().forNodes("usernames").get("name", "Neo")
      results.size() should be(1)
      results.getSingle should be(node)
    }
  }

  test("Maunal relationships index should exist") {
    val a = createNode(Map("name" -> "Neo"))
    val b = createNode()
    val rel = relate(a, b, "distance" -> 12)

    val addResult = execute(
      """MATCH (n)-[r]-(m) WHERE n.name = 'Neo' WITH r CALL db.index.manual.add.relationship('relIndex', r, 'distance', 12) YIELD success as s RETURN s"""
        .stripMargin).toList

    addResult should be(List(Map("s" -> true)))

    val result = execute(
      """CALL db.index.manual.exists.forRelationships('relIndex')
        |YIELD success AS s RETURN s""".stripMargin).toList

    result should equal(List(Map("s" -> true)))
  }

  test("Should be able to drop node index") {
    // Given a database with nodes with properties
    val node = createNode(Map("name" -> "Neo"))

    // When adding a node to the index
    graph.inTx {
      graph.index().forNodes("usernames").add(node, "name", "Neo")
    }

    // Then the index should be possible to drop
    val result = execute("CALL db.index.manual.drop('usernames')").toList

    result should be(List(Map("name" -> "usernames", "type" -> "NODE", "config" -> Map("provider" -> "lucene", "type" -> "exact"))))
  }

  test("Should be able to drop relationship index") {
    // Given a relationship with property
    val a = createNode(Map("name" -> "Neo"))
    val b = createNode()
    val rel = relate(a, b, "distance" -> 12)

    // When adding the relationship to an index
    graph.inTx {
      graph.index().forRelationships("relIndex").add(rel, "distance", 12)
    }

    // Then the index should be possible to drop
    val result = execute("CALL db.index.manual.drop('relIndex')").toList

    result should be(List(Map("name" -> "relIndex", "type" -> "RELATIONSHIP", "config" -> Map("provider" -> "lucene", "type" -> "exact"))))
  }

  test("Should able to add and remove a node from manual index") {
    val node = createNode(Map("name" -> "Neo"))

    val addResult = execute(
      """MATCH (n) WITH n CALL db.index.manual.add.node('usernames', n, 'name', 'Neo') YIELD success as s RETURN s"""
        .stripMargin).toList

    addResult should be(List(Map("s" -> true)))

    val seekResult = execute("CALL db.index.manual.seek.nodes('usernames', 'name', 'Neo') YIELD node AS n ").toList

    seekResult should equal(List(Map("n" -> node)))

    val result = execute(
      """MATCH (n) WITH n CALL db.index.manual.remove.node('usernames', n, 'name') YIELD success as s RETURN s"""
        .stripMargin).toList

    result should equal(List(Map("s" -> true)))

    val emptyResult = execute("CALL db.index.manual.seek.nodes('usernames', 'name', 'Neo') YIELD node AS n ").toList

    emptyResult should equal(List.empty)

  }

  test("Should able to add and remove a relationship from manual index") {
    val a = createNode(Map("name" -> "Neo"))
    val b = createNode()
    val rel = relate(a, b, "distance" -> 12)

    val addResult = execute(
      """MATCH (n)-[r]-(m) WHERE n.name = 'Neo' WITH r CALL db.index.manual.add.relationship('relIndex', r, 'distance', 12) YIELD success as s RETURN s"""
        .stripMargin).toList

    addResult should be(List(Map("s" -> true)))

    val seekResult = execute("CALL db.index.manual.seek.relationships('relIndex', 'distance', '12') YIELD relationship AS r ").toList

    seekResult should equal(List(Map("r" -> rel)))

    val result = execute(
      """MATCH (n)-[r]-(m) WHERE n.name = 'Neo' WITH r CALL db.index.manual.remove.relationship('relIndex', r, 'distance') YIELD success as s RETURN s"""
        .stripMargin).toList

    result should equal(List(Map("s" -> true)))

    val emptyResult = execute("CALL db.index.manual.seek.relationships('relIndex', 'distance', '12') YIELD relationship AS r ").toList

    emptyResult should equal(List.empty)
  }

  test("should be able to get or create a node index") {
    //Given the node index does not exist
    graph.inTx {
      graph.index().existsForNodes("usernames") should be(false)
    }

    //When calling nodeManualIndex
    graph.execute("CALL db.index.manual.forNodes('usernames')")

    //Then the index should exist
    graph.inTx {
      graph.index().existsForNodes("usernames") should be(true)
    }
  }

  test("should be able to get or create a relationship index") {
    //Given the relationship index does not exist
    graph.inTx {
      graph.index().existsForRelationships("relIndex") should be(false)
    }

    //When calling nodeManualIndex
    graph.execute("CALL db.index.manual.forRelationships('relIndex')")

    //Then the index should exist
    graph.inTx {
      graph.index().existsForRelationships("relIndex") should be(true)
    }
  }

  test("should be able to list manual and automatic indexes") {
    //Given the node and relationship indexes do not exist
    graph.inTx {
      graph.index().nodeIndexNames().length should be(0)
    }

    //When creating indexes both manually and automatically
    graph.execute("CALL db.index.manual.forNodes('manual1')")
    graph.execute("CALL db.index.manual.forRelationships('manual2')")
    graph.execute("CREATE (n) WITH n CALL db.index.manual.add.node('usernames',n,'username','Neo') YIELD success RETURN success")
    graph.execute("CREATE (n), (m), (n)-[r:KNOWS]->(m) WITH r CALL db.index.manual.add.relationship('relIndex',r,'distance',42) YIELD success RETURN success")
    graph.execute("CREATE (n {email:'joe@soap.net'})")
    graph.execute("CREATE (n), (m), (n)-[r:KNOWS {weight:42}]->(m)")

    //Then the indexes should all exist
    graph.inTx {
      graph.index().nodeIndexNames().toSet should be(Set("manual1",  "usernames", "node_auto_index"))
      graph.index().relationshipIndexNames().toSet should be(Set("manual2", "relIndex", "relationship_auto_index"))
    }

    //And have the right types
    val result = execute("CALL db.index.manual.list").toSet
    result should be(Set(
      Map("name" -> "manual1", "type" -> "NODE", "config" -> Map("provider" -> "lucene", "type" -> "exact")),
      Map("name" -> "manual2", "type" -> "RELATIONSHIP", "config" -> Map("provider" -> "lucene", "type" -> "exact")),
      Map("name" -> "usernames", "type" -> "NODE", "config" -> Map("provider" -> "lucene", "type" -> "exact")),
      Map("name" -> "relIndex", "type" -> "RELATIONSHIP", "config" -> Map("provider" -> "lucene", "type" -> "exact")),
      Map("name" -> "node_auto_index", "type" -> "NODE", "config" -> Map("provider" -> "lucene", "type" -> "exact")),
      Map("name" -> "relationship_auto_index", "type" -> "RELATIONSHIP", "config" -> Map("provider" -> "lucene", "type" -> "exact"))
    ))
  }
}


