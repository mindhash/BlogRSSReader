package com.linkscraper

import akka.actor.ActorSystem

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val linkMaster = system.actorOf(LinkMaster.props, "linkMaster")
  linkMaster ! LinkMaster.Initialize

  system.awaitTermination()
}