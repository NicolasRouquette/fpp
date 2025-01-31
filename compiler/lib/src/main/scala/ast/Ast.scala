package fpp.compiler.ast

import fpp.compiler.util._

object Ast {

  /** Annotated AST value */
  type Annotated[T] = (List[String], T, List[String])

  /** Formal parameter list */
  type FormalParamList = List[Annotated[AstNode[FormalParam]]]

  /** Identifier */
  type Ident = String

  /** Translation unit */
  final case class TransUnit(members: List[TUMember])

  /** Binary operation */
  sealed trait Binop
  final object Binop {
    final case object Add extends Binop {
      override def toString = "+"
    }
    final case object Div extends Binop {
      override def toString = "/"
    }
    final case object Mul extends Binop {
      override def toString = "*"
    }
    final case object Sub extends Binop {
      override def toString = "-"
    }
  }

  /** Component kind */
  sealed trait ComponentKind
  final object ComponentKind {
    final case object Active extends ComponentKind {
      override def toString = "active"
    }
    final case object Passive extends ComponentKind {
      override def toString = "passive"
    }
    final case object Queued extends ComponentKind {
      override def toString = "queued"
    }
  }

  /** Component member */
  final case class ComponentMember(node: Annotated[ComponentMember.Node])
  object ComponentMember {
    sealed trait Node
    final case class DefAbsType(node: AstNode[Ast.DefAbsType]) extends Node
    final case class DefArray(node: AstNode[Ast.DefArray]) extends Node
    final case class DefConstant(node: AstNode[Ast.DefConstant]) extends Node
    final case class DefEnum(node: AstNode[Ast.DefEnum]) extends Node
    final case class DefStruct(node: AstNode[Ast.DefStruct]) extends Node
    final case class SpecCommand(node: AstNode[Ast.SpecCommand]) extends Node
    final case class SpecEvent(node: AstNode[Ast.SpecEvent]) extends Node
    final case class SpecInclude(node: AstNode[Ast.SpecInclude]) extends Node
    final case class SpecInternalPort(node: AstNode[Ast.SpecInternalPort]) extends Node
    final case class SpecParam(node: AstNode[Ast.SpecParam]) extends Node
    final case class SpecPortInstance(node: AstNode[Ast.SpecPortInstance]) extends Node
    final case class SpecPortMatching(node: AstNode[Ast.SpecPortMatching]) extends Node
    final case class SpecTlmChannel(node: AstNode[Ast.SpecTlmChannel]) extends Node
  }

  /** Abstract type definition */
  final case class DefAbsType(name: Ident)

  /* Array definition */
  final case class DefArray(
    name: Ident,
    size: AstNode[Expr],
    eltType: AstNode[TypeName],
    default: Option[AstNode[Expr]],
    format: Option[AstNode[String]]
  )

  /** Component definition */
  final case class DefComponent(
    kind: ComponentKind,
    name: Ident,
    members: List[ComponentMember]
  )

  /** Component instance definition */
  final case class DefComponentInstance(
    name: Ident,
    component: AstNode[QualIdent],
    baseId: AstNode[Expr],
    file: Option[AstNode[String]],
    queueSize: Option[AstNode[Expr]],
    stackSize: Option[AstNode[Expr]],
    priority: Option[AstNode[Expr]],
    cpu: Option[AstNode[Expr]],
    initSpecs: List[Annotated[AstNode[SpecInit]]]
  )

  /** Constant definition */
  final case class DefConstant(name: Ident, value: AstNode[Expr])

  /** Enum definition */
  final case class DefEnum(
    name: Ident,
    typeName: Option[AstNode[TypeName]],
    constants: List[Annotated[AstNode[DefEnumConstant]]],
    default: Option[AstNode[Expr]]
  )

  /** Enum constant definition */
  final case class DefEnumConstant(
    name: Ident,
    value: Option[AstNode[Expr]]
  )

