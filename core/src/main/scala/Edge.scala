package com.graphbrain

class Edge(_id: String, val etype: String) extends Vertex(_id) {
  override val vtype = "edge"

  override def toMap: Map[String, Any] = super.toMap ++ Map(("etype" -> etype))

  override def toString: String = super.toString + " " + etype

  def participantIds = {
  	val tokens = _id.split(' ')
  	for (i <- 1 until tokens.length) yield tokens(i)
  }
}

object Edge {
  def apply(_id: String, etype:String) = new Edge(_id, etype)

  def apply(edgeType:String, participants: Array[Node]) = {
  	val tokens = List[String](edgeType) ++ (for (node <- participants) yield node._id)
  	val eid = tokens.reduceLeft(_ + " " + _)
  	new Edge(eid, edgeType)
  } 
}