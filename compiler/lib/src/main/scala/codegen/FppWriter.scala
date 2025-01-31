package fpp.compiler.codegen

import fpp.compiler.ast._
import fpp.compiler.util._
import scala.language.implicitConversions

/** Write out FPP source */
object FppWriter extends AstVisitor with LineUtils {

  private case class JoinOps(ls: List[Line]) {

    def addSuffix(suffix: String) = Line.addSuffix(ls, suffix)

    def join (sep: String) (ls1: List[Line]) =
      Line.joinLists (Line.Indent) (ls) (sep) (ls1)

    def joinNoIndent (sep: String) (ls1: List[Line]) =
      Line.joinLists (Line.NoIndent) (ls) (sep) (ls1)

    def joinWithBreak[T] (sep: String) (ls1: List[Line]) =
      (sep, ls1) match {
        case ("", Nil) => ls
        case _ => Line.addSuffix(ls, " \\") ++ Line.addPrefix(sep, ls1).map(indentIn)
      }

    def joinOpt[T] (opt: Option[T]) (sep: String) (f: T => List[Line]) =
      opt match {
        case Some(t) => join (sep) (f(t))
        case None => ls
      }

    def joinOptWithBreak[T] (opt: Option[T]) (sep: String) (f: T => List[Line]) =
      opt match {
        case Some(t) => joinWithBreak (sep) (f(t))
        case None => ls
      }

  }

  private implicit def lift(ls: List[Line]) = JoinOps(ls)

  def componentMember(member: Ast.ComponentMember) = {
    val (a1, _, a2) = member.node
    val l = matchComponentMember((), member)
    annotate(a1, l, a2)
  }

  def moduleMember(member: Ast.ModuleMember) = {
    val (a1, _, a2) = member.node
    val l = matchModuleMember((), member)
    annotate(a1, l, a2)
  }

  def topologyMember(member: Ast.TopologyMember) = {
    val (a1, _, a2) = member.node
    val l = matchTopologyMember((), member)
    annotate(a1, l, a2)
  }

  def transUnit(tu: Ast.TransUnit): Out = transUnit((), tu)

  def tuMember(tum: Ast.TUMember) = moduleMember(tum)

  def tuMemberList(tuml: List[Ast.TUMember]) =
    Line.blankSeparated (tuMember) (tuml)