  /** Module definition */
  final case class DefModule(
    name: Ident,
    members: List[ModuleMember]
  )

  /** Module member */
  final case class ModuleMember(node: Annotated[ModuleMember.Node])
  object ModuleMember {
    sealed trait Node
    final case class DefAbsType(node: AstNode[Ast.DefAbsType]) extends Node
    final case class DefArray(node: AstNode[Ast.DefArray]) extends Node
    final case class DefComponent(node: AstNode[Ast.DefComponent]) extends Node
    final case class DefComponentInstance(node: AstNode[Ast.DefComponentInstance]) extends Node
    final case class DefConstant(node: AstNode[Ast.DefConstant]) extends Node
    final case class DefEnum(node: AstNode[Ast.DefEnum]) extends Node
    final case class DefModule(node: AstNode[Ast.DefModule]) extends Node
    final case class DefPort(node: AstNode[Ast.DefPort]) extends Node
    final case class DefStruct(node: AstNode[Ast.DefStruct]) extends Node
    final case class DefTopology(node: AstNode[Ast.DefTopology]) extends Node
    final case class SpecInclude(node: AstNode[Ast.SpecInclude]) extends Node
    final case class SpecLoc(node: AstNode[Ast.SpecLoc]) extends Node
  }

  /** Port definition */
  final case class DefPort(
    name: Ident,
    params: FormalParamList,
    returnType: Option[AstNode[TypeName]]
  )

  /** Struct definition */
  final case class DefStruct(
    name: Ident,
    members: List[Annotated[AstNode[StructTypeMember]]],
    default: Option[AstNode[Expr]]
  )

  /** Expression */
  sealed trait Expr
  final case class ExprArray(elts: List[AstNode[Expr]]) extends Expr
  final case class ExprBinop(e1: AstNode[Expr], op: Binop, e2: AstNode[Expr]) extends Expr
  final case class ExprDot(e: AstNode[Expr], id: AstNode[Ident]) extends Expr
  final case class ExprIdent(value: Ident) extends Expr
  final case class ExprLiteralBool(value: LiteralBool) extends Expr
  final case class ExprLiteralInt(value: String) extends Expr
  final case class ExprLiteralFloat(value: String) extends Expr
  final case class ExprLiteralString(value: String) extends Expr
  final case class ExprParen(e: AstNode[Expr]) extends Expr
  final case class ExprStruct(members: List[AstNode[StructMember]]) extends Expr
  final case class ExprUnop(op: Unop, e: AstNode[Expr]) extends Expr

  /** Topology defintion */
  final case class DefTopology(
    name: Ident,
    members: List[TopologyMember]
  )

  /** Topology member */
  final case class TopologyMember(node: Annotated[TopologyMember.Node])
  object TopologyMember {
    sealed trait Node
    final case class SpecCompInstance(node: AstNode[Ast.SpecCompInstance]) extends Node
    final case class SpecConnectionGraph(node: AstNode[Ast.SpecConnectionGraph]) extends Node
    final case class SpecInclude(node: AstNode[Ast.SpecInclude]) extends Node
    final case class SpecTopImport(node: AstNode[Ast.SpecTopImport]) extends Node
  }

  /** Formal parameter */
  final case class FormalParam(
    kind: FormalParam.Kind,
    name: Ident,
    typeName: AstNode[TypeName]
  )
  final object FormalParam {
    /** Formal parameter kind */
    sealed trait Kind
    final case object Ref extends Kind
    final case object Value extends Kind
  }

  /** Literal bool */
  sealed trait LiteralBool
  object LiteralBool {
    final case object True extends LiteralBool {
      override def toString = "true"
    }
    final case object False extends LiteralBool {
      override def toString = "false"
    }
  }

  /** Port instance identifier */
  final case class PortInstanceIdentifier(
    componentInstance: AstNode[QualIdent],
    portName: AstNode[Ident]
  )

