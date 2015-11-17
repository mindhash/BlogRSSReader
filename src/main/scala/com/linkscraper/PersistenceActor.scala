package com.linkscraper

import akka.actor.{Actor, ActorLogging, Props} 
import com.mongodb.casbah.Imports._

class PersistenceActor extends Actor with ActorLogging {

import PersistenceActor._

	val mongoConn = MongoConnection()
	val DB = "kndb"

	 def receive = {
	 	case insert(bucket: String, newObj: MongoDBObject) =>
	 		val collection = mongoConn(DB)(bucket)
	 		 collection += newObj
	 		 println("Inserted Object into DB")
     }
}

object PersistenceActor {
	val props = Props[PersistenceActor]
	case class insert(bucket: String, newObj: MongoDBObject) 
}