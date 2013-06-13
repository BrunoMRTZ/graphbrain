package com.graphbrain.webapp

import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._
import unfiltered.Cookie

import com.graphbrain.hgdb.Edge
import com.graphbrain.hgdb.SearchInterface
import com.graphbrain.hgdb.JSONGen


object GBPlan extends cycle.Plan with cycle.SynchronousExecution with ServerErrorResponse {
  def intent = {
    case req@POST(Path("/search") & Params(params) & Cookies(cookies)) => {
      val query = params("q")(0)
      val si = new SearchInterface(Server.store)
      val results = si.query(query)
      
      val resultsList: Seq[List[String]] = (for (id <- results)
        yield List(id, Server.store.get(id).description))
      
      val json = Map(("count" -> results.size), ("results" -> resultsList))
      
      Server.log(req, cookies, "SEARCH query: " + query + "; results: " + results.size)

      ResponseString(JSONGen.json(json))
    }
    case req@POST(Path("/signup") & Params(params)) => {
      val name = params("name")(0)
      val username = params("username")(0)
      val email = params("email")(0)
      val password = params("password")(0)
      Server.store.createUser(username, name, email, password, "user")

      Server.log(req, null, "SIGNUP name: " + name + "; username: " + username + "; email:" + email)
      
      ResponseString("ok")
    }
    case req@POST(Path("/checkusername") & Params(params)) => {
      val username = params("username")(0)
      if (Server.store.usernameExists(username)) {
        ResponseString("exists " + username)
      }
      else {
        ResponseString("ok " + username)
      }
    }
    case req@POST(Path("/checkemail") & Params(params)) => {
      val email = params("email")(0)
      if (Server.store.emailExists(email)) {
        ResponseString("exists " + email)
      }
      else {
        ResponseString("ok " + email) 
      }
    }
    case req@POST(Path("/login") & Params(params)) => {
      val login = params("login")(0)
      val password = params("password")(0)
      val user = Server.store.attemptLogin(login, password)
      if (user == null) {
        Server.log(req, null, "FAILED LOGIN login: " + login)
        ResponseString("failed")
      }
      else {
        Server.log(req, null, "LOGIN login: " + login)
        ResponseString(user.username + " " + user.session)
      } 
    }
    case req@POST(Path("/undo_fact") & Params(params) & Cookies(cookies)) => {
      val userNode = Server.getUser(cookies)

      val rel = params("rel")(0)
      val participants = params("participants")(0)

      val participantIds = participants.split(" ").toList

      // undo connection
      Server.store.delrel2(rel, participantIds, userNode.id)
      // force consesnsus re-evaluation of affected edge
      val edge = Edge(rel, participantIds)
      Server.consensusActor ! edge

      ResponseString(JSONGen.json(""))
    }
    case req@GET(Path("/allusers") & Cookies(cookies)) => {
      val userNode = Server.getUser(cookies)
      AllUsersPage(userNode, req, cookies).response
    }

    // TEMPORARY: Amazon backdoor
    case req@GET(Path("/amazon") & Params(params)) => {
      val login = "amazon"
      val user = Server.store.forceLogin(login)

      Server.log(req, null, "AMAZON access ")
      
      ResponseCookies(Cookie("username", user.username)) ~> ResponseCookies(Cookie("session", user.session)) ~> Redirect("/node/user/amazon") 
    }
  }
}