  /** A possibly-qualified identifier */
  sealed trait QualIdent {

    /** Convert a qualified identifier to a list of identifiers */
    def toIdentList: List[Ident]
      
  }

  object QualIdent {

    /** An unqualified identifier */
    case class Unqualified(name: Ident) extends QualIdent {

      def toIdentList = List(name)

    }

    /** A qualified identifier */
    case class Qualified(qualifier: AstNode[QualIdent], name: AstNode[Ident]) extends QualIdent {

      def toIdentList = qualifier.data.toIdentList ++ List(name.data)

    }

    /** Construct a qualified identifier from a node list */
    def fromNodeList(nodeList: QualIdent.NodeList): QualIdent =
      QualIdent.NodeList.split(nodeList) match {
        case (Nil, name) => QualIdent.Unqualified(name.data)
        case (qualifier, name) => {
          val qualifier1 = fromNodeList(qualifier)
          val node = AstNode.create(qualifier1, QualIdent.NodeList.name(qualifier).id)
          QualIdent.Qualified(node, name)
        }
      }

    /** A qualified identifier represented as a list of identifier nodes 
     *  This is useful during parsing */
    type NodeList = List[AstNode[Ident]]

    object NodeList {

      /** Split a qualified identifier list into qualifier and name */
      def split(nodeList: NodeList) = nodeList.reverse match {
        case head :: tail => (tail.reverse, head)
        case Nil => throw InternalError("node list should not be empty")
      }

      /** Get the qualifier */
      def qualifier(nodeList: NodeList) = split(nodeList)._1

      /** Get the unqualified name*/
      def name(nodeList: NodeList) = split(nodeList)._2

    }

    /** A qualified identifier node */
    object Node {

      /** Create a QualIdent node from a node list */
      def fromNodeList(nodeList: NodeList): AstNode[QualIdent] = {
        val qualIdent = QualIdent.fromNodeList(nodeList)
        val node = AstNode.create(qualIdent)
        val loc = Locations.get(nodeList.head.id)
        Locations.put(node.id, loc)
        node
      }

    }

  }

  /** Queue full behavior */
  sealed trait QueueFull
  final object QueueFull {
    final case object Assert extends QueueFull {
      override def toString = "assert"
    }
    final case object Block extends QueueFull {
      override def toString = "block"
    }
    final case object Drop extends QueueFull {
      override def toString = "drop"
    }
  }

  /** Command specifier */
  final case class SpecCommand(
    kind: SpecCommand.Kind,
    name: Ident,
    params: FormalParamList,
    opcode: Option[AstNode[Expr]],
    priority: Option[AstNode[Expr]],
    queueFull: Option[AstNode[QueueFull]]
  )
  object SpecCommand {
    sealed trait Kind
    final case object Async extends Kind {
      override def toString = "async"
    }
    final case object Guarded extends Kind {
      override def toString = "guarded"
    }
    final case object Sync extends Kind {
      override def toString = "sync"
    }
  }

  /** Component instance specifier */
  final case class SpecCompInstance(
    visibility: Visibility,
    instance: AstNode[QualIdent]
  )

  /** Connection graph specifier */
  sealed trait SpecConnectionGraph
  final object SpecConnectionGraph {

    final case class Direct(
      name: Ident,
      connections: List[Connection]
    ) extends SpecConnectionGraph

    final object Pattern {
      sealed trait Kind
      final case object Command extends Kind {
        override def toString = "command"
      }
      final case object Event extends Kind {
        override def toString = "event"
      }
      final case object Health extends Kind {
        override def toString = "health"
      }
      final case object Param extends Kind {
        override def toString = "param"
      }
      final case object Telemetry extends Kind {
        override def toString = "telemetry"
      }
      final case object TextEvent extends Kind {
        override def toString = "text event"
      }
      final case object Time extends Kind {
        override def toString = "time"
      }
    }