  override def defAbsTypeAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefAbsType]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"type ${ident(data.name)}")
  }

  override def defArrayAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefArray]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"array ${ident(data.name)} = [").
      join ("") (exprNode(data.size)).
      join ("] ") (typeNameNode(data.eltType)).
      joinOpt (data.default) (" default ") (exprNode).
      joinOpt (data.format) (" format ") (applyToData(string))
  }

  override def defComponentAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefComponent]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val kind = data.kind.toString
    List(line(s"$kind component ${ident(data.name)} {"), Line.blank) ++
    (Line.blankSeparated (componentMember) (data.members)).map(indentIn) ++
    List(Line.blank, line("}"))
  }

  override def defComponentInstanceAnnotatedNode(
    in: In,
    aNode: Ast.Annotated[AstNode[Ast.DefComponentInstance]]
  ) = {
    def initSpecs(list: List[Ast.Annotated[AstNode[Ast.SpecInit]]]) =
      addBracesIfNonempty(list.flatMap(annotateNode(specInit)))
    val (_, node, _) = aNode
    val data = node.data
    lines(s"instance ${ident(data.name)}").
      join (": ") (qualIdent(data.component.data)).
      join (" base id ") (exprNode(data.baseId)).
      joinOptWithBreak (data.file) ("at ") (applyToData(string)).
      joinOptWithBreak (data.queueSize) ("queue size ") (exprNode).
      joinOptWithBreak (data.stackSize) ("stack size ") (exprNode).
      joinOptWithBreak (data.priority) ("priority ") (exprNode).
      joinOptWithBreak (data.cpu) ("cpu ") (exprNode).
      joinWithBreak ("") (initSpecs(data.initSpecs))
  }

  override def defConstantAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefConstant]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"constant ${ident(data.name)}").join (" = ") (exprNode(data.value))
  }

  override def defEnumAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefEnum]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"enum ${ident(data.name)}").
      joinOpt (data.typeName) (": ") (typeNameNode).
      joinNoIndent (" ") (
        addBraces(data.constants.flatMap(annotateNode(defEnumConstant)))
      )
  }

  override def defModuleAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefModule]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    List(line(s"module ${ident(data.name)} {"), Line.blank) ++
    (Line.blankSeparated (moduleMember) (data.members)).map(indentIn) ++
    List(Line.blank, line("}"))
  }

  override def defPortAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefPort]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"port ${ident(data.name)}").
      join ("") (formalParamList(data.params)).
      joinOpt (data.returnType) (" -> ") (typeNameNode)
  }

  override def defStructAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.DefStruct]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"struct ${ident(data.name)}").
    joinNoIndent (" ") (
      addBraces(data.members.flatMap(annotateNode(structTypeMember)))
    ).
    joinOpt (data.default) (" default ") (exprNode)
  }

  override def defTopologyAnnotatedNode(
    in: In,
    aNode: Ast.Annotated[AstNode[Ast.DefTopology]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    List(line(s"topology ${ident(data.name)} {"), Line.blank) ++
    (Line.blankSeparated (topologyMember) (data.members)).map(indentIn) ++
    List(Line.blank, line("}"))
  }

  override def default(in: Unit) =
    throw new InternalError("FppWriter: Visitor not implemented")

  override def exprArrayNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprArray
  ) =
    (line("[") :: e.elts.flatMap(exprNode).map(indentIn)) :+ line("]")

  override def exprBinopNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprBinop
  ) = exprNode(e.e1).join (binop(e.op)) (exprNode(e.e2))

  override def exprDotNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprDot
  ) = exprNode(e.e).join (".") (lines(e.id.data))

  override def exprIdentNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprIdent
  ) = lines(e.value)

  override def exprLiteralBoolNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprLiteralBool
  ) = lines(e.value.toString)

  override def exprLiteralFloatNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprLiteralFloat
  ) = lines(e.value)

  override def exprLiteralIntNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprLiteralInt
  ) = lines(e.value)

  override def exprLiteralStringNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprLiteralString
  ) = string(e.value)

  override def exprParenNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprParen
  ) = Line.addPrefixAndSuffix("(", exprNode(e.e), ")")

  override def exprStructNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprStruct
  ) = addBraces(e.members.flatMap(applyToData(structMember)))

  override def exprUnopNode(
    in: Unit,
    node: AstNode[Ast.Expr],
    e: Ast.ExprUnop
  ) = lines(unop(e.op)).join ("") (exprNode(e.e))

  override def specCommandAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecCommand]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val kind = data.kind.toString
    lines(s"$kind command ${ident(data.name)}").
      join ("") (formalParamList(data.params)).
      joinOptWithBreak (data.opcode) ("opcode ") (exprNode).
      joinOptWithBreak (data.priority) ("priority ") (exprNode).
      joinOptWithBreak (data.queueFull) ("") (applyToData(queueFull))
  }

  override def specCompInstanceAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecCompInstance]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val visibility = data.visibility match {
      case Ast.Visibility.Public => ""
      case Ast.Visibility.Private => "private "
    }
    lines(visibility).
    join ("instance ") (qualIdent(data.instance.data))
  }

  override def specConnectionGraphAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecConnectionGraph]]
  ) = {
    val (_, node, _) = aNode
    def direct(scg: Ast.SpecConnectionGraph.Direct) =
      lines(s"connections ${ident(scg.name)}").
      joinNoIndent (" ") (
        addBraces(scg.connections.flatMap(connection))
      )
    def pattern(scg: Ast.SpecConnectionGraph.Pattern) = {
      Line.addPrefix(
        s"${scg.kind.toString} connections instance ",
        qualIdent(scg.source.data).
        joinNoIndent (" ") (
          addBracesIfNonempty(scg.targets.flatMap(applyToData(qualIdent)))
        )
      )
    }
    node.data match {
      case scg : Ast.SpecConnectionGraph.Direct => direct(scg)
      case scg : Ast.SpecConnectionGraph.Pattern => pattern(scg)
    }
  }

  override def specEventAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecEvent]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val severity = data.severity.toString
    lines(s"event ${ident(data.name)}").
      join ("") (formalParamList(data.params)).
      joinWithBreak ("severity ") (lines(severity)).
      joinOptWithBreak (data.id) ("id ") (exprNode).
      joinWithBreak ("format ") (string(data.format.data)).
      joinOptWithBreak (data.throttle) ("throttle ") (exprNode)
  }

  override def specIncludeAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecInclude]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines("include").join (" ") (string(data.file.data))
  }

  override def specInternalPortAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecInternalPort]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"internal port ${ident(data.name)}").
      join ("") (formalParamList(data.params)).
      joinOptWithBreak (data.priority) ("priority ") (exprNode).
      joinOptWithBreak (data.queueFull) ("") (queueFull)
  }

  override def specLocAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecLoc]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val kind = data.kind.toString
    lines(s"locate ${kind}").
      join (" ") (qualIdent(data.symbol.data)).
      join (" at ") (string(data.file.data))
  }

  override def specParamAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecParam]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    lines(s"param ${ident(data.name)}").
      join (": ") (typeNameNode(data.typeName)).
      joinOpt (data.default) (" default ") (exprNode).
      joinOpt (data.id) (" id ") (exprNode).
      joinOptWithBreak (data.setOpcode) ("set opcode ") (exprNode).
      joinOptWithBreak (data.saveOpcode) ("save opcode ") (exprNode)
  }

  override def specPortInstanceAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecPortInstance]]
  ) = {
    val (_, node, _) = aNode
    def general(i: Ast.SpecPortInstance.General) = {
      val kind = i.kind.toString
      def port(portOpt: Option[AstNode[Ast.QualIdent]]) =
        portOpt match {
          case Some(qidNode) => qualIdent(qidNode.data)
          case None => lines("serial")
        }
      lines(s"$kind port ${ident(i.name)}:").
        joinOpt (i.size) (" ") (bracketExprNode).
        join (" ") (port(i.port)).
        joinOptWithBreak (i.priority) ("priority ") (exprNode).
        joinOptWithBreak (i.queueFull) ("") (applyToData(queueFull))
    }
    def special(i: Ast.SpecPortInstance.Special) = {
      val kind = i.kind.toString
      lines(s"$kind port ${ident(i.name)}")
    }
    node.data match {
      case i : Ast.SpecPortInstance.General => general(i)
      case i : Ast.SpecPortInstance.Special => special(i)
    }
  }

  override def specPortMatchingAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecPortMatching]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val port1 = data.port1.data
    val port2 = data.port2.data
    lines(s"match $port1 with $port2")
  }

  override def specTlmChannelAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecTlmChannel]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    def update(u: Ast.SpecTlmChannel.Update) = lines(u.toString)
    def limit(l: Ast.SpecTlmChannel.Limit) = {
      val (k, en) = l
      lines(k.data.toString).join (" ") (exprNode(en))
    }
    def optList[T](l: T) = l match {
      case Nil => None
      case _ => Some(l)
    }
    def limitSeq (ls: List[Ast.SpecTlmChannel.Limit]) =
      addBraces(ls.flatMap(limit))
    lines(s"telemetry ${ident(data.name)}").
      join (": ") (typeNameNode(data.typeName)).
      joinOpt (data.id) (" id ") (exprNode).
      joinOpt (data.update) (" update ") (update).
      joinOptWithBreak (data.format) ("format ") (applyToData(string)).
      joinOptWithBreak (optList(data.low)) ("low ") (limitSeq).
      joinOptWithBreak (optList(data.high)) ("high ") (limitSeq)
  }

  override def specTopImportAnnotatedNode(
    in: Unit,
    aNode: Ast.Annotated[AstNode[Ast.SpecTopImport]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    Line.addPrefix("import ", qualIdent(data.top.data))
  }

  override def transUnit(
    in: Unit,
    tu: Ast.TransUnit
  ) = tuMemberList(tu.members)

  override def typeNameBoolNode(
    in: Unit,
    node: AstNode[Ast.TypeName]
  ) = lines("bool")

  override def typeNameFloatNode(
    in: Unit,
    node: AstNode[Ast.TypeName],
    tn: Ast.TypeNameFloat
  ) = lines(tn.name.toString)

  override def typeNameIntNode(
    in: Unit,
    node: AstNode[Ast.TypeName],
    tn: Ast.TypeNameInt
  ) = lines(tn.name.toString)

  override def typeNameQualIdentNode(
    in: Unit,
    node: AstNode[Ast.TypeName],
    tn: Ast.TypeNameQualIdent
  ) = qualIdent(tn.name.data)

  override def typeNameStringNode(
    in: Unit,
    node: AstNode[Ast.TypeName],
    tn: Ast.TypeNameString
  ) = lines("string").joinOpt (tn.size) (" size ") (exprNode)

  private def addBraces(ls: List[Line]): List[Line] =
    line("{") :: (ls.map(indentIn) :+ line("}"))

  private def addBracesIfNonempty(ls: List[Line]): List[Line] =
    ls match {
      case Nil => Nil
      case _ => addBraces(ls)
    }

  private def annotate(
    pre: List[String],
    lines: List[Line],
    post: List[String]
  ) = {
    val pre1 = pre.map((s: String) => line("@ " ++ s))
    val post1 = post.map((s: String) => line("@< " ++ s))
    (pre1 ++ lines).join (" ") (post1)
  }

  private def annotateNode[T](f: T => List[Line]):
  Ast.Annotated[AstNode[T]] => List[Line] =
    (aNode: Ast.Annotated[AstNode[T]]) => {
      val (a1, node, a2) = aNode
      annotate(a1, f(node.data), a2)
    }

  private def applyToData[A,B](f: A => B): AstNode[A] => B =
    (a: AstNode[A]) => f(a.data)

  private def binop(op: Ast.Binop) = s" ${op.toString} "

  private def bracketExprNode(en: AstNode[Ast.Expr]) =
    Line.addPrefixAndSuffix("[", exprNode(en), "]")

  private def defEnumConstant(dec: Ast.DefEnumConstant) =
    lines(ident(dec.name)).joinOpt (dec.value) (" = ") (exprNode)

  private def exprNode(node: AstNode[Ast.Expr]): List[Line] =
    matchExprNode((), node)

  private def ident(id: Ast.Ident) =
    if (keywords.contains(id)) "$" ++ id else id

  private def formalParam(fp: Ast.FormalParam) = {
    val prefix = fp.kind match {
      case Ast.FormalParam.Ref => "ref "
      case Ast.FormalParam.Value => ""
    }
    val name = prefix ++ ident(fp.name)
    lines(name).join (": ") (typeNameNode(fp.typeName))
  }

  private def formalParamList(fpl: Ast.FormalParamList) =
    fpl match {
      case Nil => Nil
      case _ =>
        lines("(") ++
        fpl.flatMap(annotateNode(formalParam)).map(indentIn) ++
        lines(")")
    }

  private def qualIdent(qid: Ast.QualIdent): List[Line] =
    lines(qualIdentString(qid))

  private def qualIdentString(qid: Ast.QualIdent): String =
    qid match {
      case Ast.QualIdent.Unqualified(name) => ident(name)
      case Ast.QualIdent.Qualified(qualifier, name) =>
        qualIdentString(qualifier.data) ++ "." ++ ident(name.data)
    }

  private def queueFull(qf: Ast.QueueFull) = lines(qf.toString)

  private def specInit(si: Ast.SpecInit) =
    Line.addPrefix("phase ", exprNode(si.phase)).
    join (" ") (string(si.code))

  private def string(s: String) =
    s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").
      split("\n").toList match {
        case Nil => lines("\"\"")
        case s :: Nil => lines("\"" ++ s ++ "\"")
        case ss => lines("\"\"\"") ++ ss.map(line) ++ lines("\"\"\"")
      }

  private def structMember(member: Ast.StructMember) =
    lines(ident(member.name)).join (" = ") (exprNode(member.value))

  private def structTypeMember(member: Ast.StructTypeMember) =
    lines(s"${ident(member.name)}:").
      joinOpt (member.size) (" ") (bracketExprNode).
      join (" ") (typeNameNode(member.typeName)).
      joinOpt (member.format) (" format ") (applyToData(string))

  private def portInstanceId(pii: Ast.PortInstanceIdentifier) =
    qualIdent(pii.componentInstance.data).
    addSuffix(s".${ident(pii.portName.data)}")

  private def connection(c: Ast.SpecConnectionGraph.Connection) =
    portInstanceId(c.fromPort.data).
    joinOpt (c.fromIndex) ("") (bracketExprNode).
    join (" -> ") (portInstanceId(c.toPort.data)).
    joinOpt (c.toIndex) ("") (bracketExprNode)

  private def typeNameNode(node: AstNode[Ast.TypeName]) = matchTypeNameNode((), node)

  private def unop(op: Ast.Unop) = op.toString

  type In = Unit

  type Out = List[Line]

  val keywords = Set(
    "F32",
    "F64",
    "I16",
    "I32",
    "I64",
    "I8",
    "U16",
    "U32",
    "U64",
    "U8",
    "active",
    "activity",
    "always",
    "array",
    "assert",
    "async",
    "at",
    "base",
    "block",
    "bool",
    "change",
    "command",
    "component",
    "connections",
    "constant",
    "default",
    "diagnostic",
    "drop",
    "enum",
    "event",
    "false",
    "fatal",
    "format",
    "get",
    "guarded",
    "health",
    "high",
    "id",
    "import",
    "include",
    "input",
    "instance",
    "internal",
    "locate",
    "low",
    "match",
    "module",
    "on",
    "opcode",
    "orange",
    "output",
    "param",
    "passive",
    "phase",
    "port",
    "priority",
    "private",
    "queue",
    "queued",
    "recv",
    "red",
    "ref",
    "reg",
    "resp",
    "save",
    "serial",
    "set",
    "severity",
    "size",
    "stack",
    "string",
    "struct",
    "sync",
    "telemetry",
    "text",
    "throttle",
    "time",
    "topology",
    "true",
    "type",
    "update",
    "warning",
    "with",
    "yellow",
  )

}
