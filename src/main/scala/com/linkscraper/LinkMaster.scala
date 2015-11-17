package com.linkscraper

import java.io.IOException

import akka.actor.{Actor, ActorRef,OneForOneStrategy,SupervisorStrategy, Terminated,ActorLogging, Props}
import akka.actor.SupervisorStrategy.Restart
import akka.routing.CurrentRoutees
import akka.routing.RoundRobinRouter
import akka.routing.RouterRoutees

class LinkMaster extends Actor with ActorLogging {
 
  import LinkMaster._
  var replyTo: ActorRef = _
  var toplinks: List[String] = _
  val linkFileActor = context.actorOf(LinkFileActor.props, "linkFileActor")

  val router = context.actorOf(Props(new LinkWorker).
    withRouter(RoundRobinRouter(2,
      supervisorStrategy = OneForOneStrategy(
        maxNrOfRetries = 2) {
          case _: IOException ⇒ Restart
        })), name = "router")

  def receive = {

  	case Initialize =>  
      replyTo = sender
  	  linkFileActor ! LinkFileActor.getLinks

  	case LinkFileActor.LinkList(links) => 
  	  toplinks = links.distinct
      toplinks.foreach(println)
      //if list is empty try to terminate
      //context.system.shutdown
      for (t <- toplinks) router ! (t) 


    case (url: String, status: String) =>
      println(status)

    case stop =>
      context.system.shutdown()
  }
}

object LinkMaster {
  val props = Props[LinkMaster]
  case object Initialize
}