    final case class Pattern(
      kind: Pattern.Kind,
      source: AstNode[QualIdent],
      targets: List[AstNode[QualIdent]],
    ) extends SpecConnectionGraph

    /** Connection */
    final case class Connection(
      fromPort: AstNode[PortInstanceIdentifier],
      fromIndex: Option[AstNode[Expr]],
      toPort: AstNode[PortInstanceIdentifier],
      toIndex: Option[AstNode[Expr]]
    )
  }

  /** Event specifier */
  final case class SpecEvent(
    name: Ident,
    params: FormalParamList,
    severity: SpecEvent.Severity,
    id: Option[AstNode[Expr]],
    format: AstNode[String],
    throttle: Option[AstNode[Expr]]
  )
  final object SpecEvent {
    /** Event severity */
    sealed trait Severity
    final case object ActivityHigh extends Severity {
      override def toString = "activity high"
    }
    final case object ActivityLow extends Severity {
      override def toString = "activity low"
    }
    final case object Command extends Severity {
      override def toString = "command"
    }
    final case object Diagnostic extends Severity {
      override def toString = "diagnostic"
    }
    final case object Fatal extends Severity {
      override def toString = "fatal"
    }
    final case object WarningHigh extends Severity {
      override def toString = "warning high"
    }
    final case object WarningLow extends Severity {
      override def toString = "warning low"
    }
  }

  /** Include specifier */
  final case class SpecInclude(file: AstNode[String])

  /** Init specifier */
  final case class SpecInit(
    phase: AstNode[Expr],
    code: String
  )

  /** Internal port specifier */
  final case class SpecInternalPort(
    name: Ident,
    params: FormalParamList,
    priority: Option[AstNode[Expr]],
    queueFull: Option[QueueFull]
  )

  /** Location specifier */
  final case class SpecLoc(
    kind: SpecLoc.Kind,
    symbol: AstNode[QualIdent],
    file: AstNode[String]
  )
  object SpecLoc {
    /** Location specifier kind */
    sealed trait Kind
    final case object Component extends Kind {
      override def toString = "component"
    }
    final case object ComponentInstance extends Kind {
      override def toString = "instance"
    }
    final case object Constant extends Kind {
      override def toString = "constant"
    }
    final case object Port extends Kind {
      override def toString = "port"
    }
    final case object Topology extends Kind {
      override def toString = "topology"
    }
    final case object Type extends Kind {
      override def toString = "type"
    }
  }

  /** Parameter specifier */
  final case class SpecParam(
    name: Ident,
    typeName: AstNode[TypeName],
    default: Option[AstNode[Expr]],
    id: Option[AstNode[Expr]],
    setOpcode:  Option[AstNode[Expr]],
    saveOpcode:  Option[AstNode[Expr]]
  )

  /** Port instance specifier */
  sealed trait SpecPortInstance
  object SpecPortInstance {

    /** General port instance */
    final case class General(
      kind: GeneralKind,
      name: Ident,
      size: Option[AstNode[Expr]],
      port: Option[AstNode[QualIdent]],
      priority: Option[AstNode[Expr]],
      queueFull: Option[AstNode[QueueFull]]
    ) extends SpecPortInstance

    /** General port instance kind */
    sealed trait GeneralKind
    final case object AsyncInput extends GeneralKind {
      override def toString = "async input"
    }
    final case object GuardedInput extends GeneralKind {
      override def toString = "guarded input"
    }
    final case object Output extends GeneralKind {
      override def toString = "output"
    }
    final case object SyncInput extends GeneralKind {
      override def toString = "sync input"
    }

    /** Special port instance */
    final case class Special (
      kind: SpecialKind,
      name: Ident
    ) extends SpecPortInstance

