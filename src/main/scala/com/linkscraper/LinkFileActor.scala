package com.linkscraper

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source._

class LinkFileActor extends Actor with ActorLogging {
	
	 def receive = {
  	case getLinks =>  
  	   	val links = fromFile("target1.txt").getLines.toList 
  		sender ! LinkFileActor.LinkList(links)
  }	

}

object LinkFileActor {
  val props = Props[LinkFileActor]
  case object getLinks
  case class LinkList(list: List[String])
  
}
