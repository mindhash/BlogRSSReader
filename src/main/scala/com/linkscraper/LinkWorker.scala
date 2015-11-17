package com.linkscraper

import akka.actor.{Actor, ActorLogging, ActorRef,Props}
import com.mongodb.casbah.Imports._

class RssLinkExtractor extends Actor with ActorLogging {

import net.ruippeixotog.scalascraper.browser.Browser

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.jsoup.nodes.Element
import scala.util.control.Breaks._

//need to add this logic in case url is already rss feed ???
//def couldBeFeedData 

def receive ={
		case (url: String) => 
			val browser = new Browser
			val doc = browser.get(url)	 //handle exception whe url is not reachable
			log.info("Fetched URL data")
			var retURL: String = null

		    val rsslinks: List[Element] = doc >> elementList("link") //>> attr("href")("link") 

		    for (e <- rsslinks) {
		    	  
		    	 if (e.attr("type").contains("rss") && (!e.attr("href").contains("comments")) )  
		    	 	retURL =e.attr("href");  

		    	 if (e.attr("type").contains("atom") && (!e.attr("href").contains("comments")) ) 
		    	 	retURL =e.attr("href")  
		    }
		  
		    sender() !  LinkWorker.ProcessRssUrl (retURL)
	}	
}

object RssLinkExtractor {
	val props = Props[RssLinkExtractor]
}

class LinkWorker extends Actor with ActorLogging {

import LinkWorker._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._ 
import com.linkscraper.models._

val rssLinkExtractor = context.actorOf(RssLinkExtractor.props, "RssLinkExtractor")
val rssReader = context.actorOf(RssReader.props, "RssReader")
val persistenceActor = context.actorOf(PersistenceActor.props, "PersistenceActor")
var replyTo : ActorRef = _

def sendFeedToDb (feed: Seq[RssFeed]) = {
	//iterate through feed.created DB object and send to Persistence Actor

	for (fd <- feed; f <- fd.items ) {
		println("Title:" + f.title)
		val newObj = MongoDBObject("title" -> f.title, "ArticleURL" -> f.link, "feedURL" -> fd.link, "sourceguid" -> f.guid,
			"desc" -> f.desc)
		//write article metadata to DB
  		persistenceActor ! PersistenceActor.insert("ArticleMaster",newObj)
	}

}

//need to change type to URL
def receive ={
	case  (url: String) =>
		//val f1 = ask(rssLinkExtractor ,url)
		//val rssURL = Await.result(f1,15 seconds).asInstanceOf[String]
		//val rssURL: Future[String] = ask(rssLinkExtractor, url).mapTo[String]
		replyTo = sender
		println("Sending Toplink URL" + url)
		rssLinkExtractor !  url

	case ProcessRssUrl (rssURL: String) =>
		println("In Process URL" + rssURL)
		rssReader ! rssURL

	case RssReader.RssFeedData (feed: Seq[RssFeed])=>
		println("In Linkworker : RssFeeddata")
		sendFeedToDb (feed)

	case done (url: String, status: String) => 	
	    //send message to sender (replyTo) with status
	    println()
}


//put failed message for retry
// override def preRestart(
//    reason: Throwable, message: Option[Any]) {
//    message foreach { self forward _ }
//  }

}

object LinkWorker {
	   val props = Props[LinkWorker]
	   case class ProcessRssUrl (rssURL: String) 
	   case class done(url: String, status: String) 
}