    /** Special port instance kind */
    sealed trait SpecialKind
    final case object CommandRecv extends SpecialKind {
      override def toString = "command recv"
    }
    final case object CommandReg extends SpecialKind {
      override def toString = "command reg"
    }
    final case object CommandResp extends SpecialKind {
      override def toString = "command resp"
    }
    final case object Event extends SpecialKind {
      override def toString = "event"
    }
    final case object ParamGet extends SpecialKind {
      override def toString = "param get"
    }
    final case object ParamSet extends SpecialKind {
      override def toString = "param set"
    }
    final case object Telemetry extends SpecialKind {
      override def toString = "telemetry"
    }
    final case object TextEvent extends SpecialKind {
      override def toString = "text event"
    }
    final case object TimeGet extends SpecialKind {
      override def toString = "time get"
    }

  }

  /** Port matching specifier */
  final case class SpecPortMatching(
    port1: AstNode[Ident],
    port2: AstNode[Ident]
  )

  /** Telemetry channel specifier */
  final case class SpecTlmChannel(
    name: Ident,
    typeName: AstNode[TypeName],
    id: Option[AstNode[Expr]],
    update: Option[SpecTlmChannel.Update],
    format: Option[AstNode[String]],
    low: List[SpecTlmChannel.Limit],
    high: List[SpecTlmChannel.Limit]
  )
  final object SpecTlmChannel {

    /** Telemetry update */
    sealed trait Update
    final case object Always extends Update {
      override def toString = "always"
    }
    final case object OnChange extends Update {
      override def toString = "on change"
    }

    /** Telemetry limit */
    type Limit = (AstNode[LimitKind], AstNode[Expr])

    /** Telemetry limit kind */
    sealed trait LimitKind
    final case object Red extends LimitKind {
      override def toString = "red"
    }
    final case object Orange extends LimitKind {
      override def toString = "orange"
    }
    final case object Yellow extends LimitKind {
      override def toString = "yellow"
    }

  }

  /** Topology import specifier */
  final case class SpecTopImport(top: AstNode[QualIdent])

  /** Struct member */
  final case class StructMember(name: Ident, value: AstNode[Expr])

  /** Struct type member */
  final case class StructTypeMember(
    name: Ident,
    size: Option[AstNode[Expr]],
    typeName: AstNode[TypeName],
    format: Option[AstNode[String]]
  )

  /** Translation unit member */
  type TUMember = ModuleMember
  val TUMember = ModuleMember

  /** Float type */
  sealed trait TypeFloat
  final case class F32() extends TypeFloat {
    override def toString = "F32"
  }
  final case class F64() extends TypeFloat {
    override def toString = "F64"
  }

  /** Int type */
  sealed trait TypeInt
  final case class I8() extends TypeInt {
    override def toString = "I8"
  }
  final case class I16() extends TypeInt {
    override def toString = "I16"
  }
  final case class I32() extends TypeInt {
    override def toString = "I32"
  }
  final case class I64() extends TypeInt {
    override def toString = "I64"
  }
  final case class U8() extends TypeInt {
    override def toString = "U8"
  }
  final case class U16() extends TypeInt {
    override def toString = "U16"
  }
  final case class U32() extends TypeInt {
    override def toString = "U32"
  }
  final case class U64() extends TypeInt {
    override def toString = "U64"
  }

  /** Type name */
  sealed trait TypeName
  final case class TypeNameFloat(name: TypeFloat) extends TypeName
  final case class TypeNameInt(name: TypeInt) extends TypeName
  final case class TypeNameQualIdent(name: AstNode[QualIdent]) extends TypeName
  final case object TypeNameBool extends TypeName
  final case class TypeNameString(size: Option[AstNode[Expr]]) extends TypeName

  /** Unary operation */
  sealed trait Unop
  final object Unop {
    final case object Minus extends Unop {
      override def toString = "-"
    }
  }

  /** Visibility */
  sealed trait Visibility
  final object Visibility {
    final case object Private extends Visibility {
      override def toString = "private"
    }
    final case object Public extends Visibility {
      override def toString = "public"
    }
  }

}
