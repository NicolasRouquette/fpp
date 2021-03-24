package fpp.compiler.analysis

import fpp.compiler.ast._
import fpp.compiler.util._

/** An FPP topology */
case class Topology(
  /** The AST node defining the topology */
  aNode: Ast.Annotated[AstNode[Ast.DefTopology]],
  /** The imported topologies */
  importedTopologyMap: Map[Symbol.Topology, Location] = Map(),
  /** The instances of this topology */
  instanceMap: Map[ComponentInstance, (Ast.Visibility, Location)] = Map(),
  /** The connection patterns of this topology */
  patternMap: Map[Ast.SpecConnectionGraph.Pattern.Kind, ConnectionPattern] = Map(),
  /** The connection graphs of this topology */
  connectionGraphMap: Map[Name.Unqualified, List[Connection]] = Map(),
  /** The output connections going from each port */
  outputConnectionMap: Map[PortInstanceIdentifier, Set[PortInstanceIdentifier]] = Map(),
  /** The input connections going to each port */
  inputConnectionMap: Map[PortInstanceIdentifier, Set[PortInstanceIdentifier]] = Map(),
  /** The set of port instances declared as unused */
  declaredUnusedPortSet: Set[PortInstanceIdentifier] = Set(),
  /** The set of port instances actually unused */
  unusedPortSet: Set[PortInstanceIdentifier] = Set()
) {

  /** Gets the unqualified name of the topology */
  def getUnqualifiedName = aNode._2.data.name

  /** Add a connection */
  def addConnection(
    graphName: Name.Unqualified,
    connection: Connection
  ): Topology = {
    val cgMap = {
      val connections = connectionGraphMap.getOrElse(graphName, Nil)
      connectionGraphMap + (graphName -> (connection :: connections))
    }
    val ocMap = {
      val from = connection.from.portInstanceIdentifier
      val connections = outputConnectionMap.getOrElse(from, Set())
      val to = connection.to.portInstanceIdentifier
      outputConnectionMap + (from -> (connections + to))
    }
    val icMap = {
      val to = connection.to.portInstanceIdentifier
      val connections = inputConnectionMap.getOrElse(to, Set())
      val from = connection.from.portInstanceIdentifier
      inputConnectionMap + (to -> (connections + from))
    }
    this.copy(
      connectionGraphMap = cgMap,
      outputConnectionMap = ocMap,
      inputConnectionMap = icMap
    )
  }

  /** Add a pattern */
  def addPattern(
    kind: Ast.SpecConnectionGraph.Pattern.Kind,
    pattern: ConnectionPattern
  ): Result.Result[Topology] = patternMap.get(kind) match {
    case Some(prevPattern) =>
      Left(
        SemanticError.DuplicatePattern(
          kind.toString,
          pattern.getLoc,
          prevPattern.getLoc
        )
      )
    case None =>
      val pm = patternMap + (kind -> pattern)
      Right(this.copy(patternMap = pm))
  }

  /** Add an imported topology */
  def addImportedTopology(
    symbol: Symbol.Topology,
    loc: Location
  ): Result.Result[Topology] =
    importedTopologyMap.get(symbol) match {
      case Some(prevLoc) => Left(
        SemanticError.DuplicateTopology(
          symbol.getUnqualifiedName,
          loc,
          prevLoc
        )
      )
      case None =>
        val map = importedTopologyMap + (symbol -> loc)
        Right(this.copy(importedTopologyMap = map))
    }

  /** Add an instance that may already be there */
  def addMergedInstance(
    instance: ComponentInstance,
    vis: Ast.Visibility,
    loc: Location
  ): Topology = {
    // Use the previous location, if it exists
    // Use the new visibility
    val mergedLoc = instanceMap.get(instance).map(_._2).getOrElse(loc)
    val map = instanceMap + (instance -> (vis, mergedLoc))
    this.copy(instanceMap = map)
  }

  /** Add an instance that must be unique */
  def addUniqueInstance(
    instance: ComponentInstance,
    vis: Ast.Visibility,
    loc: Location
  ): Result.Result[Topology] =
    instanceMap.get(instance) match {
      case Some((_, prevLoc)) => Left(
        SemanticError.DuplicateInstance(
          instance.getUnqualifiedName,
          loc,
          prevLoc
        )
      )
      case None => Right(addMergedInstance(instance, vis, loc))
    }

  /** Compute the unused ports for this topology */
  private def computeUnusedPorts: Result.Result[Topology] = {
    // TODO
    Right(this)
  }

  /** Fill in the port numbers for this topology */
  private def computePortNumbers: Result.Result[Topology] = {
    // TODO
    Right(this)
  }

  /** Get the connections from a port */
  def getConnectionsFrom(from: PortInstanceIdentifier): Set[PortInstanceIdentifier] =
    outputConnectionMap.getOrElse(from, Set())

  /** Get the connections to a port */
  def getConnectionsTo(to: PortInstanceIdentifier): Set[PortInstanceIdentifier] =
    inputConnectionMap.getOrElse(to, Set())

  /** Check whether a connection exists */
  def connectionExists(
    from: PortInstanceIdentifier,
    to: PortInstanceIdentifier
  ) = outputConnectionMap.get(from) match {
    case Some(set) => set.contains(to)
    case None => false
  }

  /** Check the instances of a pattern */
  private def checkPatternInstances(pattern: ConnectionPattern) =
    for {
      // Check the source
      _ <- {
        val (instance, loc) = pattern.source
        lookUpInstanceAt(instance, loc)
      }
      // Check the targets
      _ <- Result.map(
        pattern.targets.toList,
        (pair: (ComponentInstance, Location)) => {
          val (instance, loc) = pair
          lookUpInstanceAt(instance, loc)
        }
      )
    }
    yield ()

  /** Look up a component instance used at a location */
  private def lookUpInstanceAt(
    instance: ComponentInstance,
    loc: Location
  ): Result.Result[(Ast.Visibility, Location)] =
    instanceMap.get(instance) match {
      case Some(result) => Right(result)
      case None => Left(
        SemanticError.InvalidComponentInstance(
          loc,
          instance.getUnqualifiedName,
          this.getUnqualifiedName
        )
      )
    }

  /** Resolve a topology definition */
  def resolve(a: Analysis): Result.Result[Topology] =
    Result.seq(
      Right(this),
      List(
        _.resolvePartiallyNumbered(a),
        _.computePortNumbers,
        _.computeUnusedPorts
      )
    )

  /** Resolve the connection patterns of this topology */
  private def resolvePatterns(a: Analysis): Result.Result[Topology] = {
    import Ast.SpecConnectionGraph._
    def getGraphName(kind: Pattern.Kind) = kind match {
      case Pattern.Command => "Command"
      case Pattern.Event => "Event"
      case Pattern.Health => "Health"
      case Pattern.Telemetry => "Telemetry"
      case Pattern.Time => "Time"
    }
    Result.foldLeft (patternMap.values.toList) (this) ((t, p) => {
      val instances = instanceMap.keys.toList
      for {
        _ <- checkPatternInstances(p)
        connections <- PatternResolver.resolve(a, p, instances)
      }
      yield {
        val name = getGraphName(p.ast.kind)
        connections.foldLeft (t) ((t, c) => {
          if (
            // Skip this connection if we already imported it
            // Or if the user entered it manually
            !connectionExists(
              c.from.portInstanceIdentifier,
              c.to.portInstanceIdentifier
            )
          ) t.addConnection(name, c)
          else t
        })
      }
    })
  }

  /** Resolve the direct connections of this topology */
  private def resolveDirectConnections: Result.Result[Topology] = {
    def endpointIsPublic(endpoint: Connection.Endpoint) = {
      val instance = endpoint.portInstanceIdentifier.componentInstance
      for (pair <- lookUpInstanceAt(instance, endpoint.loc))
        yield pair._1 == Ast.Visibility.Public
    }
    def connectionIsPublic(connection: Connection) =
      for {
        fromIsPublic <- endpointIsPublic(connection.from)
        toIsPublic <- endpointIsPublic(connection.to)
      } yield fromIsPublic && toIsPublic
    def importConnection(
      t: Topology,
      graphName: Name.Unqualified,
      connection: Connection
    ) = 
      for (public <- connectionIsPublic(connection))
        yield if (public)
          t.addConnection(graphName, connection)
        else t
    def importConnections(
      t: Topology,
      mapEntry: (Name.Unqualified, List[Connection])
    ) = {
      val (graphName, connections) = mapEntry
      Result.foldLeft (connections) (t) ((t, c) => importConnection(t, graphName, c))
    }
    Result.foldLeft (connectionGraphMap.toList) (this) (importConnections)
  }

  /** Resolve the instances of this topology */
  private def resolveInstances(a: Analysis): Result.Result[Topology] = {
    def importInstance(
      t: Topology,
      mapEntry: (ComponentInstance, (Ast.Visibility, Location))
    ) = {
      val (instance, (vis, loc)) = mapEntry
      vis match {
        case Ast.Visibility.Public =>
          t.addMergedInstance(instance, vis, loc)
        case Ast.Visibility.Private => t
      }
    }
    def importInstances(into: Topology, fromSymbol: Symbol.Topology) =
      a.topologyMap(fromSymbol).instanceMap.foldLeft (into) (importInstance)
    Right(importedTopologyMap.keys.foldLeft (this) (importInstances))
  }

  /** Resolve this topology to a partially numbered topology */
  private def resolvePartiallyNumbered(a: Analysis): Result.Result[Topology] =
    Result.seq(
      Right(this),
      List(
        _.resolveInstances(a),
        _.resolveDirectConnections,
        _.resolvePatterns(a)
      )
    )

}
